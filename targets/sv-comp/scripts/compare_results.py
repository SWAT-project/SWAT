import json
import os

def compare_results(current_file, reference_file):
    # Load current and reference data
    with open(current_file, 'r') as f:
        current_data = json.load(f)

    with open(reference_file, 'r') as f:
        reference_data = json.load(f)

    # Extract results from both files
    current_results = current_data.get("results", {})
    reference_results = reference_data.get("results", {})

    # Compare cases
    changes = []
    for case, current_value in current_results.items():
        current_status = current_value[0]  # Extract the status from the current results

        # Check if the case exists in reference
        if case in reference_results:
            reference_status = reference_results[case][0]  # Extract the status from the reference results
            if current_status != reference_status:
                changes.append({
                    "case": case,
                    "from": reference_status,
                    "to": current_status
                })
        else:
            # New case added in current results
            changes.append({
                "case": case,
                "from": "Not present",
                "to": current_status
            })

    # Identify cases removed in the current results
    for case in reference_results:
        if case not in current_results:
            changes.append({
                "case": case,
                "from": reference_results[case][0],
                "to": "Removed"
            })

    # Print the changes
    for change in changes:
        print(f"Case: {change['case']} changed from {change['from']} to {change['to']}")

# Replace these with paths to your JSON files
SCRIPT_DIR = os.path.dirname(os.path.realpath(__file__))
current_file = os.path.join(SCRIPT_DIR, '..', 'results', "results_20241122_103412.json")
reference_file = os.path.join(SCRIPT_DIR, '..', 'results', "results_20241122_100419.json")

compare_results(current_file, reference_file)
