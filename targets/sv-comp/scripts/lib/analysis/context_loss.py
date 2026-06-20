#!/usr/bin/env python3
"""
Analyze a SV-COMP run from its consolidated per-testcase ``stats.json`` files.

A run lives under ``runs/run_<timestamp>/`` with:
  - ``results/results_<category>_<ts>.json`` : per-target verdict / points / timing
  - ``logs/<rel>/<name>_<cat>/stats.json``   : per-testcase missing invocations,
                                               context-loss subset, execution errors

This reads that structured data directly — no log scraping — and prints a scoring
summary (Correct / Failed / Unk / Error / Timeout) plus the missing-invocation
superset and its authoritative context-loss subset (issue #25).
"""

import json
import sys
from collections import defaultdict
from pathlib import Path
from typing import Optional

# lib/analysis/context_loss.py -> scripts/
SCRIPT_DIR = Path(__file__).resolve().parents[2]

SCORE_BUCKETS = ['Correct', 'Failed', 'Unk', 'Error', 'Timeout']


def find_latest_run(runs_root: Path) -> Optional[Path]:
    """Return the most recently modified runs/run_* directory, or None."""
    if not runs_root.exists():
        return None
    run_dirs = [p for p in runs_root.glob('run_*') if p.is_dir()]
    if not run_dirs:
        return None
    return max(run_dirs, key=lambda p: p.stat().st_mtime)


def classify(case: str, points, exec_status, error) -> str:
    """Bucket a single result into Correct / Failed / Unk / Error / Timeout.

    Precedence: a timeout or a crash/error is reported as such regardless of points;
    otherwise positive points are Correct, negative points are Failed (wrong verdict),
    and zero points are Unknown (unknown / non-symbolic).
    """
    s = str(exec_status).lower()
    if 'timeout' in s:
        return 'Timeout'
    if 'error' in s or error or 'crash' in str(case).lower():
        return 'Error'
    if points and points > 0:
        return 'Correct'
    if points and points < 0:
        return 'Failed'
    return 'Unk'


def load_json(path: Path):
    """Load a JSON file, returning None if it is missing or unreadable."""
    try:
        with open(path) as f:
            return json.load(f)
    except (OSError, json.JSONDecodeError):
        return None


def stats_path_for(logs_dir: Path, target: str, category: str) -> Path:
    """Path to a testcase's stats.json, derived from its target and property."""
    cat_short = category.replace('.prp', '')
    return logs_dir / f"{target}_{cat_short}" / 'stats.json'


def signature(inv: dict) -> str:
    """Render a missing invocation as a stable owner/name:desc signature."""
    return f"{inv['owner']}/{inv['name']}:{inv['desc']}"


def analyze_run(run_dir: Path):
    """Aggregate and print the analysis for a single run directory."""
    run_dir = Path(run_dir)
    results_dir = run_dir / 'results'
    logs_dir = run_dir / 'logs'

    results_files = sorted(results_dir.glob('results_*.json'))
    if not results_files:
        print(f"No results files found in {results_dir}")
        return

    print(f"Analyzing run: {run_dir.name}")
    print(f"  results: {results_dir}")
    print(f"  logs:    {logs_dir}\n")

    # Scoring buckets per category, and total points per category.
    score: dict = {}
    points_by_cat: dict = {}

    # Missing-invocation aggregation across all testcases of the run.
    # signature -> {tasks: set, count: int, context_loss: bool, isSymbolic: bool}
    missing: dict = {}
    exec_errors: dict = defaultdict(list)  # message -> [tasks]

    total_tasks = 0
    stats_missing = 0  # testcases with no stats.json on disk

    for rf in results_files:
        data = load_json(rf)
        if not data:
            continue
        category = data.get('category', rf.stem)
        cat_buckets: dict = defaultdict(int)

        for target, tup in data.get('results', {}).items():
            total_tasks += 1
            case = tup[0] if len(tup) > 0 else ''
            points = tup[1] if len(tup) > 1 else 0
            exec_status = tup[2] if len(tup) > 2 else ''
            error = tup[3] if len(tup) > 3 else False
            cat_buckets[classify(case, points, exec_status, error)] += 1

            stats = load_json(stats_path_for(logs_dir, target, category))
            if stats is None:
                stats_missing += 1
                continue
            for inv in stats.get('missing_invocations', []):
                sig = signature(inv)
                m = missing.setdefault(
                    sig, {'tasks': set(), 'count': 0, 'context_loss': False, 'isSymbolic': False})
                m['tasks'].add(target)
                m['count'] += inv.get('count', 1)
                m['context_loss'] = m['context_loss'] or inv.get('context_loss', False)
                m['isSymbolic'] = m['isSymbolic'] or inv.get('isSymbolic', False)
            for e in stats.get('execution_errors', []):
                exec_errors[e.get('message', '')].append(target)

        score[category] = dict(cat_buckets)
        points_by_cat[category] = data.get('points')

    _print_scoring_summary(score, points_by_cat, total_tasks)
    _print_missing_invocations(missing)
    _print_execution_errors(exec_errors)

    if stats_missing:
        print(f"\nNote: {stats_missing} testcase(s) had no stats.json "
              f"(crash before the explorer wrote one, or an older run).")


