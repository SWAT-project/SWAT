"""
Performance analysis for SV-COMP test execution.

Identifies performance bottlenecks and optimization targets by analyzing
execution times across all testcases.
"""

import json
import logging
from pathlib import Path
from typing import Dict, List, Tuple

from .timing import TimingAnalysis

logger = logging.getLogger(__name__)

# Module directory (scripts/lib/analysis)
SCRIPT_DIR = Path(__file__).resolve().parent.parent.parent


class PerformanceAnalysis:
    """Performance-focused analysis utilities."""

    @staticmethod
    def find_slowest_testcases(log_base_dir: Path = None, top_n: int = 10) -> List[Tuple[str, float, Dict]]:
        """
        Find the slowest testcases by total execution time.

        Args:
            log_base_dir: Base log directory
            top_n: Number of slowest testcases to return

        Returns:
            List of tuples (testcase_name, total_time, timing_breakdown)
        """
        timing_files = TimingAnalysis.collect_timing_files(log_base_dir)
        base = log_base_dir or SCRIPT_DIR.parent / 'logs'

        testcases = []
        for timing_file in timing_files:
            try:
                with open(timing_file, 'r') as f:
                    timing_data = json.load(f)
                    aggregates = timing_data.get('aggregates', {})
                    total_time = aggregates.get('total_time', 0.0)

                    # Get testcase name from path
                    testcase_name = str(timing_file.parent.relative_to(base))

                    testcases.append((testcase_name, total_time, aggregates))
            except Exception as e:
                logger.debug(f"Could not read {timing_file}: {e}")

        # Sort by total time descending
        testcases.sort(key=lambda x: x[1], reverse=True)
        return testcases[:top_n]

    @staticmethod
    def print_slowest_testcases(log_base_dir: Path = None, top_n: int = 10):
        """Print the slowest testcases."""
        slowest = PerformanceAnalysis.find_slowest_testcases(log_base_dir, top_n)

        logger.info("\n" + "="*70)
        logger.info(f"TOP {top_n} SLOWEST TESTCASES")
        logger.info("="*70)

        for i, (name, total_time, breakdown) in enumerate(slowest, 1):
            logger.info(f"\n{i}. {name}")
            logger.info(f"   Total time: {total_time:.2f}s")
            logger.info(f"   Breakdown:")
            logger.info(f"     - Symbolic Executor:   {breakdown.get('symbolic_executor', 0):.2f}s")
            logger.info(f"     - SMT Solver:          {breakdown.get('smt_solver', 0):.2f}s")
            logger.info(f"     - Symbolic Explorer:   {breakdown.get('symbolic_explorer', 0):.2f}s")
            logger.info(f"     - Witness Generation:  {breakdown.get('witness_generation', 0):.2f}s")
            logger.info(f"     - Witness Validation:  {breakdown.get('witness_validation', 0):.2f}s")

        logger.info("="*70)
