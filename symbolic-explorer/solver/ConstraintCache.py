"""
ConstraintCache: Caches parsed Z3 constraints to avoid reparsing during exploration.

This module provides a caching layer on top of ConstraintManager to ensure that
constraints are only parsed once during an exploration session. This significantly
improves performance by:
1. Avoiding redundant SMT-LIB parsing
2. Maintaining a single shared Z3 context across all constraints
3. Caching variable extraction for symbolic branch detection

Key Features:
- Parse-once semantics for both node and path constraints
- Shared Z3 context for constraint consistency
- Hash-based caching for path constraints (SMT strings)
- Automatic variable extraction and metadata
"""

from z3 import Context
from typing import Dict, Set, List, Tuple, Any
from solver.ConstraintManager import ConstraintManager
import logging

logger = logging.getLogger(__name__)


class ConstraintCache:
    """
    Caches parsed Z3 constraints to avoid reparsing during exploration.

    This cache maintains:
    - A shared Z3 context for all constraints (critical for consistency)
    - Parsed Z3 expressions for node constraints
    - Parsed Z3 expressions for path constraints (SMT strings)
    - Extracted variable names for quick symbolic branch detection
    """

    def __init__(self):
        """Initialize the constraint cache with a shared Z3 context."""
        self._node_cache: Dict[Tuple[int, int], Dict[str, Any]] = {}
        self._smt_cache: Dict[int, Dict[str, Any]] = {}
        self._shared_context = Context()
        self._shared_mgr = ConstraintManager(shared_context=self._shared_context)

        logger.info(f"ConstraintCache initialized with shared context {id(self._shared_context)}")

    @property
    def shared_context(self) -> Context:
        """Get the shared Z3 context for all cached constraints."""
        return self._shared_context

    @property
    def shared_manager(self) -> ConstraintManager:
        """Get the shared ConstraintManager instance."""
        return self._shared_mgr

    def get_node_constraint(self, node: Any) -> Dict[str, Any]:
        """
        Get parsed constraint data for a node.

        Args:
            node: Node object with constraint and trace_id

        Returns:
            Dict with keys:
                - 'exprs': List of Z3 ExprRef objects
                - 'variables': Set of variable names found in constraint
                - 'smt': Original SMT-LIB string
        """
        key = (node.id, node.trace_id)

        if key not in self._node_cache:
            constraint_str = node.constraint[node.trace_id]
            logger.debug(f"Cache MISS for node {node.id}, parsing constraint")
            self._node_cache[key] = self._parse_and_extract(constraint_str, source=f"node_{node.id}")
        else:
            logger.debug(f"Cache HIT for node {node.id}")

        return self._node_cache[key]

    def get_smt_constraint(self, smt_string: str, source: str = "path") -> Dict[str, Any]:
        """
        Get parsed constraint data for an SMT-LIB string (typically path constraints).

        Args:
            smt_string: SMT-LIB format constraint string
            source: Human-readable source identifier for debugging

        Returns:
            Dict with keys:
                - 'exprs': List of Z3 ExprRef objects
                - 'variables': Set of variable names found in constraint
                - 'smt': Original SMT-LIB string
        """
        key = hash(smt_string)

        if key not in self._smt_cache:
            logger.debug(f"Cache MISS for SMT constraint (source={source}), parsing")
            self._smt_cache[key] = self._parse_and_extract(smt_string, source=source)
        else:
            logger.debug(f"Cache HIT for SMT constraint (source={source})")

        return self._smt_cache[key]

    def _parse_and_extract(self, smt_string: str, source: str) -> Dict[str, Any]:
        """
        Parse an SMT-LIB string and extract variables.

        Args:
            smt_string: SMT-LIB format constraint
            source: Source identifier for logging

        Returns:
            Dict with parsed expressions, variables, and original SMT string
        """
        try:
            # Parse using shared ConstraintManager
            exprs = self._shared_mgr.parse_smt_string(smt_string, source=source)

            # Extract all variables from parsed expressions
            variables = set()
            for expr in exprs:
                variables.update(self._shared_mgr._extract_variables(expr))

            logger.info(f"Parsed constraint from {source}: {len(exprs)} expressions, {len(variables)} variables")

            return {
                'exprs': exprs,
                'variables': variables,
                'smt': smt_string
            }

        except Exception as e:
            logger.error(f"Failed to parse constraint from {source}: {e}")
            logger.error(f"SMT string: {smt_string[:200]}...")
            # Return empty result on failure
            return {
                'exprs': [],
                'variables': set(),
                'smt': smt_string
            }

    def clear(self):
        """Clear all cached constraints. Use between exploration sessions."""
        logger.info(f"Clearing constraint cache: {len(self._node_cache)} node constraints, {len(self._smt_cache)} SMT constraints")
        self._node_cache.clear()
        self._smt_cache.clear()

    def stats(self) -> Dict[str, int]:
        """Get cache statistics."""
        return {
            'node_cache_size': len(self._node_cache),
            'smt_cache_size': len(self._smt_cache),
            'total_cached': len(self._node_cache) + len(self._smt_cache)
        }


# Global constraint cache instance for the exploration session
_global_cache: ConstraintCache = None


def get_constraint_cache() -> ConstraintCache:
    """
    Get the global constraint cache instance.

    Creates a new cache if one doesn't exist. The cache persists for the
    entire exploration session to maximize reuse.

    Returns:
        Global ConstraintCache instance
    """
    global _global_cache
    if _global_cache is None:
        _global_cache = ConstraintCache()
        logger.info("Created global constraint cache")
    return _global_cache


def clear_constraint_cache():
    """Clear the global constraint cache. Call between exploration sessions."""
    global _global_cache
    if _global_cache is not None:
        _global_cache.clear()
        logger.info("Cleared global constraint cache")