def _print_scoring_summary(score: dict, points_by_cat: dict, total_tasks: int):
    print("=" * 78)
    print("SCORING SUMMARY")
    print("=" * 78)
    header = f"{'Category':<26}" + "".join(f"{b:>9}" for b in SCORE_BUCKETS) + f"{'Total':>8}{'Points':>9}"
    print(header)
    print("-" * len(header))

    totals = {b: 0 for b in SCORE_BUCKETS}
    total_points = 0
    for category in sorted(score):
        buckets = score[category]
        row_total = sum(buckets.get(b, 0) for b in SCORE_BUCKETS)
        pts = points_by_cat.get(category) or 0
        total_points += pts
        for b in SCORE_BUCKETS:
            totals[b] += buckets.get(b, 0)
        if row_total == 0:
            continue  # don't clutter the table with categories that had no testcases
        cells = "".join(f"{buckets.get(b, 0):>9}" for b in SCORE_BUCKETS)
        print(f"{category:<26}{cells}{row_total:>8}{pts:>9}")

    print("-" * len(header))
    grand_total = sum(totals.values())
    cells = "".join(f"{totals[b]:>9}" for b in SCORE_BUCKETS)
    print(f"{'TOTAL':<26}{cells}{grand_total:>8}{total_points:>9}")
    print()


def _print_missing_invocations(missing: dict):
    print("=" * 78)
    cl = {sig: m for sig, m in missing.items() if m['context_loss']}
    print(f"MISSING INVOCATIONS  (superset: {len(missing)} signatures; "
          f"context-loss subset: {len(cl)})")
    print("=" * 78)
    if not missing:
        print("None.")
        print()
        return

    # Most-impactful first: by number of testcases, then total count.
    for sig, m in sorted(missing.items(), key=lambda kv: (-len(kv[1]['tasks']), -kv[1]['count'])):
        marker = "CL " if m['context_loss'] else "   "
        print(f"  {marker}{len(m['tasks']):>4} tasks  (count={m['count']:<5}) {sig}")
    print()

    if cl:
        print("-" * 78)
        print(f"CONTEXT-LOSS METHODS (subset that caused symbolic context loss):")
        for sig in sorted(cl):
            print(f"    {sig}")
        print()


def _print_execution_errors(exec_errors: dict):
    if not exec_errors:
        return
    print("=" * 78)
    print(f"EXECUTION ERRORS  ({len(exec_errors)} distinct)")
    print("=" * 78)
    for msg, tasks in sorted(exec_errors.items(), key=lambda kv: -len(kv[1])):
        unique = list(dict.fromkeys(tasks))
        print(f"  {len(unique):>4} tasks  {msg[:90]}")
        print(f"           e.g. {', '.join(unique[:3])}")
    print()


def main():
    """Standalone entry point: analyze a run dir argument or the latest run."""
    if len(sys.argv) >= 2:
        run_dir = Path(sys.argv[1])
    else:
        run_dir = find_latest_run(SCRIPT_DIR / '..' / 'runs')
        if run_dir is None:
            print("Usage: python3 -m lib.analysis.context_loss <run_dir>")
            print("No runs found under runs/")
            sys.exit(1)
        print(f"Using latest run: {run_dir}")
    analyze_run(run_dir)


if __name__ == '__main__':
    main()
