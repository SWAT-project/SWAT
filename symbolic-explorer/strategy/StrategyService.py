import json
import logging

from data.BinaryExecutionTree.Node import Node

from data.Database import Database
from strategy.DFS import dfs
from solver.SolverHandler import SATResult, Z3Handler

logger = logging.getLogger(__name__)


class StrategyService:
    
    @staticmethod
    def select_branch(**kwargs):
        endpoint_id = kwargs.get('endpoint_id', None)
        db = Database.instance()
        tree = db.get_tree(endpoint_id)
        visited = set()
        possible_branches = dfs(visited, tree, tree.root, db.get_solutions(), db.get_unsat_branches())
        
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
    def solve_branch(possible_branch: Node, endpoint_id=None):
        db = Database.instance()

        path_constraints = StrategyService.collect_path_constrains(possible_branch)

        path_constraints.extend(StrategyService.collect_input_constrains(possible_branch))

        path_constraints.extend(StrategyService.collect_uf_definitions(possible_branch))

        inputs = possible_branch.inputs

        sat, sol = Z3Handler.solve(possible_branch, path_constraints)

        if sat == SATResult.SAT:
            db.add_solution(branch_id=possible_branch.gid, sol=sol, inputs=inputs, endpoint_id=endpoint_id)

        elif sat == SATResult.UNSAT:
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

        try:
            # Get cached parsed constraint (parse once, reuse many times)
            cache = get_constraint_cache()
            cached_data = cache.get_node_constraint(node)

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

            return is_symbolic

        except Exception as e:
            logger.error(f"[is_symbolic_branch] Error checking branch {node.id}: {e}")
            # On error, assume it's symbolic to be safe (won't skip exploration)
            raise e
    
        
        
        
        