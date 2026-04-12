import re
import time
from svcomp.SymbolicStorage import DataTypes
from z3 import Solver, is_true, Not, Optimize, Z3_OP_UNINTERPRETED, Abs
from enum import Enum
from typing import Union, Dict, Tuple, Any
from datetime import datetime
import os
from timing.TimingManager import TimingManager
import logging
from data.Database import Database

logger = logging.getLogger("explorer.log")


class SATResult(Enum):
    SAT = 'sat'
    UNSAT = 'unsat'
    UNKNOWN = 'unknown'
    
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
            return value.as_string()
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
    def solve(node: Any, path_constraints: list) -> Tuple[SATResult, Dict[str, Any]]:
        """
        Solve for the given node and path constraints using ConstraintCache (modern approach).

        Uses cached parsed constraints to avoid reparsing. All constraints share
        a single Z3 context for consistency.

        Args:
        - node (Any): The node for which to solve.
        - path_constraints (list): List of path constraint SMT strings.

        Returns:
        - Tuple[SATResult, Dict[str, Any]]: Tuple containing SAT result and solution dictionary.
        """
        import time
        from solver.ConstraintCache import get_constraint_cache

        t_start = time.time()


        # Get the global constraint cache (with shared context)
        cache = get_constraint_cache()

        # Get cached node constraint (already parsed if is_symbolic_branch was called)
        node_cached = cache.get_node_constraint(node)
        node_exprs = node_cached['exprs']


        # Get cached path constraints
        all_exprs = list(node_exprs)  # Start with node constraint
        for i, path_constraint_str in enumerate(path_constraints):
            path_cached = cache.get_smt_constraint(path_constraint_str, source=f"path_{i}")
            all_exprs.extend(path_cached['exprs'])


        # Create solver with shared context from cache
        solver = Solver(ctx=cache.shared_context)

        # The FIRST set of expressions are from the node constraint - we need to NEGATE them
        # (We're looking for a solution that takes a different path)
        if len(node_exprs) > 0:
            # Negate all node constraint expressions and add them
            for expr in node_exprs:
                negated = Not(expr)
                solver.add(negated)

            # Add all path constraint expressions (already parsed and cached)
            for i, path_expr in enumerate(all_exprs[len(node_exprs):]):
                solver.add(path_expr)

        t_path = time.time() - t_start

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

        solver.set("timeout", 60 * 1000)
        t_start = time.time()
        res = solver.check()
        t_check = time.time() - t_start


        if str(res) == SATResult.SAT.value:
            t_start = time.time()
            sol = solver.model()

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

            # DEBUG: Print the full Z3 model
            for decl in sol.decls():
                var_name = decl.name()
                var_value = sol[decl]

                # For arrays, show first few elements
                if '[' in str(var_name):
                    from z3 import Int as Z3Int
                    try:
                        # Get context from the array to avoid context mismatch
                        ctx = var_value.ctx if hasattr(var_value, 'ctx') else None
                        for i in range(min(5, 10)):  # Show first 5 elements
                            idx = Z3Int(i, ctx=ctx) if ctx is not None else Z3Int(i)
                            elem_val = sol.eval(var_value[idx], model_completion=True)
                    except:
                        pass

            # DEBUG: Verify model satisfies constraints
            for i, constraint in enumerate(solver.assertions()):
                satisfied = sol.evaluate(constraint, model_completion=True)
                status = "✓" if satisfied else "✗"

            encoded_sol = Z3Handler.extract_and_encode_values(sol)
            t_encode = time.time() - t_start
            return SATResult.SAT, encoded_sol
        else:
            return SATResult.UNSAT if str(res) == SATResult.UNSAT.value else SATResult.UNKNOWN, {}

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
        smt_file = Z3Handler.write_optimizer_to_file(optimizer)
        print(f"Saved SMT-LIB file to: {smt_file}")

        # Time the SMT solver check
        solver_start = time.perf_counter()
        res = optimizer.check()
        solver_duration = time.perf_counter() - solver_start

        # Record timing
        TimingManager.instance().record_solver_time(solver_duration)

        if str(res) == SATResult.SAT.value:
            sol = optimizer.model()
            encoded_sol = Z3Handler.extract_and_encode_values(sol)
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