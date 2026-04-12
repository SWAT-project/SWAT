#!/usr/bin/env python3
"""
Quick verification script to manually check timing aggregation.
"""
import json
from pathlib import Path
import statistics

# Find all timing files
script_dir = Path(__file__).resolve().parent
log_dir = script_dir.parent / 'logs'
timing_files = list(log_dir.rglob('timing_data.json'))

print(f"Found {len(timing_files)} timing files\n")

# Manually aggregate
totals = {
    'symbolic_executor': 0.0,
    'smt_solver': 0.0,
    'symbolic_explorer': 0.0,
    'witness_generation': 0.0,
    'witness_validation': 0.0
}

count = 0
anomalies = []
corrected_totals = {
    'symbolic_executor': 0.0,
    'smt_solver': 0.0,
    'symbolic_explorer': 0.0,
    'witness_generation': 0.0,
    'witness_validation': 0.0
}

# Collect per-testcase values for median computation
per_testcase = {
    'symbolic_executor': [],
    'smt_solver': [],
    'symbolic_explorer': [],
    'witness_generation': [],
    'witness_validation': []
}

# Store examples for each category
zero_solver_examples = []
zero_explorer_examples = []
both_zero_examples = []

for tf in timing_files:  # Process ALL files
    try:
        with open(tf) as f:
            data = json.load(f)
            agg = data.get('aggregates', {})

            # Check for anomalies (where solver/explorer > executor)
            executor = agg.get('symbolic_executor', 0)
            solver = agg.get('smt_solver', 0)
            explorer = agg.get('symbolic_explorer', 0)

            if solver > executor or explorer > executor:
                anomalies.append((tf.parent.name, executor, solver, explorer))

            # Add to totals using OLD values (what's in file)
            for key in totals:
                totals[key] += agg.get(key, 0)

            # Also compute CORRECTED explorer time (as residual)
            total_time = agg.get('total_time', 0)
            witness_gen = agg.get('witness_generation', 0)
            witness_val = agg.get('witness_validation', 0)
            corrected_explorer = max(0.0, total_time - executor - solver - witness_gen - witness_val)

            corrected_totals['symbolic_executor'] += executor
            corrected_totals['smt_solver'] += solver
            corrected_totals['symbolic_explorer'] += corrected_explorer
            corrected_totals['witness_generation'] += witness_gen
            corrected_totals['witness_validation'] += witness_val

            # Store per-testcase values
            per_testcase['symbolic_executor'].append(executor)
            per_testcase['smt_solver'].append(solver)
            per_testcase['symbolic_explorer'].append(corrected_explorer)
            per_testcase['witness_generation'].append(witness_gen)
            per_testcase['witness_validation'].append(witness_val)

            # Collect examples of zero-time cases
            testcase_name = tf.parent.name
            if solver == 0.0 and len(zero_solver_examples) < 10:
                zero_solver_examples.append((testcase_name, executor, solver, corrected_explorer, total_time))
            if corrected_explorer == 0.0 and len(zero_explorer_examples) < 10:
                zero_explorer_examples.append((testcase_name, executor, solver, corrected_explorer, total_time))
            if solver == 0.0 and corrected_explorer == 0.0 and len(both_zero_examples) < 10:
                both_zero_examples.append((testcase_name, executor, solver, corrected_explorer, total_time))

            count += 1
    except Exception as e:
        print(f"ERROR reading {tf}: {e}")

print(f"\nProcessed {count} files")
print(f"Found {len(anomalies)} anomalies where solver/explorer > executor")

# Show examples of zero-time cases
print(f"\n{'='*60}")
print(f"EXAMPLES OF ZERO-TIME CASES:")
print(f"{'='*60}")

print(f"\nTestcases with ZERO solver time (first 10):")
for name, executor, solver, explorer, total in zero_solver_examples:
    print(f"  {name}:")
    print(f"    Total: {total:.2f}s, Executor: {executor:.2f}s, Solver: {solver:.2f}s, Explorer: {explorer:.2f}s")

