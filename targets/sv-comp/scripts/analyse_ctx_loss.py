#!/usr/bin/env python3
"""
Analyze SV-Comp results to identify context loss methods that need implementation.
Run this script in the directory containing the results JSON file.

Usage: python3 analyse_ctx_loss.py results_valid-assert.prp_XXXXXX.json
"""

import json
import sys
import re
from collections import defaultdict
from pathlib import Path


def extract_context_loss_methods(log_content: str) -> list[str]:
    """Extract all method names causing context loss from log content."""
    methods = set()  # Use set to dedupe
    if not log_content:
        return list(methods)

    # Look for "Context loss:" pattern
    for match in re.finditer(r'Context loss:\s*(.+?)(?:\n|$)', log_content):
        methods.add(match.group(1).strip())

    # Also look for "Uninstrumented invocation" pattern
    for match in re.finditer(r'Uninstrumented invocation.*?:\s*(.+?)(?:\n|$)', log_content):
        methods.add(match.group(1).strip())

    # Look for "Context loss recorded" with method info on previous lines
    for match in re.finditer(r'(\S+\.\S+)\s*\n.*Context loss recorded', log_content):
        methods.add(match.group(1).strip())

    return list(methods)


def extract_missing_invocations(log_content: str) -> list[str]:
    """Extract missing/unimplemented method invocations from log content."""
    methods = set()  # Use set to dedupe
    if not log_content:
        return list(methods)

    # Pattern: "Error visiting Instruction INVOKE* owner/class/method:desc"
    # Example: "Error visiting Instruction INVOKEVIRTUAL java/lang/Double/byteValue:()B"
    for match in re.finditer(
        r'Error visiting Instruction (INVOKE\w+)\s+(\S+?)(?:\s*\(|$)',
        log_content
    ):
        invoke_type = match.group(1)
        method_sig = match.group(2)
        # Clean up the method signature (remove id info if present)
        method_sig = re.sub(r'\s*\(id:.*', '', method_sig)
        methods.add(f"{invoke_type} {method_sig}")

    # Pattern: "Not implemented: <method> in <class>"
    for match in re.finditer(r'Not implemented:\s*(.+?)\s+in\s+(\S+)', log_content):
        method = match.group(1).strip()
        cls = match.group(2).strip()
        methods.add(f"NOT_IMPLEMENTED {cls}/{method}")

    return list(methods)


def extract_symbolic_invocations_from_stats(stats_path: Path) -> list[str]:
    """
    Extract symbolic method invocations from stats JSON files.

    Stats files contain entries like:
    {"owner":"java/lang/Character","isSymbolic":true,"name":"isUnicodeIdentifierStart","desc":"(I)Z"}

    Returns list of method signatures that have isSymbolic=true (missing implementations).
    """
    methods = set()

    # Check both stats_1.json and stats_-1.json
    for stats_file in ['stats_1.json', 'stats_-1.json']:
        file_path = stats_path / stats_file
        if not file_path.exists():
            continue

        try:
            with open(file_path) as f:
                data = json.load(f)

            if isinstance(data, list):
                for entry in data:
                    if isinstance(entry, dict) and entry.get('isSymbolic', False):
                        owner = entry.get('owner', '')
                        name = entry.get('name', '')
                        desc = entry.get('desc', '')
                        is_instance = entry.get('isInstance', False)
                        invoke_type = 'INVOKEVIRTUAL' if is_instance else 'INVOKESTATIC'
                        if owner and name:
                            methods.add(f"{invoke_type} {owner}/{name}:{desc}")
        except (json.JSONDecodeError, IOError):
            pass

    return list(methods)


def extract_errors(log_content: str) -> list[str]:
    """Extract error messages from log content."""
    errors = []
    if not log_content:
        return errors

    # Look for SWAT Exception patterns
    for match in re.finditer(r'\[SWAT Exception\]:\s*(.+?)(?:\n|$)', log_content):
        errors.append(match.group(1).strip())

    # Look for general exceptions
    for match in re.finditer(r'Exception.*?:\s*(.+?)(?:\n|$)', log_content):
        msg = match.group(1).strip()
        if msg and len(msg) < 200:
            errors.append(msg)

    return errors


def get_log_dir(logs_dir: Path, task_name: str, property_name: str) -> Path:
    """
    Construct the path to the log directory for a given task.

    Task name format: folder/testname (e.g., "objects/objects03")
    Log dir format: logs/<folder>/<testname>_<property>/
    """
    parts = task_name.split('/')
    if len(parts) >= 2:
        folder = parts[0]
        testname = parts[1]
    else:
        folder = ""
        testname = parts[0]

    # Property name like "valid-assert.prp" -> "valid-assert"
    prop_short = property_name.replace('.prp', '')

    return logs_dir / folder / f"{testname}_{prop_short}"


