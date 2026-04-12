#!/usr/bin/env python3
"""
Analyze timing data grouped by verdict correctness.
"""
import json
from pathlib import Path
import statistics

# Find most recent results file
results_dir = Path(__file__).resolve().parent.parent / 'results'
results_files = sorted(results_dir.glob('*.json'), key=lambda p: p.stat().st_mtime, reverse=True)

if not results_files:
    print("No results files found!")
    exit(1)

results_file = results_files[0]
print(f"Loading: {results_file.name}")
print(f"Note: Filtering out testcases with solver_count == 0")

# Extract property from results filename (e.g., "valid-assert" from "results_valid-assert.prp_...")
results_filename = results_file.name
property_suffix = None
if results_filename.startswith("results_") and ".prp_" in results_filename:
    property_part = results_filename[len("results_"):results_filename.index(".prp_")]
    property_suffix = "_" + property_part
    print(f"Detected property suffix: {property_suffix}")

# Debug: count how many we skip
skipped_count = 0
processed_count = 0

# Load results
with open(results_file) as f:
    results = json.load(f)

# Load timing data
log_dir = Path(__file__).resolve().parent.parent / 'logs'
timing_files = {tf.parent.name: tf for tf in log_dir.rglob('timing_data.json')}

# Group testcases by different categories
correct = []           # All correct: expected == actual (TP + TN)
true_correct = []      # True Positives: violation -> violation
true_negative = []     # True Negatives: safe -> safe
false_results = []     # False Positives + False Negatives: expected != actual (excluding unknown/crash)
incorrect = []         # All incorrect: expected != actual (including unknown/crash)

# Parse results structure: results[testcase_name] = [case_occurrence, points, status, dse_error, witness, elapsed, timing]
for testcase_name, result_data in results.get('results', {}).items():
    if not isinstance(result_data, list) or len(result_data) < 7:
        continue

    case_occurrence = result_data[0]  # e.g., "safe -> unknown" or "violation -> violation"
    timing_breakdown = result_data[6]  # Dict with timing data

    # Parse expected -> actual from case_occurrence
    parts = case_occurrence.split(' -> ')
    if len(parts) != 2:
        continue
    expected, actual = parts

    # Get timing data
    try:
        # Convert testcase name to directory name
        # e.g., 'objects/objects08' -> 'objects08_valid-assert'
        if '/' in testcase_name:
            # Remove category prefix (everything before and including '/')
            testcase_short = testcase_name.split('/')[-1]
        else:
            testcase_short = testcase_name

        # Add property suffix if detected
        if property_suffix:
            testcase_dir = testcase_short + property_suffix
        else:
            testcase_dir = testcase_short

        # Check if timing file exists to get solver_count
        timing_file = timing_files.get(testcase_dir)
        if timing_file:
            with open(timing_file) as f:
                timing_data_full = json.load(f)
                solver_count = timing_data_full.get('statistics', {}).get('solver_count', 0)
                # Skip testcases with zero solver calls
                if solver_count == 0:
                    skipped_count += 1
                    continue
                else:
                    processed_count += 1
        else:
            # No timing file found, skip this testcase
            skipped_count += 1
            continue

        total_time = timing_breakdown.get('total_time', 0)
        executor = timing_breakdown.get('symbolic_executor', 0)
        solver = timing_breakdown.get('smt_solver', 0)
        witness_gen = timing_breakdown.get('witness_generation', 0)
        witness_val = timing_breakdown.get('witness_validation', 0)

        # Compute corrected explorer time as residual
        corrected_explorer = max(0.0, total_time - executor - solver - witness_gen - witness_val)

        timing_entry = {
            'name': testcase_name,
            'total': total_time,
            'executor': executor,
            'solver': solver,
            'explorer': corrected_explorer,
            'witness_gen': witness_gen,
            'witness_val': witness_val,
            'expected': expected,
            'actual': actual,
            'case': case_occurrence
        }

        # Categorize into groups
        is_correct = (expected == actual)

        if is_correct:
            correct.append(timing_entry)
            if actual == 'violation':  # True Positive
                true_correct.append(timing_entry)
            elif actual == 'safe':  # True Negative
                true_negative.append(timing_entry)

        if not is_correct:
            incorrect.append(timing_entry)
            # False results = incorrect but not unknown/crash
            if actual not in ['unknown', 'crash']:
                false_results.append(timing_entry)

    except Exception as e:
        print(f"Error processing {testcase_name}: {e}")