print(f"\nTestcases with ZERO explorer overhead (first 10):")
for name, executor, solver, explorer, total in zero_explorer_examples:
    print(f"  {name}:")
    print(f"    Total: {total:.2f}s, Executor: {executor:.2f}s, Solver: {solver:.2f}s, Explorer: {explorer:.2f}s")

print(f"\nTestcases with BOTH solver and explorer ZERO (first 10):")
for name, executor, solver, explorer, total in both_zero_examples:
    print(f"  {name}:")
    print(f"    Total: {total:.2f}s, Executor: {executor:.2f}s, Solver: {solver:.2f}s, Explorer: {explorer:.2f}s")

if anomalies:
    print(f"\n{'='*60}")
    print(f"ANOMALIES (solver/explorer > executor):")
    print(f"{'='*60}")
    print(f"\nFirst 20 anomalies:")
    for name, executor, solver, explorer in anomalies[:20]:
        print(f"  {name}: executor={executor:.2f}s, solver={solver:.2f}s, explorer={explorer:.2f}s")

print(f"\n{'='*60}")
print(f"OLD (BUGGY) AGGREGATION (all {count} testcases):")
print(f"{'='*60}")

total_time = sum(totals.values())
for key, val in totals.items():
    avg = val / count if count > 0 else 0
    pct = (avg / (total_time/count) * 100) if total_time > 0 else 0
    print(f"{key:<24} avg={avg:>8.2f}s ({pct:>5.1f}%)")

print(f"\n{'='*60}")
print(f"CORRECTED AGGREGATION - MEAN (explorer as residual):")
print(f"{'='*60}")

corrected_total_time = sum(corrected_totals.values())
for key, val in corrected_totals.items():
    avg = val / count if count > 0 else 0
    pct = (avg / (corrected_total_time/count) * 100) if corrected_total_time > 0 else 0
    print(f"{key:<24} avg={avg:>8.2f}s ({pct:>5.1f}%)")

print(f"\n{'='*60}")
print(f"CORRECTED AGGREGATION - MEDIAN (explorer as residual):")
print(f"{'='*60}")

# Compute median for each stage
medians = {key: statistics.median(vals) for key, vals in per_testcase.items()}
median_total = sum(medians.values())

for key, median_val in medians.items():
    pct = (median_val / median_total * 100) if median_total > 0 else 0
    print(f"{key:<24} median={median_val:>8.2f}s ({pct:>5.1f}%)")

# Show distribution to validate the 0.00 medians
print(f"\n{'='*60}")
print(f"DISTRIBUTION ANALYSIS (to validate median values):")
print(f"{'='*60}")

for key in ['symbolic_executor', 'smt_solver', 'symbolic_explorer']:
    vals = per_testcase[key]
    zero_count = sum(1 for v in vals if v == 0.0)
    nonzero_count = len(vals) - zero_count
    min_val = min(vals) if vals else 0
    max_val = max(vals) if vals else 0

    print(f"\n{key}:")
    print(f"  Zero values:     {zero_count:>4} ({zero_count/len(vals)*100:>5.1f}%)")
    print(f"  Non-zero values: {nonzero_count:>4} ({nonzero_count/len(vals)*100:>5.1f}%)")
    print(f"  Min: {min_val:.4f}s, Max: {max_val:.2f}s")
    if nonzero_count > 0:
        nonzero_vals = [v for v in vals if v > 0]
        median_nonzero = statistics.median(nonzero_vals)
        print(f"  Median (non-zero only): {median_nonzero:.4f}s")

print(f"\n{'='*60}")
print("Now comparing with TimingAnalysis results...")
print(f"{'='*60}")

try:
    from lib.analysis.timing import TimingAnalysis
    stats = TimingAnalysis.aggregate_timing_from_files()

    if stats:
        avg_timing = stats['avg_timing']
        total_avg = sum(avg_timing.values())
        for stage, time_val in avg_timing.items():
            percent = (time_val / total_avg * 100) if total_avg > 0 else 0
            print(f"{stage:<24} avg={time_val:>8.2f}s ({percent:>5.1f}%)")
except Exception as e:
    print(f"Could not run TimingAnalysis: {e}")
