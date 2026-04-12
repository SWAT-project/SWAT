#!/usr/bin/env python3
"""
Extract CFG metadata from analysis results into a CSV file.
"""

import json
import csv
from pathlib import Path
import sys

def parse_directory_path(metadata_path):
    """
    Parse the directory path to extract group, testcase, and category.

    Example paths:
    - targets/sv-comp/analysis/jayhorn-recursive/Ackermann01_valid-assert/Main_main_metadata.json
      -> group: jayhorn-recursive, testcase: Ackermann01, category: valid-assert
    - targets/sv-comp/analysis/java-ranger-regression/alarm/Alarm_prop1_valid-assert/Main_main_metadata.json
      -> group: java-ranger-regression, testcase: alarm/Alarm_prop1, category: valid-assert
    """
    parts = metadata_path.parts

    # Find the 'analysis' directory index
    try:
        analysis_idx = parts.index('analysis')
    except ValueError:
        return None, None, None

    # Group is the directory immediately after 'analysis'
    if analysis_idx + 1 >= len(parts):
        return None, None, None
    group = parts[analysis_idx + 1]

    # The test case directory is the parent directory of the metadata file
    # (the directory containing the metadata.json file)
    if len(parts) < 2:
        return None, None, None
    testcase_dir = parts[-2]  # Parent directory of the metadata file

    # Get all intermediate directories between group and testcase_dir (exclusive)
    # These will be prepended to the testcase name
    # Range should be from group+1 to testcase_dir (not including testcase_dir)
    intermediate_dirs = []
    for i in range(analysis_idx + 2, len(parts) - 2):
        intermediate_dirs.append(parts[i])

    # Split testcase_dir by the LAST underscore to separate testcase and category
    # Pattern: {testcase_name}_{category} where category is after the last underscore
    # Example: "Alarm_prop4_valid-assert" -> testcase_name="Alarm_prop4", category="valid-assert"
    last_underscore_idx = testcase_dir.rfind('_')
    if last_underscore_idx != -1:
        testcase_name = testcase_dir[:last_underscore_idx]
        category = testcase_dir[last_underscore_idx + 1:]
    else:
        testcase_name = testcase_dir
        category = ""

    # Build full testcase path including intermediate directories
    if intermediate_dirs:
        testcase = '/'.join(intermediate_dirs) + '/' + testcase_name
    else:
        testcase = testcase_name

    return group, testcase, category


def parse_symbolic_variables(method_calls):
    """
    Parse symbolic variable counts from Verifier.nondet* method calls.

    Example method signature: <org.sosy_lab.sv_benchmarks.Verifier: int nondetInt()>
    """
    sym_counts = {
        'boolean': 0,
        'byte': 0,
        'char': 0,
        'short': 0,
        'int': 0,
        'long': 0,
        'float': 0,
        'double': 0,
        'string': 0,
        'object': 0,
    }

    all_calls = method_calls.get('allCalls', {})

    for method_sig, count in all_calls.items():
        # Check if this is a Verifier.nondet* method
        if 'Verifier:' in method_sig and 'nondet' in method_sig:
            # Parse the method signature
            # Format: <org.sosy_lab.sv_benchmarks.Verifier: {type} nondet{Type}()>

            if 'int nondetInt()' in method_sig:
                sym_counts['int'] += count
            elif 'boolean nondetBoolean()' in method_sig:
                sym_counts['boolean'] += count
            elif 'byte nondetByte()' in method_sig:
                sym_counts['byte'] += count
            elif 'char nondetChar()' in method_sig:
                sym_counts['char'] += count
            elif 'short nondetShort()' in method_sig:
                sym_counts['short'] += count
            elif 'long nondetLong()' in method_sig:
                sym_counts['long'] += count
            elif 'float nondetFloat()' in method_sig:
                sym_counts['float'] += count
            elif 'double nondetDouble()' in method_sig:
                sym_counts['double'] += count
            elif 'java.lang.String nondetString()' in method_sig:
                sym_counts['string'] += count
            else:
                # Other object types
                sym_counts['object'] += count

    return sym_counts


def count_java_library_calls(method_calls):
    """
    Count Java library method calls (excluding AssertionError).
    Returns tuple: (total_count, dict of method_sig -> count)

    Example: <java.lang.Boolean: java.lang.Boolean valueOf(boolean)>
    """
    all_calls = method_calls.get('allCalls', {})
    java_lib_calls = {}
    total_count = 0

    for method_sig, count in all_calls.items():
        # Check if this is a Java library call (starts with <java.)
        # but exclude AssertionError
        if method_sig.startswith('<java.') and 'AssertionError' not in method_sig:
            java_lib_calls[method_sig] = count
            total_count += count

    return total_count, java_lib_calls


