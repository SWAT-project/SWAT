"""
Analysis submodule for SV-COMP test execution results.

This submodule provides organized analysis utilities for different aspects
of test execution:

**Timing Analysis (timing.py):**
- Aggregate timing data from all testcases
- Compute average time per testcase for each module
- Performance statistics and bottleneck identification

**Failure Analysis (failures.py):**
- Analyze root causes of test failures
- Track symbolic context loss, precision loss, uncaught exceptions
- Identify patterns in failures

**Performance Analysis (performance.py):**
- Find slowest testcases
- Identify optimization targets

**Context Loss Analysis (context_loss.py):**
- Detailed context loss method extraction
- Missing invocation analysis
- Error categorization

**Usage:**
  # Run all analyses
  from lib.analysis import run_all_analyses
  run_all_analyses()

  # Run specific analyses
  from lib.analysis.timing import TimingAnalysis
  from lib.analysis.failures import FailureAnalysis

  TimingAnalysis.print_timing_statistics()
  FailureAnalysis.print_failure_statistics()
"""

from .timing import TimingAnalysis
from .failures import FailureAnalysis
from .performance import PerformanceAnalysis
from .context_loss import main as analyze_context_loss

__all__ = [
    'TimingAnalysis',
    'FailureAnalysis',
    'PerformanceAnalysis',
    'analyze_context_loss',
    'run_all_analyses'
]


def run_all_analyses(log_base_dir=None):
    """Run all available analyses and print comprehensive report."""
    import logging
    logger = logging.getLogger(__name__)

    logger.info("\n" + "#"*70)
    logger.info("COMPREHENSIVE ANALYSIS REPORT")
    logger.info("#"*70)

    TimingAnalysis.print_timing_statistics(log_base_dir)
    PerformanceAnalysis.print_slowest_testcases(log_base_dir, top_n=10)
    FailureAnalysis.print_failure_statistics(log_base_dir)

    logger.info("\n" + "#"*70)
    logger.info("END OF ANALYSIS REPORT")
    logger.info("#"*70)
