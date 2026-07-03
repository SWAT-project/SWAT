"""
Consolidated per-testcase statistics for SV-COMP runs.

The Symbolic Explorer owns the per-testcase output: after a testcase completes it writes a single
``stats.json`` into the log directory containing the verdict, the soundness-relevant flags, and the
methods that could not be modelled symbolically. This replaces the executor-written ``stats_*.json``
files and lets the analysis rely on authoritative structured data instead of scraping logs.

The missing-invocation list is the superset; entries with ``context_loss=True`` are the dangerous
subset that received symbolic arguments and caused symbolic context loss. This makes the
superset/subset relationship explicit (see issue #25), rather than inferring it from regexes.
"""

import json
from pathlib import Path

import log

logger = log.get_logger()


def _signature(inv: dict) -> str:
    """Render a missing invocation as a stable ``owner/name:desc`` signature."""
    return f"{inv['owner']}/{inv['name']}:{inv['desc']}"


def build_testcase_stats(verdict, category, tree) -> dict:
    """
    Assemble the consolidated per-testcase statistics dictionary.

    Args:
        verdict: The final (post-downgrade) Verdict enum for the testcase.
        category: The VerificationCategory enum for the property.
        tree: The per-endpoint Tree holding accumulated metadata for the testcase.

    Returns:
        A JSON-serializable dict describing the testcase outcome.
    """
    # Order: context-loss culprits first, then by signature, for readable output.
    missing = sorted(
        tree.missing_invocations.values(),
        key=lambda i: (not i['context_loss'], i['owner'], i['name'], i['desc']),
    )
    context_loss_methods = sorted({_signature(i) for i in missing if i['context_loss']})

    return {
        'property': category.value,
        'verdict': verdict.name,
        'verdict_raw': verdict.value,
        'soundness': {
            'symbolic_context_loss': tree.symbolic_context_loss,
            'symbolic_precision_loss': tree.symbolic_precision_loss,
            'reference_semantic_change': tree.reference_semantic_change,
            'uncaught_exceptions': tree.uncaught_exceptions,
        },
        'symbolic_vars': sorted(tree.symbolic_vars),
        'missing_invocations': missing,
        'context_loss_methods': context_loss_methods,
        'execution_errors': list(tree.execution_errors),
        'counts': {
            'missing_invocations': len(missing),
            'context_loss_invocations': sum(1 for i in missing if i['context_loss']),
            'execution_errors': len(tree.execution_errors),
        },
    }


def write_testcase_stats(filepath: Path, verdict, category, tree):
    """Write the consolidated per-testcase statistics to ``filepath`` as JSON."""
    data = build_testcase_stats(verdict, category, tree)

    filepath.parent.mkdir(parents=True, exist_ok=True)
    with open(filepath, 'w') as f:
        json.dump(data, f, indent=2)

    logger.info(
        f"[STATS] Saved testcase stats to {filepath} "
        f"({data['counts']['missing_invocations']} missing invocations, "
        f"{data['counts']['context_loss_invocations']} context-loss, "
        f"{data['counts']['execution_errors']} execution errors)"
    )
