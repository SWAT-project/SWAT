"""SV-COMP library modules."""

from .dtypes import (
    ExpectedVerdict,
    Verdict,
    VerificationCategory,
    VerificationTask,
    Command,
    VerificationResult,
)
from .selection import extract_testcases, print_testcase_statistics
from .command_gen import generate_commands, generate_command, is_port_available
from .execution import (
    run_parallel,
    run_single_target,
    target_execution,
    check_port_availability,
    ExecutionStatus,
)
from .witness import generate_and_validate_witness
from .analysis.context_loss import main as analyze_context_loss
from .comparison import compare_results
from .benchexec_comparison import (
    collect_result_files,
    load_benchexec_results,
    compare_benchexec_results,
    format_benchexec_diff,
    diff_to_json,
)
from .utils import ci_print
from .cfg_metrics import (
    check_cfg_extractor_available,
    generate_cfg_commands,
    extract_cfg_metrics_batch,
    extract_single_benchmark,
)

__all__ = [
    # Data types
    "ExpectedVerdict",
    "Verdict",
    "VerificationCategory",
    "VerificationTask",
    "Command",
    "VerificationResult",
    # Selection
    "extract_testcases",
    "print_testcase_statistics",
    # Command generation
    "generate_commands",
    "generate_command",
    "is_port_available",
    # Execution
    "run_parallel",
    "run_single_target",
    "target_execution",
    "check_port_availability",
    "ExecutionStatus",
    # Witness
    "generate_and_validate_witness",
    # Analysis
    "analyze_context_loss",
    # Comparison
    "compare_results",
    "collect_result_files",
    "load_benchexec_results",
    "compare_benchexec_results",
    "format_benchexec_diff",
    "diff_to_json",
    # Utils
    "ci_print",
    # CFG Metrics
    "check_cfg_extractor_available",
    "generate_cfg_commands",
    "extract_cfg_metrics_batch",
    "extract_single_benchmark",
]
