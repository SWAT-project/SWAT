"""
Timing analysis for SV-COMP test execution.

Aggregates timing data from timing_data.json files across all testcases
and computes per-testcase average timing for each execution module.
"""

import json
import logging
from pathlib import Path
from typing import Dict, List

logger = logging.getLogger(__name__)

# Module directory (scripts/lib/analysis)
SCRIPT_DIR = Path(__file__).resolve().parent.parent.parent


class TimingAnalysis:
    """Aggregate and analyze timing data from all testcases."""

    @staticmethod
    def collect_timing_files(log_base_dir: Path = None) -> List[Path]:
        """
        Find all timing_data.json files in log directories.

        Args:
            log_base_dir: Base log directory (defaults to scripts/../logs)

        Returns:
            List of paths to timing_data.json files
        """
        if log_base_dir is None:
            log_base_dir = SCRIPT_DIR.parent / 'logs'

        if not log_base_dir.exists():
            logger.warning(f"Log directory does not exist: {log_base_dir}")
            return []

        return list(log_base_dir.rglob('timing_data.json'))

    @staticmethod
    def aggregate_timing_from_files(log_base_dir: Path = None) -> Dict:
        """
        Read all timing_data.json files and compute aggregate statistics.

        Args:
            log_base_dir: Base log directory (defaults to scripts/../logs)

        Returns:
            Dictionary with total and average timing per stage across all testcases
        """
        timing_files = TimingAnalysis.collect_timing_files(log_base_dir)

        if not timing_files:
            logger.warning(f"No timing files found in {log_base_dir or SCRIPT_DIR.parent / 'logs'}")
            return {}

        # Aggregate timing data
        total_timing = {
            'symbolic_executor': 0.0,
            'smt_solver': 0.0,
            'symbolic_explorer': 0.0,
            'witness_generation': 0.0,
            'witness_validation': 0.0
        }

        # Track per-testcase timing for statistics
        per_testcase_timing = []
        testcase_count = 0

        for timing_file in timing_files:
            try:
                with open(timing_file, 'r') as f:
                    timing_data = json.load(f)
                    aggregates = timing_data.get('aggregates', {})

                    # Store per-testcase timing
                    testcase_timing = {stage: aggregates.get(stage, 0.0) for stage in total_timing.keys()}
                    per_testcase_timing.append(testcase_timing)

                    # Add to totals
                    for stage in total_timing.keys():
                        total_timing[stage] += aggregates.get(stage, 0.0)

                    testcase_count += 1
            except Exception as e:
                logger.warning(f"Could not read timing file {timing_file}: {e}")
                continue

        if testcase_count == 0:
            return {}

        # Calculate averages
        avg_timing = {stage: total / testcase_count for stage, total in total_timing.items()}

        # Calculate min/max per stage
        min_timing = {stage: min(tc[stage] for tc in per_testcase_timing) for stage in total_timing.keys()}
        max_timing = {stage: max(tc[stage] for tc in per_testcase_timing) for stage in total_timing.keys()}

        return {
            'testcase_count': testcase_count,
            'total_timing': total_timing,
            'avg_timing': avg_timing,
            'min_timing': min_timing,
            'max_timing': max_timing,
            'per_testcase': per_testcase_timing
        }

    @staticmethod
    def print_timing_statistics(log_base_dir: Path = None):
        """Print comprehensive timing statistics from all timing files."""
        stats = TimingAnalysis.aggregate_timing_from_files(log_base_dir)

        if not stats:
            logger.info("No timing data available for analysis")
            return

        testcase_count = stats['testcase_count']
        total_timing = stats['total_timing']
        avg_timing = stats['avg_timing']
        min_timing = stats['min_timing']
        max_timing = stats['max_timing']

        logger.info("\n" + "="*70)
        logger.info("TIMING ANALYSIS - AGGREGATE STATISTICS")
        logger.info("="*70)
        logger.info(f"Total testcases analyzed: {testcase_count}")
        logger.info("")

        # Per-testcase averages
        logger.info("Average time per testcase (seconds):")
        total_avg = sum(avg_timing.values())
        for stage, time_val in avg_timing.items():
            percent = (time_val / total_avg * 100) if total_avg > 0 else 0
            stage_name = stage.replace('_', ' ').title()
            logger.info(f"  {stage_name:<24} {time_val:>8.2f}s ({percent:>5.1f}%)")
        logger.info(f"  {'Total per testcase:':<24}{total_avg:>8.2f}s")

        logger.info("")
        logger.info("Min/Max time per testcase (seconds):")
        for stage in avg_timing.keys():
            stage_name = stage.replace('_', ' ').title()
            logger.info(f"  {stage_name:<24} min={min_timing[stage]:>7.2f}s, max={max_timing[stage]:>7.2f}s")

        logger.info("")
        logger.info("Total time across all testcases (seconds):")
        grand_total = sum(total_timing.values())
        for stage, time_val in total_timing.items():
            percent = (time_val / grand_total * 100) if grand_total > 0 else 0
            stage_name = stage.replace('_', ' ').title()
            logger.info(f"  {stage_name:<24} {time_val:>8.2f}s ({percent:>5.1f}%)")
        logger.info(f"  {'Grand total:':<24}{grand_total:>8.2f}s")
        logger.info("="*70)
