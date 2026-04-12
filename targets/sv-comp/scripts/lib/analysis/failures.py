"""
Failure analysis for SV-COMP test execution.

Analyzes log files to identify root causes of test failures including:
- Symbolic context loss
- Symbolic precision loss
- Uncaught exceptions
- Reference semantic changes
- Internal SWAT errors
"""

import logging
from pathlib import Path
from typing import Dict, List, Tuple

logger = logging.getLogger(__name__)

# Module directory (scripts/lib/analysis)
SCRIPT_DIR = Path(__file__).resolve().parent.parent.parent


class FailureAnalysis:
    """Analyze reasons for test failures across all testcases."""

    @staticmethod
    def collect_log_files(log_base_dir: Path = None) -> List[Tuple[Path, Path]]:
        """
        Find all log directories and their associated timing files.

        Args:
            log_base_dir: Base log directory

        Returns:
            List of tuples (log_dir, timing_file)
        """
        if log_base_dir is None:
            log_base_dir = SCRIPT_DIR.parent / 'logs'

        if not log_base_dir.exists():
            return []

        result = []
        for timing_file in log_base_dir.rglob('timing_data.json'):
            log_dir = timing_file.parent
            result.append((log_dir, timing_file))

        return result

    @staticmethod
    def analyze_failures(log_base_dir: Path = None) -> Dict:
        """
        Analyze failure reasons across all testcases.

        Looks for:
        - Symbolic context loss
        - Symbolic precision loss
        - Uncaught exceptions
        - Reference semantic changes
        - Other errors

        Args:
            log_base_dir: Base log directory

        Returns:
            Dictionary with failure statistics
        """
        log_dirs = FailureAnalysis.collect_log_files(log_base_dir)
        base = log_base_dir or SCRIPT_DIR.parent / 'logs'

        failure_stats = {
            'symbolic_context_loss': [],
            'symbolic_precision_loss': [],
            'uncaught_exceptions': [],
            'reference_semantic_change': [],
            'internal_errors': [],
            'total_analyzed': 0
        }

        for log_dir, timing_file in log_dirs:
            failure_stats['total_analyzed'] += 1

            # Look for verdict log or main log
            log_files = list(log_dir.glob('*.log'))

            for log_file in log_files:
                try:
                    with open(log_file, 'r') as f:
                        content = f.read()

                        testcase_name = str(log_dir.relative_to(base))

                        if 'Found symbolic context loss' in content:
                            failure_stats['symbolic_context_loss'].append(testcase_name)

                        if 'Found symbolic precision loss' in content:
                            failure_stats['symbolic_precision_loss'].append(testcase_name)

                        if 'Found uncaught exceptions' in content:
                            failure_stats['uncaught_exceptions'].append(testcase_name)

                        if 'Found reference semantic change' in content:
                            failure_stats['reference_semantic_change'].append(testcase_name)

                        if '[SWAT Assertion failed]' in content or 'java.lang.AssertionError: [SWAT]' in content:
                            failure_stats['internal_errors'].append(testcase_name)

                except Exception as e:
                    logger.debug(f"Could not read log file {log_file}: {e}")

        return failure_stats

    @staticmethod
    def print_failure_statistics(log_base_dir: Path = None):
        """Print failure analysis statistics."""
        stats = FailureAnalysis.analyze_failures(log_base_dir)

        logger.info("\n" + "="*70)
        logger.info("FAILURE ANALYSIS - ROOT CAUSE BREAKDOWN")
        logger.info("="*70)
        logger.info(f"Total testcases analyzed: {stats['total_analyzed']}")
        logger.info("")

        categories = [
            ('Symbolic Context Loss', 'symbolic_context_loss'),
            ('Symbolic Precision Loss', 'symbolic_precision_loss'),
            ('Uncaught Exceptions', 'uncaught_exceptions'),
            ('Reference Semantic Change', 'reference_semantic_change'),
            ('Internal SWAT Errors', 'internal_errors')
        ]

        for name, key in categories:
            count = len(stats[key])
            percent = (count / stats['total_analyzed'] * 100) if stats['total_analyzed'] > 0 else 0
            logger.info(f"{name:<30} {count:>5} ({percent:>5.1f}%)")

        logger.info("")
        logger.info("Detailed breakdown:")
        for name, key in categories:
            if stats[key]:
                logger.info(f"\n{name}:")
                for testcase in stats[key][:10]:  # Show first 10
                    logger.info(f"  - {testcase}")
                if len(stats[key]) > 10:
                    logger.info(f"  ... and {len(stats[key]) - 10} more")

        logger.info("="*70)
