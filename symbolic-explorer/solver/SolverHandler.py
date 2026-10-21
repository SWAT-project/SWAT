import copy
import re
import time
from svcomp.SymbolicStorage import DataTypes
import z3
from z3 import Solver, is_true, Not, Optimize, Z3_OP_UNINTERPRETED, Abs
from enum import Enum
from typing import Union, Dict, Tuple, Any
from datetime import datetime
import os
from timing.TimingManager import TimingManager
import logging
from data.Database import Database

logger = logging.getLogger("explorer.log")

# Z3 5.1 made a new regex solver the default (smt.seq.regex_monadic). On the string queries of
# String/Character method models (str.in_re over re.range with many char bounds) it is much
# slower than the previous one (e.g. jbmc-regression/StaticCharMethods02/03: seconds before,
# minutes or no answer after), so keep the previous solver. Older Z3 has no such parameter.
try:
    z3.set_param("smt.seq.regex_monadic", False)
except z3.Z3Exception:
    pass


class SATResult(Enum):
    SAT = 'sat'
    UNSAT = 'unsat'
    UNKNOWN = 'unknown'
    # The solver ran out of the resource limit (rlimit) it was given. Unlike UNKNOWN, a larger
    # limit may still decide the query. Only returned when solve() is given an rlimit.
    LIMIT = 'limit'
    
# The shared Z3 context's cumulative resource count after the last check. Checks run one at a time,
# so the difference to the count after the next one is what that check used (see _rlimit_used).
_last_rlimit_count = 0


def _rlimit_used(solver) -> int:
    """The resources (Z3 rlimit units) the check just run on `solver` used."""
    global _last_rlimit_count
    stats = solver.statistics()
    count = next((stats.get_key_value(k) for k in stats.keys() if k == 'rlimit count'), _last_rlimit_count)
    used, _last_rlimit_count = count - _last_rlimit_count, count
    return used


def _has_fp(exprs) -> bool:
    """Whether any of the expressions contains a floating-point or rounding-mode sorted term.

    Walks the term DAG through the C API (about 3x faster than the z3py wrappers, which build a
    Python object per child), with a memo of the AST ids seen in this call only: an id is unique
    only among live ASTs of one context, and these are all kept alive by `exprs` while it runs."""
    exprs = list(exprs)
    if not exprs:
        return False
    c = exprs[0].ctx.ref()
    seen = set()
    stack = [e.as_ast() for e in exprs]
    while stack:
        a = stack.pop()
        k = z3.Z3_get_ast_id(c, a)
        if k in seen:
            continue
        seen.add(k)
        kind = z3.Z3_get_ast_kind(c, a)
        if kind != z3.Z3_APP_AST and kind != z3.Z3_NUMERAL_AST:
            continue
        sort_kind = z3.Z3_get_sort_kind(c, z3.Z3_get_sort(c, a))
        if sort_kind == z3.Z3_FLOATING_POINT_SORT or sort_kind == z3.Z3_ROUNDING_MODE_SORT:
            return True
        if kind == z3.Z3_APP_AST:
            app = z3.Z3_to_app(c, a)
            stack.extend(z3.Z3_get_app_arg(c, app, i) for i in range(z3.Z3_get_app_num_args(c, app)))
    return False


def _entry_has_fp(entry: dict) -> bool:
    """_has_fp of a ConstraintCache entry's expressions, computed once and kept in the entry
    (which lives as long as its expressions, so the flag cannot go stale)."""
    flag = entry.get('has_fp')
    if flag is None:
        flag = entry['has_fp'] = _has_fp(entry['exprs'])
    return flag


def _cut_short(reason: str) -> bool:
    """Whether a check that ended in unknown was stopped from outside rather than given up: its
    rlimit ran out ("canceled"; older Z3: "max. resource limit exceeded"), its timeout passed, or
    the context was interrupted (ResourceBudget). The default solver and the qffp tactic solver
    report the same strings."""
    return any(s in reason for s in ('canceled', 'resource', 'timeout', 'interrupted'))


def _satisfies(model, assertions) -> bool:
    """Whether every assertion is true in the model. Evaluates on a copy, as model completion
    adds interpretations to the model it evaluates in, which would change the inputs that
    extract_and_encode_values() reports."""
    completed = copy.copy(model)
    return all(is_true(completed.evaluate(a, model_completion=True)) for a in assertions)