def get_log_path(logs_dir: Path, task_name: str, property_name: str) -> Path:
    """
    Construct the path to the explorer.log for a given task.
    """
    return get_log_dir(logs_dir, task_name, property_name) / "explorer.log"


def get_stats_dir(logs_dir: Path, task_name: str, property_name: str) -> Path:
    """
    Construct the path to the stats directory (logs/ subdirectory) for a given task.
    """
    return get_log_dir(logs_dir, task_name, property_name) / "logs"


def read_log_file(log_path: Path) -> str:
    """Read log file content, return empty string if not found."""
    try:
        if log_path.exists():
            return log_path.read_text(errors='replace')
    except Exception:
        pass
    return ""


def main():
    if len(sys.argv) < 2:
        # Find the latest results file
        results_dir = Path('.')
        json_files = list(results_dir.glob('results_*.json'))
        if not json_files:
            json_files = list(Path('results').glob('results_*.json'))
        if not json_files:
            print("Usage: python3 analyse_ctx_loss.py <results_file.json>")
            print("No results files found in current directory")
            sys.exit(1)
        results_file = max(json_files, key=lambda p: p.stat().st_mtime)
        print(f"Using latest results file: {results_file}")
    else:
        results_file = Path(sys.argv[1])

    # Determine logs directory (sibling to results file or parent's logs/)
    if results_file.parent.name == 'results':
        logs_dir = results_file.parent.parent / 'logs'
    else:
        logs_dir = results_file.parent / 'logs'

    print(f"Looking for logs in: {logs_dir}")

    with open(results_file) as f:
        data = json.load(f)

    results = data['results']
    category = data.get('category', 'valid-assert.prp')

    # Analyze all 0-point tasks
    zero_points_by_folder = defaultdict(list)
    context_loss_methods = defaultdict(list)
    missing_invocations = defaultdict(list)
    all_errors = defaultdict(list)

    for name, task in results.items():
        # task format: [status_change, points, execution_status, error_bool, validated, time]
        points = task[1]
        if points == 0:
            folder = name.split('/')[0]
            status = task[0]
            exec_status = task[2]

            # Read the actual log file from disk
            log_path = get_log_path(logs_dir, name, category)
            log_content = read_log_file(log_path)

            # Get stats directory for symbolic invocations
            stats_dir = get_stats_dir(logs_dir, name, category)

            zero_points_by_folder[folder].append({
                'name': name,
                'status': status,
                'exec_status': exec_status,
                'log_path': str(log_path),
                'log_content': log_content,
                'log_exists': log_path.exists(),
                'stats_dir': str(stats_dir)
            })

            # Extract context loss methods
            methods = extract_context_loss_methods(log_content)
            for method in methods:
                context_loss_methods[method].append(name)

            # Extract missing invocations from error logs
            invocations = extract_missing_invocations(log_content)
            for inv in invocations:
                missing_invocations[inv].append(name)

            # Extract symbolic invocations from stats JSON files
            symbolic_invocations = extract_symbolic_invocations_from_stats(stats_dir)
            for inv in symbolic_invocations:
                missing_invocations[inv].append(name)

            # Extract errors
            errors = extract_errors(log_content)
            for error in errors:
                all_errors[error].append(name)

    # Print summary by folder
    print("\n" + "=" * 80)
    print("TASKS WITH 0 POINTS BY FOLDER")
    print("=" * 80)
    total_zero = 0
    logs_found = 0
    for folder, tasks in sorted(zero_points_by_folder.items(), key=lambda x: -len(x[1])):
        folder_logs_found = sum(1 for t in tasks if t['log_exists'])
        print(f"{folder}: {len(tasks)} tasks ({folder_logs_found} logs found)")
        total_zero += len(tasks)
        logs_found += folder_logs_found
    print(f"\nTotal: {total_zero} tasks with 0 points ({logs_found} logs found)")

    # Print context loss methods sorted by frequency
    print("\n" + "=" * 80)
    print("CONTEXT LOSS METHODS (sorted by frequency)")
    print("=" * 80)

    if not context_loss_methods:
        print("No context loss methods found in logs.")
    else:
        for method, tasks in sorted(context_loss_methods.items(), key=lambda x: -len(x[1])):
            print(f"\n{len(tasks):3d} tasks - {method}")
            print(f"    Examples: {', '.join(tasks[:3])}")

    # Print missing invocations sorted by frequency
    print("\n" + "=" * 80)
    print("MISSING/UNIMPLEMENTED INVOCATIONS (sorted by frequency)")
    print("=" * 80)

    if not missing_invocations:
        print("No missing invocations found in logs.")
    else:
        for inv, tasks in sorted(missing_invocations.items(), key=lambda x: -len(x[1])):
            # Dedupe task list (same task may appear multiple times)
            unique_tasks = list(dict.fromkeys(tasks))
            print(f"\n{len(unique_tasks):3d} tasks - {inv}")
            print(f"    Examples: {', '.join(unique_tasks[:3])}")

    # Print errors sorted by frequency
    print("\n" + "=" * 80)
    print("ERRORS (sorted by frequency)")
    print("=" * 80)

    if not all_errors:
        print("No specific errors found in logs.")
    else:
        for error, tasks in sorted(all_errors.items(), key=lambda x: -len(x[1]))[:20]:
            print(f"\n{len(tasks):3d} tasks - {error[:100]}")
            print(f"    Examples: {', '.join(tasks[:3])}")

    # Detailed breakdown for each folder
    print("\n" + "=" * 80)
    print("DETAILED BREAKDOWN BY FOLDER")
    print("=" * 80)

    for folder in ['autostub', 'jbmc-regression', 'java-ranger-regression', 'algorithms', 'argv-tasks', 'float-nonlinear-calculation', 'securibench', 'objects']:
        if folder not in zero_points_by_folder:
            continue

        print(f"\n--- {folder} ({len(zero_points_by_folder[folder])} tasks) ---")

        folder_context_loss = defaultdict(list)
        folder_missing_invocations = defaultdict(list)
        folder_errors = defaultdict(list)
        other_issues = defaultdict(list)
        tasks_with_missing_invocations = set()

        for task in zero_points_by_folder[folder]:
            log_content = task['log_content']
            stats_dir = Path(task['stats_dir'])
            methods = extract_context_loss_methods(log_content)
            invocations = extract_missing_invocations(log_content)
            symbolic_invocations = extract_symbolic_invocations_from_stats(stats_dir)
            errors = extract_errors(log_content)

            if methods:
                for method in methods:
                    folder_context_loss[method].append(task['name'])

            all_invocations = invocations + symbolic_invocations
            if all_invocations:
                tasks_with_missing_invocations.add(task['name'])
                for inv in all_invocations:
                    folder_missing_invocations[inv].append(task['name'])

            # Always categorize by verdict/status (not just when no missing invocations)
            if errors:
                for error in errors:
                    folder_errors[error[:80]].append(task['name'])

            if 'timeout' in task['exec_status'].lower():
                other_issues['timeout'].append(task['name'])
            elif not task['log_exists']:
                other_issues['log file not found'].append(task['name'])
            elif not log_content.strip():
                other_issues['log file empty'].append(task['name'])
            elif 'DONT-KNOW' in log_content:
                other_issues['verdict: DONT-KNOW'].append(task['name'])
            elif 'NoThreadContextException' in log_content:
                other_issues['NoThreadContextException'].append(task['name'])
            elif not methods and not all_invocations:
                other_issues['unknown (check log)'].append(task['name'])

        if folder_context_loss:
            print("  Context loss methods:")
            for method, tasks in sorted(folder_context_loss.items(), key=lambda x: -len(x[1]))[:10]:
                unique_tasks = list(dict.fromkeys(tasks))
                print(f"    {len(unique_tasks):3d} - {method[:80]}")

        if folder_missing_invocations:
            print(f"  Missing invocations ({len(tasks_with_missing_invocations)} unique tasks):")
            for inv, tasks in sorted(folder_missing_invocations.items(), key=lambda x: -len(x[1]))[:10]:
                unique_tasks = list(dict.fromkeys(tasks))
                print(f"    {len(unique_tasks):3d} - {inv[:80]}")

        if folder_errors:
            print("  Errors:")
            for error, tasks in sorted(folder_errors.items(), key=lambda x: -len(x[1]))[:10]:
                unique_tasks = list(dict.fromkeys(tasks))
                print(f"    {len(unique_tasks):3d} - {error[:80]}")

        if other_issues:
            print("  Other issues:")
            for issue, tasks in sorted(other_issues.items(), key=lambda x: -len(x[1])):
                unique_tasks = list(dict.fromkeys(tasks))
                print(f"    {len(unique_tasks):3d} - {issue[:80]}")

    # Print sample logs for debugging
    print("\n" + "=" * 80)
    print("SAMPLE LOGS (first 5 tasks with 0 points)")
    print("=" * 80)

    count = 0
    for name, task in results.items():
        if task[1] == 0 and count < 5:
            log_path = get_log_path(logs_dir, name, category)
            log_content = read_log_file(log_path)

            print(f"\n{name}:")
            print(f"  Status: {task[0]}")
            print(f"  Exec status: {task[2]}")
            print(f"  Log path: {log_path}")
            print(f"  Log exists: {log_path.exists()}")
            if log_content:
                # Show last 500 chars which usually has the verdict
                print(f"  Log tail: ...{log_content[-500:]}")
            else:
                print(f"  Log: (empty or not found)")
            count += 1


if __name__ == '__main__':
    main()
