"""
Centralized timing manager for tracking execution time across different stages.

This module provides a singleton TimingManager that tracks timing for:
- Symbolic Executor: Time spent running instrumented Java code
- SMT Solver: Time spent in Z3 solving constraints
- Symbolic Explorer: Time spent in Python coordination logic
- Witness Generation: Time spent generating witness files
- Witness Validation: Time spent validating witness files
"""

import json
import time
from typing import Dict, Any, Optional
from pathlib import Path
import logging

logger = logging.getLogger(__name__)


class TimingManager:
    """Singleton class for managing timing data across execution stages."""

    _instance: Optional['TimingManager'] = None

    def __init__(self):
        """Initialize timing storage."""
        self.executor_time: float = 0.0
        self.solver_time: float = 0.0
        self.explorer_time: float = 0.0
        self.witness_generation_time: float = 0.0
        self.witness_validation_time: float = 0.0
        self.total_start_time: Optional[float] = None
        self.total_end_time: Optional[float] = None

        # Counters for statistics
        self.executor_count: int = 0
        self.solver_count: int = 0
        self.explorer_count: int = 0

    @classmethod
    def instance(cls) -> 'TimingManager':
        """Get the singleton instance of TimingManager."""
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    @classmethod
    def reset(cls):
        """Reset the singleton instance (useful for testing)."""
        cls._instance = None

    def start_total_timer(self):
        """Start the total execution timer."""
        self.total_start_time = time.perf_counter()

    def stop_total_timer(self):
        """Stop the total execution timer."""
        self.total_end_time = time.perf_counter()

    def record_executor_time(self, duration: float):
        """
        Record time spent in symbolic executor (Java).

        Args:
            duration: Time in seconds
        """
        self.executor_time += duration
        self.executor_count += 1
        logger.debug(f"[TIMING] Executor: +{duration:.3f}s (total: {self.executor_time:.3f}s)")

    def record_solver_time(self, duration: float):
        """
        Record time spent in SMT solver.

        Args:
            duration: Time in seconds
        """
        self.solver_time += duration
        self.solver_count += 1
        logger.debug(f"[TIMING] Solver: +{duration:.3f}s (total: {self.solver_time:.3f}s)")

    def record_explorer_time(self, duration: float):
        """
        Record time spent in symbolic explorer logic.

        Args:
            duration: Time in seconds
        """
        self.explorer_time += duration
        self.explorer_count += 1
        logger.debug(f"[TIMING] Explorer: +{duration:.3f}s (total: {self.explorer_time:.3f}s)")

    def record_witness_generation_time(self, duration: float):
        """
        Record time spent generating witness files.

        Args:
            duration: Time in seconds
        """
        self.witness_generation_time += duration
        logger.debug(f"[TIMING] Witness Generation: {duration:.3f}s")

    def record_witness_validation_time(self, duration: float):
        """
        Record time spent validating witness files.

        Args:
            duration: Time in seconds
        """
        self.witness_validation_time += duration
        logger.debug(f"[TIMING] Witness Validation: {duration:.3f}s")

    def get_aggregates(self) -> Dict[str, float]:
        """
        Get aggregated timing data.

        Returns:
            Dictionary with total time for each stage
        """
        total_time = 0.0
        if self.total_start_time is not None and self.total_end_time is not None:
            total_time = self.total_end_time - self.total_start_time
        else:
            # Fallback: sum of all components
            total_time = (self.executor_time + self.solver_time + self.explorer_time +
                         self.witness_generation_time + self.witness_validation_time)

        # Compute explorer time as residual: overhead time not accounted for by other stages
        # explorer_time = total_time - executor_time - solver_time - witness_times
        computed_explorer_time = max(0.0, total_time - self.executor_time - self.solver_time -
                                      self.witness_generation_time - self.witness_validation_time)

        return {
            'total_time': total_time,
            'symbolic_executor': self.executor_time,
            'smt_solver': self.solver_time,
            'symbolic_explorer': computed_explorer_time,  # Use computed residual, not measured value
            'witness_generation': self.witness_generation_time,
            'witness_validation': self.witness_validation_time
        }

    def get_statistics(self) -> Dict[str, Any]:
        """
        Get execution statistics.

        Returns:
            Dictionary with counts and averages
        """
        aggregates = self.get_aggregates()

        return {
            'executor_count': self.executor_count,
            'solver_count': self.solver_count,
            'avg_executor_time': aggregates['symbolic_executor'] / self.executor_count if self.executor_count > 0 else 0.0,
            'avg_solver_time': aggregates['smt_solver'] / self.solver_count if self.solver_count > 0 else 0.0,
            # Note: explorer_time is computed as residual, so no per-operation average
        }

    def to_dict(self) -> Dict[str, Any]:
        """
        Convert all timing data to a dictionary for serialization.

        Returns:
            Complete timing data as a dictionary
        """
        aggregates = self.get_aggregates()
        statistics = self.get_statistics()

        return {
            'aggregates': aggregates,
            'statistics': statistics
        }

    def save_to_file(self, filepath: Path):
        """
        Save timing data to a JSON file.

        Args:
            filepath: Path where to save the JSON file
        """
        data = self.to_dict()

        # Ensure parent directory exists
        filepath.parent.mkdir(parents=True, exist_ok=True)

        with open(filepath, 'w') as f:
            json.dump(data, f, indent=2)

        logger.info(f"[TIMING] Saved timing data to {filepath}")

    def print_summary(self):
        """Print a human-readable summary of timing data to the logger."""
        aggregates = self.get_aggregates()
        statistics = self.get_statistics()

        total = aggregates['total_time']

        logger.info("=" * 60)
        logger.info("TIMING SUMMARY".center(60))
        logger.info("=" * 60)
        logger.info(f"Total Execution Time:        {total:>8.2f}s")

        if total > 0:
            logger.info(f"  - Symbolic Executor:       {aggregates['symbolic_executor']:>8.2f}s ({aggregates['symbolic_executor']/total*100:>5.1f}%)")
            logger.info(f"  - SMT Solver:              {aggregates['smt_solver']:>8.2f}s ({aggregates['smt_solver']/total*100:>5.1f}%)")
            logger.info(f"  - Symbolic Explorer:       {aggregates['symbolic_explorer']:>8.2f}s ({aggregates['symbolic_explorer']/total*100:>5.1f}%)")
            logger.info(f"  - Witness Generation:      {aggregates['witness_generation']:>8.2f}s ({aggregates['witness_generation']/total*100:>5.1f}%)")
            logger.info(f"  - Witness Validation:      {aggregates['witness_validation']:>8.2f}s ({aggregates['witness_validation']/total*100:>5.1f}%)")
        else:
            logger.info(f"  - Symbolic Executor:       {aggregates['symbolic_executor']:>8.2f}s")
            logger.info(f"  - SMT Solver:              {aggregates['smt_solver']:>8.2f}s")
            logger.info(f"  - Symbolic Explorer:       {aggregates['symbolic_explorer']:>8.2f}s")
            logger.info(f"  - Witness Generation:      {aggregates['witness_generation']:>8.2f}s")
            logger.info(f"  - Witness Validation:      {aggregates['witness_validation']:>8.2f}s")

        logger.info("")
        logger.info("Detailed Breakdown:")
        logger.info(f"  - Executor runs:           {statistics['executor_count']} rounds, avg {statistics['avg_executor_time']:.2f}s per round")
        logger.info(f"  - SMT solver calls:        {statistics['solver_count']} calls, avg {statistics['avg_solver_time']:.2f}s per call")
        logger.info(f"  - Explorer (overhead):     Computed as residual time")
        logger.info("=" * 60)