class Z3Handler:
    """Class to handle symbolic solving and related tasks."""

    @staticmethod
    def encode_array(model, array_var, element_type_prefix: str, length: int) -> str:
        """
        Encode a Z3 array to a JSON string representation using proper JSON encoding.

        Args:
        - model: The Z3 model containing the array
        - array_var: The Z3 array variable
        - element_type_prefix: The type prefix for array elements (e.g., 'I' for int)
        - length: The length of the array

        Returns:
        - str: JSON array representation (e.g., "[1, 2, 3]" or '["a", "b", "c"]')
        """
        import json

        elements = []
        for i in range(length):
            # Query the array at index i
            # Use model_completion=True to force Z3 to give concrete values, not symbolic expressions
            # IMPORTANT: Use Python integer i directly - Z3's array indexing automatically
            # converts it to a concrete constant in the correct context. Using Int(i) would
            # create a symbolic variable, which breaks model.eval() for constant arrays.
            element_value = model.eval(array_var[i], model_completion=True)
            # Encode the element using existing encode_value method (returns string)
            # This maintains consistency with how non-array values are encoded
            encoded_str = Z3Handler.encode_value(element_type_prefix, element_value)
            elements.append(encoded_str)

        # Use json.dumps to create a JSON array of strings
        # This ensures proper escaping and structure, while keeping elements as strings
        # for consistent decoding on the Java side using existing parse methods
        return json.dumps(elements, ensure_ascii=False)

    @staticmethod
    def encode_value(prefix: str, value: Any) -> str:
        """
        Encode a Z3 value based on its prefix type.

        Args:
        - prefix (str): The data type prefix (from DataTypes enum).
        - value (Any): The Z3 value to encode.

        Returns:
        - str: The encoded value.
        """
        from z3 import is_bv

        if prefix == DataTypes.BOOLEAN.value:
            return "true" if is_true(value) else "false"
        elif prefix in {DataTypes.CHAR.value, DataTypes.BYTE.value, DataTypes.SHORT.value, DataTypes.INT.value, DataTypes.LONG.value}:
            # Handle both IntNumRef and BitVecNumRef
            from z3 import is_int, is_bv_value, is_int_value

            # Helper function to convert unsigned bitvector to signed Java int/long
            def to_signed(unsigned_value, bit_width):
                """Convert unsigned bitvector value to signed integer."""
                max_signed = (1 << (bit_width - 1)) - 1  # 2^(n-1) - 1
                if unsigned_value > max_signed:
                    # Negative number - convert from unsigned to signed
                    return unsigned_value - (1 << bit_width)  # subtract 2^n
                return unsigned_value

            # Check if it's a numeric value (not symbolic)
            if is_int_value(value) or is_bv_value(value):
                # For numeric values, as_long() gives us unsigned value for bitvectors
                unsigned = value.as_long()

                # Convert to signed based on Java type
                if prefix == DataTypes.BYTE.value:
                    signed = to_signed(unsigned, 8)
                elif prefix == DataTypes.SHORT.value or prefix == DataTypes.CHAR.value:
                    signed = to_signed(unsigned, 16)
                elif prefix == DataTypes.INT.value:
                    signed = to_signed(unsigned, 32)
                elif prefix == DataTypes.LONG.value:
                    signed = to_signed(unsigned, 64)
                else:
                    signed = unsigned

                return str(signed)
            elif is_int(value):
                # It's an integer sort but might be symbolic
                # Try to get the value as string and parse it
                try:
                    return str(value.as_long())
                except:
                    # If it's symbolic, return a default value
                    return "0"
            elif is_bv(value):
                # It's a bit vector - try to get its value
                try:
                    # For concrete bit vectors
                    unsigned = value.as_long()
                    # Determine bit width
                    bit_width = value.size()
                    signed = to_signed(unsigned, bit_width)
                    return str(signed)
                except:
                    # Symbolic bit vector - use default
                    return "0"
            else:
                # Unknown type - fallback
                try:
                    return str(int(str(value)))
                except:
                    return "0"
        elif prefix == DataTypes.FLOAT.value:
            # Check for special values (NaN, Infinity)
            if value.isNaN():
                # IEEE 754: NaN = sign bit (any) | all 1s exponent | non-zero significand
                # Java's Float.NaN = 0x7fc00000 (quiet NaN, positive)
                encoded = 0x7fc00000
                return str(encoded)
            elif value.isInf():
                # IEEE 754: +Infinity = 0x7f800000, -Infinity = 0xff800000
                if value.isPositive():
                    encoded = 0x7f800000
                else:
                    encoded = 0xff800000
                print(f"[DEBUG FLOAT] Z3 value: Infinity -> {encoded:08x}")
                return str(encoded)

            # Extract sign, exponent, and significand from the Z3 float value
            sign = value.sign_as_bv().as_long()
            exponent = value.exponent_as_long()
            significand = value.significand_as_long()
            # Shift and combine
            encoded = (sign << 31) | (exponent << 23) | significand
            return str(encoded)

        elif prefix == DataTypes.DOUBLE.value:
            # Check for special values (NaN, Infinity)
            if value.isNaN():
                # IEEE 754: NaN = sign bit (any) | all 1s exponent | non-zero significand
                # Java's Double.NaN = 0x7ff8000000000000 (quiet NaN, positive)
                encoded = 0x7ff8000000000000
                return str(encoded)
            elif value.isInf():
                # IEEE 754: +Infinity = 0x7ff0000000000000, -Infinity = 0xfff0000000000000
                if value.isPositive():
                    encoded = 0x7ff0000000000000
                else:
                    encoded = 0xfff0000000000000
                return str(encoded)

            # Extract sign, exponent, and significand from the Z3 float value
            sign = value.sign_as_bv().as_long()
            exponent = value.exponent_as_long()
            significand = value.significand_as_long()
            # Shift and combine
            encoded = (sign << 63) | (exponent << 52) | significand

            return str(encoded)
        elif prefix == DataTypes.STRING.value:
            # as_string() escapes some characters as \u{...} but prints a backslash as is, so its
            # output is ambiguous. Read the code points instead.
            n = z3.simplify(z3.Length(value)).as_long()
            return ''.join(chr(z3.simplify(z3.StrToCode(z3.SubString(value, i, 1))).as_long())
                           for i in range(n))
        else:
            raise ValueError(f"Unsupported prefix {prefix} for value {value}")


    @staticmethod
    def extract_and_encode_values(model: Any) -> Dict[str, Dict[str, Union[str, int]]]:
        """
        Extract and encode values from a Z3 model.

        Args:
        - model (Any): The Z3 model.

        Returns:
        - Dict[str, Dict[str, Union[str, int]]]: Dictionary of encoded values.
        """
        encoded_values = {}
        # First pass: collect array lengths
        array_lengths = {}
        from data.Database import Database
        for var_decl in model.decls():
            name = str(var_decl.name())
            if name.endswith('_length') and name.startswith('['):
                # This is an array length variable (e.g., "[I_0_length")
                array_name = name[:-7]  # Remove "_length"
                value = model[var_decl]
                array_lengths[array_name] = value.as_long()

        # Handle arrays that only have length constraints (no element constraints)
        # Create array entries with default values
        for array_name, length in array_lengths.items():
            # Check if the array variable exists in the model
            array_exists = any(str(var_decl.name()) == array_name for var_decl in model.decls())
            if not array_exists:
                # Array is unconstrained - create with default values (all zeros)
                match = re.match(r'\[([A-Z])_(\d+)', array_name)
                if match:
                    element_type = match.group(1)
                    idx = match.group(2)

                    # Create array with default values based on type
                    if element_type == 'Z':  # boolean
                        default_array = ', '.join(['false'] * length)
                    elif element_type in ['B', 'C', 'S', 'I', 'J']:  # numeric types
                        default_array = ', '.join(['0'] * length)
                    elif element_type == 'F':  # float
                        default_array = ', '.join(['0'] * length)
                    elif element_type == 'D':  # double
                        default_array = ', '.join(['0'] * length)
                    else:
                        default_array = ''

                    encoded_values[array_name] = {
                        'encoded_value': f'[{default_array}]',
                        'plain_value': f'unconstrained array of length {length}',
                        'index': idx
                    }

        # Second pass: encode all variables
        for var_decl in model.decls():
            # Extract the variable (as an expression) and its value
            name = str(var_decl.name())
            # Functions (e.g. the executor's Math models, math_tan_3) are not inputs
            if var_decl.arity() > 0:
                continue
            # Updated regex to match array names like "[I_0" as well as regular names
            if not re.match(r'(\[?[\w\/]+)_(\d+)', name):
                continue
            value = model[var_decl]

            # Skip array length variables (handled separately)
            if name.endswith('_length') and name.startswith('['):
                continue

            # Handle array variables: [<element_type>_<idx>
            # Example: [I_0, [Z_1, etc.
            if name.startswith('['):
                # Extract array element type and index
                # Format: [<type>_<idx> where <type> is I, Z, B, etc.
                match = re.match(r'\[([A-Z])_(\d+)', name)
                if match:
                    element_type = match.group(1)
                    idx = match.group(2)

                    # Get array length
                    length = array_lengths.get(name, 1)  # Default to 1 if length not found

                    # Encode array to JSON string
                    encoded_array = Z3Handler.encode_array(model, value, element_type, length)

                    # Store with array prefix (e.g., "[I" for int array)
                    prefix = f'[{element_type}'
                    encoded_values[name] = {
                        'encoded_value': encoded_array,
                        'plain_value': str(value),
                        'index': idx
                    }
                continue

            # Handle list element variables: List_{parent_uid}_{type_prefix}_{element_index}
            # Example: List_20_I_0, List_20_I_1, etc.
            if name.startswith('List_'):
                # Parse list element name: List_{parent_uid}_{type_prefix}_{element_index}
                parts = name.split('_')
                if len(parts) >= 4:
                    # parts = ['List', parent_uid, type_prefix, element_index, ...]
                    parent_uid = parts[1]
                    type_prefix = parts[2]
                    element_index = parts[3]

                    # Use the element type prefix for encoding
                    prefix = type_prefix
                    # Use element index as the index (though it's not used for list elements)
                    idx = element_index
                else:
                    # Malformed list variable
                    raise
            else:
                # Regular variable: prefix_index
                parts = name.split('_', 1)  # Split on first underscore only
                if len(parts) == 2:
                    prefix, idx = parts
                else:
                    # Handle edge case where variable might have multiple underscores
                    # Use rsplit to get the last part as index
                    parts = name.rsplit('_', 1)
                    if len(parts) == 2:
                        prefix, idx = parts
                    else:
                        continue

            encoded_values[name] = {
                'encoded_value': Z3Handler.encode_value(prefix, value),
                'plain_value': value,
                'index': idx
            }
        return encoded_values


    @staticmethod
    def string_to_expr(s: str) -> Any:
        """
        Convert a string constraint to a Z3 expression.

        Args:
        - s (str): String representation of the constraint.

        Returns:
        - Any: Z3 expression corresponding to the string constraint.
        """
        solver = Solver()
        s = s.replace('\\', '')
        solver.from_string(s)
        return solver.assertions()[0]


    @staticmethod
    def write_optimizer_to_file(optimizer: Optimize, output_dir: str = "smt_files") -> str:
        """
        Write the optimizer output to an SMT-LIB format file.

        Args:
        - optimizer (Optimize): The Z3 optimizer instance
        - output_dir (str): Directory to store the SMT files

        Returns:
        - str: Path to the created file
        """
        # Create output directory if it doesn't exist
        os.makedirs(output_dir, exist_ok=True)
        
        # Generate filename with timestamp
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"optimizer_{timestamp}.smt2"
        filepath = os.path.join(output_dir, filename)
        
        # Write optimizer output to file
        with open(filepath, 'w') as f:
            f.write(str(optimizer))
            # Add get-model command after check-sat
            f.write("\n(get-model)\n")
            
        return filepath

    @staticmethod
    def lift_int2bv(cache, assertions: list) -> list:
        """The assertions with comparisons over int_to_bv of string lengths, indices and char codes
        made over the Integers (see Int2BvLifting), which Z3 decides much faster. Equivalent for
        Java strings (at most 2^31 - 1 chars of at most 0xFFFF each); on any error, the assertions
        as they are."""
        from solver.Int2BvLifting import Int2BvLifting, lift_query
        try:
            # One lifting per char code range, as each caches its results across queries.
            java_chars = getattr(cache, 'int2bv_lifting', None)
            if java_chars is None:
                java_chars = cache.int2bv_lifting = Int2BvLifting(java_chars=True)
            if not any(java_chars.mentions_int2bv(a) for a in assertions):
                return assertions
            if all(java_chars.java_chars_only(a) for a in assertions):
                return lift_query(java_chars, assertions)
            # The query itself puts a char above 0xFFFF (or one it cannot bound) into a string.
            unicode = getattr(cache, 'int2bv_lifting_unicode', None)
            if unicode is None:
                unicode = cache.int2bv_lifting_unicode = Int2BvLifting()
            return lift_query(unicode, assertions)
        except Exception as e:
            logger.warning(f"[SOLVER] int_to_bv lifting failed, solving the query as it is: {e}")
            return assertions

    @staticmethod
    def solve(node: Any, path_constraints: list, timeout_ms: int | None = 60 * 1000, rlimit: int | None = None,
              extra_exprs: list | None = None) -> Tuple[SATResult, Dict[str, Any]]:
        """
        Solve for the given node and path constraints using ConstraintCache (modern approach).

        Uses cached parsed constraints to avoid reparsing. All constraints share
        a single Z3 context for consistency.

        Args:
        - node (Any): The node for which to solve.
        - path_constraints (list): List of path constraint SMT strings.
        - timeout_ms: Wall-clock limit for the check, or None.
        - rlimit: Z3 resource limit for the check, or None. Unlike a timeout it is deterministic:
          the same query gets as far regardless of machine load. Running out of it gives LIMIT.
        - extra_exprs: Further Z3 constraints (in the cache's shared context) on the inputs, e.g.
          from retry_constraints().

        Returns:
        - Tuple[SATResult, Dict[str, Any]]: Tuple containing SAT result and solution dictionary.
        """
        from solver.ConstraintCache import get_constraint_cache

        t_start = time.perf_counter()


        # Get the global constraint cache (with shared context)
        cache = get_constraint_cache()

        # Get cached node constraint (already parsed if is_symbolic_branch was called)
        node_cached = cache.get_node_constraint(node)
        node_exprs = node_cached['exprs']


        # Get cached path constraints
        all_exprs = list(node_exprs)  # Start with node constraint
        entries = [node_cached]
        for i, path_constraint_str in enumerate(path_constraints):
            path_cached = cache.get_smt_constraint(path_constraint_str, source=f"path_{i}")
            all_exprs.extend(path_cached['exprs'])
            entries.append(path_cached)
        
        # logger.debug(f"[SOLVER] all_exprs: {all_exprs}")


        # The FIRST set of expressions are from the node constraint - we need to NEGATE them
        # (We're looking for a solution that takes a different path)
        original, query = [], []
        if len(node_exprs) > 0:
            # Negate all node constraint expressions, then add all path constraint expressions
            # (already parsed and cached)
            original = [Not(expr) for expr in node_exprs] + all_exprs[len(node_exprs):] + list(extra_exprs or [])
            query = Z3Handler.lift_int2bv(cache, original[:len(original) - len(extra_exprs or [])]) + list(extra_exprs or [])

        # Create solver with shared context from cache
        solver = Solver(ctx=cache.shared_context)
        solver.add(*query)
        # The lifting neither adds nor removes floating-point terms, so whether the query has
        # any is known from the cached constraints it is built from (each checked once).
        fp_tactic = (bool(query) and getattr(Database.instance(), 'fp_tactic', True)
                     and (any(_entry_has_fp(e) for e in entries) or _has_fp(extra_exprs or [])))

        # Building the solver is Z3 work (SMT-LIB parsing on cache misses, assertion
        # construction), so it counts towards the SMT solver stage rather than the
        # explorer residual. It is not a query, hence count=False.
        t_path = time.perf_counter() - t_start
        TimingManager.instance().record_solver_time(t_path, count=False)

        # Array length constraints are now added in the Java symbolic executor
        # when the array length is made symbolic (InternalInvocation.java)
        # This ensures the constraint is part of the path from the beginning.

        # Log SMT formula to disk for debugging (if enabled)
        if Database.instance().log_smt_formulas:
            try:
                # Determine logs directory
                if hasattr(Database.instance(), 'args') and hasattr(Database.instance().args, 'logdir'):
                    log_dir = Database.instance().args.logdir
                else:
                    log_dir = 'logs'

                os.makedirs(log_dir, exist_ok=True)
                smt_filename = os.path.join(log_dir, f'smt_branch_{node.id}.smt2')

                with open(smt_filename, 'w') as f:
                    f.write(solver.to_smt2())
                logger.info(f"[SOLVER] Saved SMT formula to {smt_filename}")
            except Exception as e:
                logger.warning(f"[SOLVER] Failed to save SMT formula: {e}")

        res, sol, solver, t_check, rlimit_used = Z3Handler.check_query(
            solver, query, original, timeout_ms=timeout_ms, rlimit=rlimit, fp_tactic=fp_tactic)

        if res == SATResult.SAT:
            t_start = time.perf_counter()

            # Log model to disk for debugging (if enabled)
            if Database.instance().log_smt_formulas:
                try:
                    # Determine logs directory
                    if hasattr(Database.instance(), 'args') and hasattr(Database.instance().args, 'logdir'):
                        log_dir = Database.instance().args.logdir
                    else:
                        log_dir = 'logs'

                    os.makedirs(log_dir, exist_ok=True)
                    model_filename = os.path.join(log_dir, f'model_branch_{node.id}.txt')
                    with open(model_filename, 'w') as f:
                        f.write(str(sol))
                    logger.info(f"[SOLVER] Saved model to {model_filename}")
                except Exception as e:
                    logger.warning(f"[SOLVER] Failed to save model: {e}")

            # Sanity check: every assertion must hold under the returned model.
            # Evaluating all assertions is real Z3 work, so only do it when DEBUG
            # logging is actually enabled. (A model of the qffp tactic is checked always, in
            # check_query.) This evaluates in the model itself, so model completion adds the
            # inputs the model left out, which extract_and_encode_values() then reports.
            if logger.isEnabledFor(logging.DEBUG):
                for constraint in solver.assertions():
                    if not is_true(sol.evaluate(constraint, model_completion=True)):
                        logger.warning(f"[SOLVER] Model does not satisfy assertion: {constraint}")

            encoded_sol = Z3Handler.extract_and_encode_values(sol)
            t_encode = time.perf_counter() - t_start
            TimingManager.instance().record_solver_time(t_encode, count=False)
            logger.debug(f"[SOLVER] build={t_path:.3f}s check={t_check:.3f}s model={t_encode:.3f}s rlimit={rlimit_used}")
            return SATResult.SAT, encoded_sol
        logger.debug(f"[SOLVER] build={t_path:.3f}s check={t_check:.3f}s rlimit={rlimit_used} ({res.value})")
        return res, {}

    @staticmethod
    def check_query(solver, query: list, original: list | None = None, timeout_ms: int | None = None,
                    rlimit: int | None = None, fp_tactic: bool = False):
        """Checks a query: `solver` (a default z3.Solver) has `query` asserted already.

        With fp_tactic, the query is first given to the qffp tactic (Tactic('qffp').solver()),
        which bit-blasts floating-point terms eagerly (fpa2bv). The default solver only does
        that for pure QF_FP/QF_FPBV; an uninterpreted function (the SampledMath models) or a
        string term (parseFloat) in the query makes it use the lazy theory_fpa of the SMT core,
        which is far slower on these queries. Whatever the tactic leaves after bit-blasting
        (strings, Int arithmetic, arrays, UFs) goes to the SMT core at the end of its pipeline,
        so it takes every query the default solver does.

        - SAT from the tactic counts only if its model satisfies every assertion of `query` and
          of `original` (the assertions before the int_to_bv lifting); otherwise the default
          solver decides.
        - UNSAT from the tactic is trusted as from the default solver. The tactic pipeline
          (simplify, propagate-values, fpa2bv, bit-blast, SAT/SMT) is a satisfiability-preserving
          transformation by Z3's own rewriters, the same fpa2bv that the default solver applies
          to pure QF_FP queries, so its UNSAT is not less reliable than the default solver's.
          There is no cheap independent check of UNSAT. On a corpus of 748 queries from SV-COMP
          runs (248 of them with FP terms), the two never disagreed on SAT vs UNSAT.
        - An unknown because the check was cut short (rlimit ran out, timeout passed, context
          interrupted by the ResourceBudget) is the result, as from the default solver: falling
          back would give the query its budget twice, and an interrupted check must stay
          stopped. An unknown for any other reason, or an exception, falls back to the default
          solver with what is left of the timeout and rlimit.

        The rlimit counts the same Z3 resource units for both, but the tactic's work per unit
        differs from the SMT core's: on FP queries it decides with fewer units, so a fixed
        rlimit (e.g. --approximate-rlimit for queries over approximate Math models) reaches
        further with the tactic.

        Returns (result, model or None, the solver that decided or None, check seconds, rlimit
        units used)."""
        timing = TimingManager.instance()
        t_check = 0.0
        rlimit_used = 0
        checks = 0

        def run(s, timeout, limit):
            nonlocal t_check, rlimit_used, checks
            if timeout is not None:
                s.set("timeout", timeout)
            if limit is not None:
                # Z3 gives each check this much on top of what the (shared) context has used already.
                s.set("rlimit", limit)
            t = time.perf_counter()
            try:
                return s.check()
            finally:
                dt = time.perf_counter() - t
                t_check += dt
                # A fallback check is the same query again: one query for the timing stats
                timing.record_solver_time(dt, count=checks == 0)
                checks += 1
                rlimit_used += _rlimit_used(s)

        timeout_left, rlimit_left = timeout_ms, rlimit
        if fp_tactic:
            try:
                tactic_solver = z3.Tactic('qffp', ctx=solver.ctx).solver()
                tactic_solver.add(*query)
                res = run(tactic_solver, timeout_ms, rlimit)
                if res == z3.unsat:
                    return SATResult.UNSAT, None, tactic_solver, t_check, rlimit_used
                if res == z3.sat:
                    model = tactic_solver.model()
                    # by id: most of the original assertions are the very terms of the query
                    check = list({a.get_id(): a for a in query + list(original or [])}.values())
                    if _satisfies(model, check):
                        return SATResult.SAT, model, tactic_solver, t_check, rlimit_used
                    logger.warning("[SOLVER] qffp tactic model does not satisfy the query, using the default solver")
                else:
                    reason = tactic_solver.reason_unknown()
                    if _cut_short(reason):
                        return Z3Handler._unknown(reason, rlimit, timeout_ms, t_check), None, tactic_solver, t_check, rlimit_used
                    logger.debug(f"[SOLVER] qffp tactic unknown ({reason}), using the default solver")
            except z3.Z3Exception as e:
                logger.warning(f"[SOLVER] qffp tactic failed ({e}), using the default solver")
            # What is left of the budget for the default solver
            if timeout_ms is not None:
                timeout_left = int(timeout_ms - t_check * 1000)
                if timeout_left <= 0:
                    return SATResult.UNKNOWN, None, None, t_check, rlimit_used
            if rlimit is not None:
                rlimit_left = rlimit - rlimit_used
                if rlimit_left <= 0:
                    return Z3Handler._unknown('canceled', rlimit, timeout_ms, t_check), None, None, t_check, rlimit_used

        res = run(solver, timeout_left, rlimit_left)
        if res == z3.sat:
            return SATResult.SAT, solver.model(), solver, t_check, rlimit_used
        if res == z3.unsat:
            return SATResult.UNSAT, None, solver, t_check, rlimit_used
        return Z3Handler._unknown(solver.reason_unknown(), rlimit, timeout_ms, t_check), None, solver, t_check, rlimit_used

    @staticmethod
    def _unknown(reason: str, rlimit: int | None, timeout_ms: int | None, t_check: float) -> SATResult:
        """LIMIT or UNKNOWN for a check that ended in unknown for `reason`."""
        # With only an rlimit set, a check cut short is reported as "canceled" (older Z3:
        # "max. resource limit exceeded"); anything else is the solver giving up for good.
        if rlimit is not None and timeout_ms is None and ('canceled' in reason or 'resource' in reason):
            logger.debug(f"[SOLVER] resource limit {rlimit} exhausted after {t_check:.3f}s")
            return SATResult.LIMIT
        logger.debug(f"[SOLVER] unknown: {reason}")
        return SATResult.UNKNOWN

    @staticmethod
    def solution_values(sol: Dict[str, Any]) -> Dict[str, Any]:
        """The scalar input values of a solution (from extract_and_encode_values) as Z3 values,
        by name. Arrays are left out: their plain value is only a description."""
        return {name: val['plain_value'] for name, val in sol.items() if z3.is_expr(val.get('plain_value'))}

    @staticmethod
    def retry_constraints(failed: list, bound: int | None = None, ctx=None) -> list:
        """Constraints for re-solving a branch whose earlier solutions `failed` did not lead the
        run to it: each of them is excluded (by its scalar inputs), and with `bound`, every
        integral input is restricted to [-bound, bound] (chars: [0, bound]).

        Both only remove models, so UNSAT under them proves nothing about the branch.
        """
        if ctx is None:
            from solver.ConstraintCache import get_constraint_cache
            ctx = get_constraint_cache().shared_context
        exprs = []
        consts = {}
        for sol in failed:
            values = Z3Handler.solution_values(sol)
            pairs = [(z3.Const(name, v.sort()), v) for name, v in values.items()]
            consts.update((name, c) for (c, _), name in zip(pairs, values))
            # With no scalar input to tell models apart, the failed one cannot be excluded alone.
            exprs.append(z3.Or([c != v for c, v in pairs]) if pairs else z3.BoolVal(False, ctx=ctx))
        if bound is not None:
            for name, c in consts.items():
                if z3.is_bv(c):
                    width = c.size()
                    if name.startswith(DataTypes.CHAR.value + '_'):
                        exprs.append(z3.ULE(c, z3.BitVecVal(min(bound, 2**width - 1), width, ctx=c.ctx)))
                    elif bound < 2**(width - 1):
                        exprs.append(z3.And(c >= -bound, c <= bound))
                elif z3.is_int(c):
                    exprs.append(z3.And(c >= -bound, c <= bound))
        return exprs

    @staticmethod
    def solve_with_optimization(node: Any, path_constraints: list) -> Tuple[SATResult, Dict[str, Any]]:
        """
        Solve for the given node and path constraints using Optimizer to minimize values (slow).

        Args:
        - node (Any): The node for which to solve.
        - path_constraints (list): List of path constraints.

        Returns:
        - Tuple[SATResult, Dict[str, Any]]: Tuple containing SAT result and solution dictionary.
        """
        build_start = time.perf_counter()
        c = Not(Z3Handler.string_to_expr(node.constraint[node.trace_id]))
        optimizer = Optimize()
        optimizer.add(c)

        for path_constraint in path_constraints:
            p = Z3Handler.string_to_expr(path_constraint)
            optimizer.add(p)

        # Apply minimization to reduce variable values
        seen_vars = set()
        for assertion in optimizer.assertions():
            for var in assertion.children():
                if var.decl().kind() == Z3_OP_UNINTERPRETED and var not in seen_vars:
                    seen_vars.add(var)
                    sort_name = var.sort().name()
                    # Skip arrays - optimization is extremely slow for array variables
                    if sort_name.startswith("Array"):
                        continue
                    if sort_name in ["Int", "Real"]:
                        optimizer.minimize(Abs(var))

        optimizer.set("timeout", 60 * 1000)
        # Same accounting as Z3Handler.solve(): building the optimizer is solver work
        # but not a query, so it is recorded with count=False.
        TimingManager.instance().record_solver_time(time.perf_counter() - build_start, count=False)

        smt_file = Z3Handler.write_optimizer_to_file(optimizer)
        print(f"Saved SMT-LIB file to: {smt_file}")

        # Time the SMT solver check
        solver_start = time.perf_counter()
        res = optimizer.check()
        solver_duration = time.perf_counter() - solver_start

        # Record timing
        TimingManager.instance().record_solver_time(solver_duration)

        if str(res) == SATResult.SAT.value:
            model_start = time.perf_counter()
            sol = optimizer.model()
            encoded_sol = Z3Handler.extract_and_encode_values(sol)
            TimingManager.instance().record_solver_time(time.perf_counter() - model_start, count=False)
            return SATResult.SAT, encoded_sol
        else:
            return SATResult.UNSAT if str(res) == SATResult.UNSAT.value else SATResult.UNKNOWN, {}

    @staticmethod
    def solve_opt(node: Any, path_constraints: list) -> Tuple[SATResult, Dict[str, Any]]:
        """
        Solve for the given node and path constraints.
        Uses Optimizer if Database.optimize_solutions is True, otherwise uses regular Solver.

        Args:
        - node (Any): The node for which to solve.
        - path_constraints (list): List of path constraints.

        Returns:
        - Tuple[SATResult, Dict[str, Any]]: Tuple containing SAT result and solution dictionary.
        """
        from data.Database import Database

        if Database.instance().optimize_solutions:
            return Z3Handler.solve_with_optimization(node, path_constraints)
        else:
            return Z3Handler.solve(node, path_constraints)