def extract_metadata(metadata_path):
    """
    Extract relevant metrics from a metadata JSON file.
    Returns tuple: (row_dict, java_lib_calls_dict)
    """
    try:
        with open(metadata_path, 'r') as f:
            data = json.load(f)

        # Parse directory structure
        group, testcase, category = parse_directory_path(Path(metadata_path))

        # Extract metrics from JSON
        total_nodes = data.get('totalNodes', 0)
        total_edges = data.get('totalEdges', 0)

        conditionals = data.get('conditionals', {})
        total_conditionals = conditionals.get('totalConditionals', 0)

        cyclomatic_complexity = data.get('cyclomaticComplexity', 0)

        # Parse symbolic variables from method calls
        method_calls = data.get('methodCalls', {})
        sym_vars = parse_symbolic_variables(method_calls)

        # Count Java library calls
        java_lib_call_count, java_lib_calls = count_java_library_calls(method_calls)

        row = {
            'group': group,
            'testcase': testcase,
            'category': category,
            'total_nodes': total_nodes,
            'total_edges': total_edges,
            'sym_boolean': sym_vars['boolean'],
            'sym_Byte': sym_vars['byte'],
            'sym_char': sym_vars['char'],
            'sym_short': sym_vars['short'],
            'sym_int': sym_vars['int'],
            'sym_long': sym_vars['long'],
            'sym_float': sym_vars['float'],
            'sym_double': sym_vars['double'],
            'sym_string': sym_vars['string'],
            'sym_object': sym_vars['object'],
            'total_conditionals': total_conditionals,
            'cyclomatic_complexity': cyclomatic_complexity,
            'java_lib_calls': java_lib_call_count,
        }

        return row, java_lib_calls
    except Exception as e:
        print(f"Error processing {metadata_path}: {e}", file=sys.stderr)
        return None, None


def main():
    # Base directory
    analysis_dir = Path('targets/sv-comp/analysis')

    if not analysis_dir.exists():
        print(f"Error: Analysis directory not found: {analysis_dir}", file=sys.stderr)
        sys.exit(1)

    # Find all metadata files
    metadata_files = list(analysis_dir.glob('**/*_metadata.json'))

    print(f"Found {len(metadata_files)} metadata files")

    # Extract data from all files and aggregate Java library calls
    rows = []
    java_lib_aggregate = {}  # method_sig -> total_count across all test cases
    seen_testcases = set()  # Track unique testcases (group + testcase, ignore category)

    for i, metadata_file in enumerate(metadata_files, 1):
        if i % 100 == 0:
            print(f"Processing {i}/{len(metadata_files)}...")

        row, java_lib_calls = extract_metadata(metadata_file)
        if row:
            rows.append(row)

            # For aggregation, only count each unique testcase once (ignore category)
            testcase_key = (row['group'], row['testcase'])
            if testcase_key not in seen_testcases:
                seen_testcases.add(testcase_key)
                # Aggregate Java library calls for this unique testcase
                for method_sig, count in java_lib_calls.items():
                    java_lib_aggregate[method_sig] = java_lib_aggregate.get(method_sig, 0) + count

    # Write to CSV
    output_file = 'targets/sv-comp/analysis/cfg_metadata.csv'

    fieldnames = [
        'group',
        'testcase',
        'category',
        'total_nodes',
        'total_edges',
        'sym_boolean',
        'sym_Byte',
        'sym_char',
        'sym_short',
        'sym_int',
        'sym_long',
        'sym_float',
        'sym_double',
        'sym_string',
        'sym_object',
        'total_conditionals',
        'cyclomatic_complexity',
        'java_lib_calls',
    ]

    with open(output_file, 'w', newline='') as csvfile:
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)

    print(f"\nSuccessfully extracted {len(rows)} test cases to {output_file}")

    # Write Java library call aggregate to a separate CSV file
    aggregate_file = 'targets/sv-comp/analysis/java_lib_calls_aggregate.csv'

    # Sort by count (descending) for better readability
    sorted_aggregate = sorted(java_lib_aggregate.items(), key=lambda x: x[1], reverse=True)

    with open(aggregate_file, 'w', newline='') as csvfile:
        writer = csv.writer(csvfile)
        writer.writerow(['method_signature', 'total_count'])
        writer.writerows(sorted_aggregate)

    print(f"Successfully wrote {len(sorted_aggregate)} Java library calls to {aggregate_file}")


if __name__ == '__main__':
    main()
