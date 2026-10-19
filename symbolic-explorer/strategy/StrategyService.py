import functools
import json
import math
import logging
import re
import time

from data.BinaryExecutionTree.Node import Node

from data.Database import Database
from strategy.DFS import dfs
from solver.SolverHandler import SATResult, Z3Handler
from timing.TimingManager import TimingManager

logger = logging.getLogger(__name__)

# A declaration of a function with at least one argument, i.e. an uninterpreted function rather
# than a variable: "(declare-fun f (<sort> ...) <sort>)".
_UF_DECLARATION = re.compile(r'\(declare-fun\s+(\|[^|]*\||[^\s()]+)\s+\(\s*[^\s)]')

# Prefix of the functions the executor builds approximate Math models from (SampledMath.UF_PREFIX).
APPROXIMATE_UF_PREFIX = 'math_'


@functools.lru_cache(maxsize=4096)
def declared_ufs(smt: str) -> frozenset[str]:
    """The uninterpreted functions an SMT-LIB string declares."""
    return frozenset(name.strip("|") for name in _UF_DECLARATION.findall(smt))


class StrategyService:
    # is_symbolic_branch per (node gid, trace id)
    _symbolic_branches: dict[tuple[int, str], bool] = {}
    
    @staticmethod
    def select_branch(**kwargs):
        endpoint_id = kwargs.get('endpoint_id', None)
        db = Database.instance()
        visited = set()
        # With rank_by_distance, the branches are tried closest-to-an-assert first. Only the order
        # changes, so which branches exist, and hence the verdict, does not. The sort is stable:
        # ties, and branches without SA information (ranked last), keep their DFS order.
        distances = {} if kwargs.get("rank_by_distance", False) else None
        # The walk is on the Database's tree itself, so dfs records where the SA walk fell out of
        # step there directly. The returned nodes are live: they gain children as traces arrive.
        def walk(tree, solved, unsat):
            found = dfs(visited, tree, tree.root, solved, unsat, kwargs.get("sa_node", None), distances=distances)
            return found, tree.loop_state_pruned
        possible_branches, pruned = db.visit_tree(endpoint_id, walk)
        if pruned:
            logger.info(f'[DFS] Left out {pruned} subtrees below a repeated loop-head state')
        if distances:
            possible_branches.sort(key=lambda n: distances.get(n, math.inf))
        
        return possible_branches
        
        
    @staticmethod
    def collect_path_constrains(node: Node) -> list():
        path_constraints = list()
        trace_id = node.trace_id
        
        while node.parent is not None:
            node = node.parent
            if node.kind != "Special":
                if trace_id in node.constraint:
                    c = node.constraint[trace_id]
                    if "declare-fun" in c:
                        path_constraints.append(c)
                        
        return path_constraints
        
    @staticmethod
    def collect_input_constrains(node: Node) -> list():
        path_constraints = list()

        for input in node.inputs:
            path_constraints.append(input.lower_bound)
            path_constraints.append(input.upper_bound)

        return path_constraints

    @staticmethod
    def collect_array_length_constraints(node: Node, max_length: int) -> list():
        """
        Add constraints limiting array lengths to a maximum value.
        This prevents Z3 from generating solutions with unreasonably large arrays.

        Args:
            node: The node containing inputs
            max_length: Maximum allowed array length

        Returns:
            List of SMT-LIB constraints
        """
        constraints = []

        for input in node.inputs:
            # Check if this is an array length variable
            if input.name.endswith('_length') and input.name.startswith('['):
                # Add constraint: array_length <= max_length
                # Escape variable name with pipes for SMT-LIB (names with [ need escaping)
                escaped_name = f'|{input.name}|'
                constraint = f'(assert (<= {escaped_name} {max_length}))'
                constraints.append(constraint)

        return constraints

    @staticmethod
    def collect_uf_definitions(node: Node) -> list():
        uf_definitions = list()
        for uf in node.ufs:
            uf_definitions.append(uf.definition)
        return uf_definitions

    @staticmethod
    def slice_uf_definitions(definitions: list, query: list) -> tuple[list, set[str]]:
        """The definitions the query needs, and the uninterpreted functions it then contains.

        A definition is needed if it mentions a function the query or another needed definition
        contains, or no function at all. The others only constrain functions that occur nowhere
        else in the query; the executor gives every model call its own function, so each of them
        can be satisfied by its function's value whatever the inputs, and leaving it out changes
        neither satisfiability nor the inputs of any model.
        """
        needed = set()
        for smt in query:
            needed |= declared_ufs(smt)
        pending = [(d, declared_ufs(d)) for d in definitions]
        sliced = []
        changed = True
        while changed:
            changed = False
            rest = []
            for d, ufs in pending:
                if not ufs or ufs & needed:
                    sliced.append(d)
                    needed |= ufs
                    changed = True
                else:
                    rest.append((d, ufs))
            pending = rest
        # Keep the order in which the executor recorded them.
        order = {d: i for i, d in enumerate(definitions)}
        sliced.sort(key=lambda d: order[d])
        return sliced, needed

    @staticmethod
    def collect_query(node: Node) -> tuple[list, set[str]]:
        """The constraints of the query for the unexplored side of the branch, besides its own
        constraint, and the uninterpreted functions the query contains."""
        path_constraints = StrategyService.collect_path_constrains(node)
        path_constraints.extend(StrategyService.collect_input_constrains(node))
        node_constraint = node.constraint.get(node.trace_id, '')
        definitions, ufs = StrategyService.slice_uf_definitions(
            StrategyService.collect_uf_definitions(node), path_constraints + [node_constraint])
        path_constraints.extend(definitions)
        return path_constraints, ufs

    @staticmethod
    def is_approximate(ufs) -> bool:
        """Whether a query with these functions depends on an approximate Math model. Only a branch
        constraint brings one in, and the executor reports such a trace as precision loss."""
        return any(uf.startswith(APPROXIMATE_UF_PREFIX) for uf in ufs)

    @staticmethod
    def is_approximate_query(node: Node) -> bool:
        """Whether the query for the unexplored side of the branch depends on an approximate Math model."""
        return StrategyService.is_approximate(StrategyService.collect_query(node)[1])

    @staticmethod
    def solve_branch(possible_branch: Node, endpoint_id=None, solver_timeout_ms: int | None = 60 * 1000,
                     solver_rlimit: int | None = None, extra_exprs: list | None = None,
                     approximate_rlimit: int | None = None):
        """Solves for the unexplored side of the branch. LIMIT (the rlimit ran out) records nothing,
        so the branch stays a candidate and can be retried with a larger limit.

        extra_exprs further restrict the inputs (see Z3Handler.retry_constraints). UNSAT under them
        does not make the branch infeasible, so it is not recorded either.

        A query over an approximate Math model can only give a guess, and a branch whose search
        depends on one never ends in SAFE (the executor reports precision loss). With
        approximate_rlimit, such a query gets at most that resource limit, and running out of it is
        UNKNOWN (given up), not LIMIT."""
        db = Database.instance()

        path_constraints, ufs = StrategyService.collect_query(possible_branch)

        inputs = possible_branch.inputs

        capped = False
        if approximate_rlimit and StrategyService.is_approximate(ufs):
            if solver_rlimit is None or solver_rlimit >= approximate_rlimit:
                solver_rlimit = approximate_rlimit
                capped = True

        sat, sol = Z3Handler.solve(possible_branch, path_constraints, timeout_ms=solver_timeout_ms, rlimit=solver_rlimit,
                                   extra_exprs=extra_exprs)

        if sat == SATResult.LIMIT and capped:
            logger.info(f'[SOLVER] Query over an approximate model for branch {possible_branch.id} ran out of resource limit {solver_rlimit}, giving up on it')
            sat = SATResult.UNKNOWN

        if sat == SATResult.SAT:
            db.add_solution(branch_id=possible_branch.gid, sol=sol, inputs=inputs, endpoint_id=endpoint_id)

        elif sat == SATResult.UNSAT and not extra_exprs:
            db.add_unsat_branch(possible_branch.gid)

        return sat, sol

    @staticmethod
    def is_symbolic_branch(node: Node) -> bool:
        """
        Check if a branch constraint depends on symbolic variables.

        Uses semantic analysis via ConstraintCache instead of regex patterns.
        This approach:
        1. Parses the constraint once and caches it
        2. Extracts variables using Z3 AST traversal
        3. Checks if any constraint variable matches a symbolic input

        Args:
            node: Branch node to check

        Returns:
            True if the branch depends on symbolic variables, False otherwise
        """
        from solver.ConstraintCache import get_constraint_cache

        # The answer depends only on the node's constraint and inputs, which are fixed when the
        # node is created; a branch not taken stays a candidate, so it is asked again every round.
        key = (node.gid, node.trace_id)
        known = StrategyService._symbolic_branches.get(key)
        if known is not None:
            return known

        try:
            # Get cached parsed constraint (parse once, reuse many times).
            # On a cache miss this parses SMT-LIB via Z3 and walks the AST, which is
            # solver work and must not be charged to the explorer residual. It is not
            # a solver query, hence count=False.
            cache = get_constraint_cache()
            t_parse = time.perf_counter()
            cached_data = cache.get_node_constraint(node)
            TimingManager.instance().record_solver_time(time.perf_counter() - t_parse, count=False)

            # Extract variables from the parsed constraint
            constraint_vars = cached_data['variables']

            logger.info(f"[is_symbolic_branch] Branch {node.id}: found {len(constraint_vars)} variables in constraint")
            logger.debug(f"[is_symbolic_branch] Variables: {constraint_vars}")

            # Get input variable names (symbolic variables we're tracking)
            input_vars = set()
            for inp in node.inputs:
                # Add both regular and SMT-LIB escaped forms
                input_vars.add(inp.name)
                # Handle SMT-LIB escaping: [I_0 becomes |[I_0|
                if inp.name.startswith('['):
                    input_vars.add(f"|{inp.name}|")
                # Also handle _length variables for arrays
                if inp.name.startswith('['):
                    input_vars.add(f"{inp.name}_length")
                    input_vars.add(f"|{inp.name}_length|")

            logger.debug(f"[is_symbolic_branch] Input variables: {input_vars}")

            # Check if any constraint variable matches an input variable
            is_symbolic = bool(constraint_vars & input_vars)

            logger.info(f"[is_symbolic_branch] Branch {node.id} is_symbolic: {is_symbolic}")

            StrategyService._symbolic_branches[key] = is_symbolic
            return is_symbolic

        except Exception as e:
            logger.error(f"[is_symbolic_branch] Error checking branch {node.id}: {e}")
            # On error, assume it's symbolic to be safe (won't skip exploration)
            raise e
    
        
        
        
        