print(f"\n{'='*70}")
print(f"FILTERING RESULTS")
print(f"{'='*70}")
print(f"Skipped (no timing file or solver_count=0): {skipped_count}")
print(f"Processed (solver_count > 0):               {processed_count}")

print(f"\n{'='*70}")
print(f"RESULTS SUMMARY")
print(f"{'='*70}")
print(f"Correct (expected == actual):          {len(correct):>4} testcases")
print(f"  - True Correct (violation->violation): {len(true_correct):>4} testcases")
print(f"  - True Negative (safe->safe):          {len(correct)-len(true_correct):>4} testcases")
print(f"Incorrect (expected != actual):        {len(incorrect):>4} testcases")
print(f"  - False Results (not unknown/crash):   {len(false_results):>4} testcases")
print(f"  - Unknown/Crash:                       {len(incorrect)-len(false_results):>4} testcases")
print(f"Total:                                 {len(correct) + len(incorrect):>4} testcases")

def compute_stats(group, group_name):
    if not group:
        print(f"\n{group_name}: No data")
        return

    print(f"\n{'='*70}")
    print(f"{group_name} ({len(group)} testcases)")
    print(f"{'='*70}")

    # Compute mean
    mean_stats = {
        'executor': statistics.mean(t['executor'] for t in group),
        'solver': statistics.mean(t['solver'] for t in group),
        'explorer': statistics.mean(t['explorer'] for t in group),
        'witness_gen': statistics.mean(t['witness_gen'] for t in group),
        'witness_val': statistics.mean(t['witness_val'] for t in group),
    }
    mean_total = sum(mean_stats.values())

    print("\nMEAN:")
    for key, val in mean_stats.items():
        pct = (val / mean_total * 100) if mean_total > 0 else 0
        print(f"  {key:<20} {val:>8.2f}s ({pct:>5.1f}%)")
    print(f"  {'Total':<20} {mean_total:>8.2f}s")

    # Compute median
    median_stats = {
        'executor': statistics.median(t['executor'] for t in group),
        'solver': statistics.median(t['solver'] for t in group),
        'explorer': statistics.median(t['explorer'] for t in group),
        'witness_gen': statistics.median(t['witness_gen'] for t in group),
        'witness_val': statistics.median(t['witness_val'] for t in group),
    }
    median_total = sum(median_stats.values())

    print("\nMEDIAN:")
    for key, val in median_stats.items():
        pct = (val / median_total * 100) if median_total > 0 else 0
        print(f"  {key:<20} {val:>8.2f}s ({pct:>5.1f}%)")
    print(f"  {'Total':<20} {median_total:>8.2f}s")

    # Show distribution for solver (all should have solver_count > 0 due to filtering)
    solver_times = [t['solver'] for t in group]
    zero_time = sum(1 for s in solver_times if s == 0.0)
    print(f"\nSolver time distribution (all have solver_count > 0):")
    print(f"  Zero solver TIME:     {zero_time:>4} ({zero_time/len(group)*100:>5.1f}%)")
    print(f"  Non-zero solver TIME: {len(group)-zero_time:>4} ({(len(group)-zero_time)/len(group)*100:>5.1f}%)")

    # Show some examples
    print(f"\nFirst 5 examples:")
    for i, t in enumerate(group[:5], 1):
        print(f"  {i}. {t['name']}: {t['expected']} -> {t['actual']}")
        print(f"     Executor: {t['executor']:.2f}s, Solver: {t['solver']:.2f}s, Explorer: {t['explorer']:.2f}s")

compute_stats(correct, "CORRECT (All expected == actual)")
compute_stats(true_correct, "TRUE POSITIVE (violation -> violation)")
compute_stats(true_negative, "TRUE NEGATIVE (safe -> safe)")
compute_stats(false_results, "FALSE RESULTS (FP + FN, excluding unknown/crash)")
compute_stats(incorrect, "INCORRECT (All expected != actual)")
