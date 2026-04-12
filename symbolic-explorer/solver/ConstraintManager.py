"""
ConstraintManager: Modern Z3 constraint handling without regex parsing.

This module provides a clean interface for managing Z3 constraints using
the native Z3 Python API instead of brittle string parsing. It supports
both incoming SMT-LIB strings (for Java compatibility) and native Z3 objects.

Key Features:
- Parse SMT-LIB strings once, store as Z3 objects
- Shared Z3 context for efficient solving
- AST traversal for semantic analysis
- Rich debugging and visualization
"""

from z3 import *
from typing import List, Dict, Set, Any, Tuple, Optional
from dataclasses import dataclass, field
import logging

logger = logging.getLogger(__name__)


@dataclass
class ConstraintMetadata:
    """Metadata about a constraint for debugging and analysis."""
    source: str  # Where this constraint came from (e.g., "node_123", "path_constraint")
    trace_id: Optional[int] = None
    smt_original: Optional[str] = None  # Original SMT-LIB string if applicable
    variables: Set[str] = field(default_factory=set)  # Variable names in this constraint
    constraint_type: Optional[str] = None  # e.g., "array_access", "comparison", "logical"


class ConstraintManager:
    """
    Manages Z3 constraints using native Z3 API instead of string parsing.

    This class handles:
    1. Parsing SMT-LIB strings from Java (backward compatibility)
    2. Storing constraints as Z3 ExprRef objects
    3. Building solvers with proper context sharing
    4. Semantic constraint analysis and debugging
    """

    def __init__(self, shared_context: Optional[Context] = None):
        """
        Initialize the constraint manager.

        Args:
            shared_context: Optional Z3 context to share across constraints.
                          If None, creates a new context.
        """
        self.context = shared_context if shared_context is not None else Context()
        self.constraints: List[ExprRef] = []
        self.metadata: List[ConstraintMetadata] = []
        self.declarations: Dict[str, Any] = {}  # Track declared variables

        logger.debug(f"ConstraintManager initialized with context {id(self.context)}")

    def parse_smt_string(self, smt_string: str, source: str = "unknown") -> List[ExprRef]:
        """
        Parse an SMT-LIB string and extract Z3 expressions.

        This is the main entry point for converting Java-generated SMT-LIB
        strings into Z3 objects we can work with directly.

        Args:
            smt_string: SMT-LIB format string (e.g., "(declare-fun x () Int)(assert (> x 0))")
            source: Human-readable source identifier for debugging

        Returns:
            List of Z3 ExprRef objects extracted from the string
        """
        logger.debug(f"Parsing SMT string from {source}: {smt_string[:100]}...")

        try:
            # Create a temporary solver to parse the SMT string
            # This extracts all declarations and assertions
            temp_solver = Solver(ctx=self.context)
            temp_solver.from_string(smt_string)

            # Extract the assertions (these are the actual constraints)
            assertions = temp_solver.assertions()

            logger.debug(f"Successfully parsed {len(assertions)} assertions from {source}")

            # Extract declared variables for tracking
            self._extract_declarations(smt_string)

            return list(assertions)

        except Exception as e:
            logger.error(f"Failed to parse SMT string from {source}: {e}")
            logger.error(f"SMT string was: {smt_string}")
            raise RuntimeError(f"SMT parsing failed: {e}") from e

    def add_constraint_from_smt(self, smt_string: str, source: str = "unknown",
                               trace_id: Optional[int] = None) -> int:
        """
        Add constraint(s) from an SMT-LIB string.

        This method:
        1. Parses the SMT string to Z3 objects
        2. Stores the Z3 objects for later use
        3. Records metadata for debugging

        Args:
            smt_string: SMT-LIB format constraint
            source: Where this constraint came from
            trace_id: Optional trace ID for tracking

        Returns:
            Number of constraints added
        """
        logger.info(f"Adding constraint from SMT (source={source}, trace_id={trace_id})")

        # Parse to Z3 objects
        exprs = self.parse_smt_string(smt_string, source)

        # Store each expression with metadata
        for expr in exprs:
            self.constraints.append(expr)

            # Extract metadata
            variables = self._extract_variables(expr)
            constraint_type = self._classify_constraint(expr)

            metadata = ConstraintMetadata(
                source=source,
                trace_id=trace_id,
                smt_original=smt_string,
                variables=variables,
                constraint_type=constraint_type
            )
            self.metadata.append(metadata)

            logger.debug(f"Added constraint: {expr} (type={constraint_type}, vars={variables})")

        return len(exprs)

    def add_z3_constraint(self, expr: ExprRef, source: str = "unknown",
                         trace_id: Optional[int] = None):
        """
        Add a constraint directly as a Z3 object (for future use).

        Args:
            expr: Z3 expression to add
            source: Where this constraint came from
            trace_id: Optional trace ID
        """
        logger.info(f"Adding Z3 constraint directly (source={source})")

        self.constraints.append(expr)

        variables = self._extract_variables(expr)
        constraint_type = self._classify_constraint(expr)

        metadata = ConstraintMetadata(
            source=source,
            trace_id=trace_id,
            variables=variables,
            constraint_type=constraint_type
        )
        self.metadata.append(metadata)

        logger.debug(f"Added Z3 constraint: {expr} (type={constraint_type})")

    def create_solver(self, additional_constraints: Optional[List[ExprRef]] = None) -> Solver:
        """
        Create a Z3 solver with all stored constraints.

        Args:
            additional_constraints: Optional extra constraints to add

        Returns:
            Z3 Solver instance ready to check()
        """
        logger.info(f"Creating solver with {len(self.constraints)} stored constraints")

        solver = Solver(ctx=self.context)

        # Add all stored constraints
        for constraint in self.constraints:
            solver.add(constraint)

        # Add any additional constraints
        if additional_constraints:
            logger.debug(f"Adding {len(additional_constraints)} additional constraints")
            for constraint in additional_constraints:
                solver.add(constraint)

        logger.debug(f"Solver created with {len(solver.assertions())} total assertions")
        return solver

    def _extract_variables(self, expr: ExprRef) -> Set[str]:
        """
        Extract all variable names from a Z3 expression using AST traversal.

        Args:
            expr: Z3 expression to analyze

        Returns:
            Set of variable names
        """
        variables = set()

        def traverse(e):
            """Recursively traverse the AST."""
            if is_const(e) and e.decl().kind() == Z3_OP_UNINTERPRETED:
                # This is a variable (uninterpreted constant)
                variables.add(str(e))
            elif is_app(e):
                # Traverse all arguments
                for i in range(e.num_args()):
                    traverse(e.arg(i))
            elif is_quantifier(e):
                # Traverse quantifier body
                traverse(e.body())

        traverse(expr)
        return variables

    def _classify_constraint(self, expr: ExprRef) -> str:
        """
        Classify a constraint by its primary operation.

        Args:
            expr: Z3 expression to classify

        Returns:
            String describing the constraint type
        """
        if not is_app(expr):
            return "unknown"

        decl = expr.decl()
        kind = decl.kind()

        # Map Z3 operation kinds to readable names
        if kind == Z3_OP_SELECT:
            return "array_access"
        elif kind in [Z3_OP_EQ, Z3_OP_DISTINCT]:
            return "equality"
        elif kind in [Z3_OP_LT, Z3_OP_LE, Z3_OP_GT, Z3_OP_GE]:
            return "comparison"
        elif kind == Z3_OP_AND:
            return "conjunction"
        elif kind == Z3_OP_OR:
            return "disjunction"
        elif kind == Z3_OP_NOT:
            return "negation"
        elif kind in [Z3_OP_ADD, Z3_OP_SUB, Z3_OP_MUL, Z3_OP_DIV]:
            return "arithmetic"
        else:
            return f"operation_{kind}"

    def _extract_declarations(self, smt_string: str):
        """
        Extract variable declarations from SMT string for tracking.

        Args:
            smt_string: SMT-LIB string containing declarations
        """
        import re

        # Find all (declare-fun ...) patterns
        pattern = r'\(declare-fun\s+(\|[^|]+\||[\w\[\]]+)\s+\([^)]*\)\s+([^)]+)\)'

        for match in re.finditer(pattern, smt_string):
            var_name = match.group(1).strip('|')
            var_type = match.group(2).strip()
            self.declarations[var_name] = var_type
            logger.debug(f"Tracked declaration: {var_name} : {var_type}")

    def get_constraints_by_variable(self, var_name: str) -> List[Tuple[ExprRef, ConstraintMetadata]]:
        """
        Find all constraints involving a specific variable.

        Args:
            var_name: Variable name to search for

        Returns:
            List of (constraint, metadata) tuples
        """
        results = []
        for constraint, metadata in zip(self.constraints, self.metadata):
            if var_name in metadata.variables:
                results.append((constraint, metadata))
        return results

    def get_constraints_by_type(self, constraint_type: str) -> List[Tuple[ExprRef, ConstraintMetadata]]:
        """
        Find all constraints of a specific type.

        Args:
            constraint_type: Type to filter by (e.g., "array_access")

        Returns:
            List of (constraint, metadata) tuples
        """
        results = []
        for constraint, metadata in zip(self.constraints, self.metadata):
            if metadata.constraint_type == constraint_type:
                results.append((constraint, metadata))
        return results

    def debug_print_constraints(self):
        """Print all constraints with metadata for debugging."""
        logger.info("="*70)
        logger.info(f"CONSTRAINT MANAGER DEBUG: {len(self.constraints)} constraints")
        logger.info("="*70)

        for i, (constraint, metadata) in enumerate(zip(self.constraints, self.metadata)):
            logger.info(f"\nConstraint {i}:")
            logger.info(f"  Type: {metadata.constraint_type}")
            logger.info(f"  Source: {metadata.source}")
            logger.info(f"  Trace ID: {metadata.trace_id}")
            logger.info(f"  Variables: {metadata.variables}")
            logger.info(f"  Expression: {constraint}")
            if metadata.smt_original:
                logger.info(f"  Original SMT: {metadata.smt_original[:100]}...")

        logger.info("="*70)

    def clear(self):
        """Clear all stored constraints."""
        self.constraints.clear()
        self.metadata.clear()
        self.declarations.clear()
        logger.info("ConstraintManager cleared")
