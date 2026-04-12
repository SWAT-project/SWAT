from contextlib import contextmanager
import logging, os, enum, subprocess
import socket
import time

import concurrent.futures, json, datetime
from typing import List, Optional
from pprint import pformat
from dtypes import Verdict, ExpectedVerdict, VerificationCategory, VerificationTask, Command
from util import ci_print
from command_generation import extract_testcases, generate_commands
from witness_validation import generate_and_validate_witness
from collections import Counter
from pathlib import Path

try:
    import matplotlib.pyplot as plt
    import numpy as np
    MATPLOTLIB_AVAILABLE = True
except ImportError:
    MATPLOTLIB_AVAILABLE = False


logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)
SCRIPT_DIR = Path(__file__).resolve().parent

class ExecutionStatus(enum.Enum):
    SUCCESS = "success"
    ERROR = "error"
    TIMEOUT = "timeout"



class Mode(enum.Enum):
    SINGLE_TARGET = "single"
    PARALLEL = "parallel"


def check_port_availability(ver_tasks: list[VerificationTask]) -> tuple[bool, list[int]]:
    """
    Check if all ports required by verification tasks are available.

    Args:
        ver_tasks: List of verification tasks with command configuration

    Returns:
        Tuple of (all_available: bool, occupied_ports: list[int])
    """
    occupied_ports = []
    required_ports = set()

    # Extract all required ports from commands
    for ver_task in ver_tasks:
        command = ver_task.get('command')
        if command:
            cmd_list = command.get('command', [])
            # Find --port argument
            try:
                port_idx = cmd_list.index('--port')
                if port_idx + 1 < len(cmd_list):
                    port = int(cmd_list[port_idx + 1])
                    required_ports.add(port)
            except (ValueError, IndexError):
                continue

    if not required_ports:
        logger.info("No ports found in commands")
        return True, []

    logger.info(f"Checking availability of {len(required_ports)} ports (range: {min(required_ports)}-{max(required_ports)})")

    # Check each port
    for port in sorted(required_ports):
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        try:
            # Try to bind to the port
            sock.bind(('127.0.0.1', port))
            sock.close()
        except OSError:
            # Port is already in use
            occupied_ports.append(port)

    if occupied_ports:
        logger.error(f"The following {len(occupied_ports)} ports are already in use: {occupied_ports}")
        logger.error("Please kill the processes using these ports before running tests.")
        logger.error(f"You can find processes with: lsof -i :{occupied_ports[0]} (for example)")
        return False, occupied_ports
    else:
        logger.info(f"All {len(required_ports)} ports are available")
        return True, []


@contextmanager
def pushd(dirname):
    """Context manager to temporarily change the working directory."""
    original_dir = os.getcwd()
    logger.info(f"Changing directory to: {dirname}")
    os.chdir(dirname)
    try:
        yield
    finally:
        os.chdir(original_dir)

def reset_log_dir(log_dir) -> None:
    logger.info("Resetting log directory...")
    if os.path.exists(log_dir):
        os.system(f"rm -rf {log_dir}")
    os.makedirs(log_dir)


def parse_verdict_from_output(output: List[str], category: VerificationCategory) -> Optional[Verdict]:
    """Parse the verdict for a specific category from the output."""

    for line in output:
        if f"[VERDICT {category.value}]" in line:
            if "== OK" in line:
                return Verdict.SAFE
            elif "== ERROR" in line:
                return Verdict.VIOLATION
            elif "== DONT-KNOW" in line:
                return Verdict.UNKNOWN
            elif "== NON-SYMBOLIC" in line:
                return Verdict.NO_SYMBOLIC_VARS

    return None  # No verdict found for this category


def determine_result(output: List[str], category: VerificationCategory, expected_verdict: ExpectedVerdict) -> tuple[int, str]:
    """Determine points and case based on the actual vs expected verdict for a single category."""
    actual_verdict = parse_verdict_from_output(output, category)

    # If no verdict found in output, treat as crash
    if actual_verdict is None:
        if expected_verdict == ExpectedVerdict.SAFE:
            return 0, 'safe -> crash'
        else:
            return 0, 'violation -> crash'

    # Compare actual vs expected
    if expected_verdict == ExpectedVerdict.SAFE:
        if actual_verdict == Verdict.SAFE:
            return 2, 'safe -> safe'
        elif actual_verdict == Verdict.VIOLATION:
            return -16, 'safe -> violation'
        elif actual_verdict == Verdict.UNKNOWN:
            return 0, 'safe -> unknown'
        elif actual_verdict == Verdict.NO_SYMBOLIC_VARS:
            return 0, 'safe -> non-symbolic'
    else:  # expected_verdict == ExpectedVerdict.VIOLATION
        if actual_verdict == Verdict.SAFE:
            return -32, 'violation -> safe'
        elif actual_verdict == Verdict.VIOLATION:
            return 1, 'violation -> violation'
        elif actual_verdict == Verdict.UNKNOWN:
            return 0, 'violation -> unknown'
        elif actual_verdict == Verdict.NO_SYMBOLIC_VARS:
            return 0, 'violation -> non-symbolic'

    # Shouldn't reach here, but handle gracefully
    return 0, 'unknown'

def target_execution(ver_task: VerificationTask) -> tuple[Path, str, int, ExecutionStatus, bool, Optional[bool], float]:
    """
    Execute a verification task and return results including execution time.

    Returns:
        Tuple of (target, case, points, execution_status, error, validated, elapsed_time_seconds)
    """
    command = ver_task['command']
    if command is None:
        raise ValueError(f"No command found for verification task: {ver_task['file_path']}")

    target = command["target"]
    ci_print(f'::group:: Executing target: {target} ({ver_task["category"].value}) ------')
    log_dir = command["log_dir"]
    reset_log_dir(log_dir)
    cmd = command["command"]
    validated = None

    start_time = time.perf_counter()

    with pushd(log_dir):
        execution_status, output = run_command_with_timeout(cmd)
        log_output(output)
        error: bool = check_for_dse_error(output)
        points, case = determine_result(output, ver_task['category'], ver_task['verdict'])

        # Store result in the verification task
        ver_task['result'] = {
            'points': points,
            'case': case
        }

        # ToDo: add witness validation
        if 'violation' in case:
            validated = generate_and_validate_witness(ver_task, output)

    elapsed_time = time.perf_counter() - start_time

    logger.info(f"{ver_task['category'].value} | Points: {points}, Case: {case}, Execution Status: {execution_status}, Error: {error}, Validated: {validated}, Time: {elapsed_time:.2f}s")

    ci_print(f"::endgroup::")
    return target, case, points, execution_status, error, validated, elapsed_time

def check_for_dse_error(output: List[str]) -> bool:
    for line in output:
        if "SWAT Assertion failed" in line:
            return True
    return False



def log_output(output: List[str]):
    for line in output:
        logger.info(line.strip())

def run_command_with_timeout(cmd: list[str], timeout: int = 90) -> tuple[ExecutionStatus, list[str]]:
    """Executes the given command and returns output from both STDOUT and STDERR."""

    logger.info(f'[TARGET EXECUTION]: Running symbolic-explorer: {cmd}')
    output = []
    with subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            universal_newlines=True,
            bufsize=1
    ) as proc:
        try:
            # Read output and wait for process to finish, with a timeout
            stdout, _ = proc.communicate(timeout=timeout)
            output = stdout.splitlines()
        
            return ExecutionStatus.SUCCESS, output
               
            
        except subprocess.TimeoutExpired:
            proc.kill()
            stdout, _ = proc.communicate()
            output = stdout.splitlines()
            return ExecutionStatus.TIMEOUT, output
            
        except Exception as e:
            logger.critical(f'[SVCOMP] Exception: {e}')
            proc.kill()
            output = [str(e)]
            return ExecutionStatus.ERROR, output + [str(e)]

 

def run_parallel(ver_tasks: list[VerificationTask], max_workers: int=50):
    max_workers = min(max_workers, len(ver_tasks))

    logger.info(f"Running parallel target execution with {max_workers} workers...")

    results = {}
    for category in VerificationCategory:
        results[category.value] = {}

    total_start_time = time.perf_counter()

    with concurrent.futures.ThreadPoolExecutor(max_workers=max_workers) as executor:
        # Create a mapping from futures to their corresponding tasks
        future_to_task = {}
        for ver_task in ver_tasks:
            future = executor.submit(target_execution, ver_task)
            future_to_task[future] = ver_task

        for future in concurrent.futures.as_completed(future_to_task): # type: ignore
            try:
                # Get the task associated with this future
                ver_task = future_to_task[future]
                target, case, points, execution_status, error, validated, elapsed_time = future.result()
                logger.info(f"{ver_task['category'].value} | Points: {points}, Case: {case}, Execution Status: {execution_status}, Error: {error}, Validated: {validated}, Time: {elapsed_time:.2f}s")

                results[ver_task['category'].value][target] = (case, points, execution_status.value, error, validated, elapsed_time)
            except Exception as e:
                logger.error(f'Exception occurred: {e}')

    total_elapsed_time = time.perf_counter() - total_start_time
    logger.info(f"Total parallel execution time: {total_elapsed_time:.2f}s")

    evaluate_results(results)

def evaluate_results(results):
    """Evaluate results with per-category statistics including timing."""
    total_points = 0
    category_stats = {}
    all_times = []  # Collect all execution times for histogram

    # Initialize per-category stats
    for category_name in results.keys():
        category_stats[category_name] = {
            'points': 0,
            'case_occurrences': {},
            'execution_statuses': {},
            'dse_errors': 0,
            'witness_stats': {},
            'timing': {
                'times': [],
                'min': None,
                'max': None,
                'mean': None,
                'median': None,
                'total': 0
            }
        }

    # Process each category's results
    for category_name, category_results in results.items():
        category_points = 0
        category_times = []

        for target, result_tuple in category_results.items():
            # Handle both old format (5 elements) and new format (6 elements with timing)
            if len(result_tuple) == 6:
                case, points, execution_status, error, validated, elapsed_time = result_tuple
            else:
                case, points, execution_status, error, validated = result_tuple
                elapsed_time = 0.0

            # Update category stats
            category_points += points
            category_times.append(elapsed_time)
            all_times.append((category_name, target, elapsed_time))

            stats = category_stats[category_name]

            if case in stats['case_occurrences']:
                stats['case_occurrences'][case] += 1
            else:
                stats['case_occurrences'][case] = 1

            if execution_status in stats['execution_statuses']:
                stats['execution_statuses'][execution_status] += 1
            else:
                stats['execution_statuses'][execution_status] = 1

            if error:
                stats['dse_errors'] += 1

            if validated in stats['witness_stats']:
                stats['witness_stats'][validated] += 1
            else:
                stats['witness_stats'][validated] = 1

            logger.info(f"[{category_name}] Target: {target}, Execution Status: {execution_status}, Case: {case}, Points: {points}, Error: {error}, Validated: {validated}, Time: {elapsed_time:.2f}s")

        # Calculate timing statistics for this category
        if category_times:
            stats = category_stats[category_name]
            stats['timing']['times'] = category_times
            stats['timing']['min'] = min(category_times)
            stats['timing']['max'] = max(category_times)
            stats['timing']['mean'] = sum(category_times) / len(category_times)
            stats['timing']['total'] = sum(category_times)
            sorted_times = sorted(category_times)
            mid = len(sorted_times) // 2
            stats['timing']['median'] = sorted_times[mid] if len(sorted_times) % 2 else (sorted_times[mid-1] + sorted_times[mid]) / 2

        category_stats[category_name]['points'] = category_points
        total_points += category_points

        logger.info(f"\n[{category_name}] Category Points: {category_points}")
        logger.info(f"[{category_name}] Case occurrences: {category_stats[category_name]['case_occurrences']}")
        logger.info(f"[{category_name}] Execution statuses: {category_stats[category_name]['execution_statuses']}")
        logger.info(f"[{category_name}] DSE errors: {category_stats[category_name]['dse_errors']}")
        logger.info(f"[{category_name}] Witness stats: {category_stats[category_name]['witness_stats']}")

        # Log timing stats
        timing = category_stats[category_name]['timing']
        if timing['min'] is not None:
            logger.info(f"[{category_name}] Timing: min={timing['min']:.2f}s, max={timing['max']:.2f}s, mean={timing['mean']:.2f}s, median={timing['median']:.2f}s, total={timing['total']:.2f}s")

    logger.info(f"\n{'='*50}")
    logger.info(f"TOTAL POINTS (ALL CATEGORIES): {total_points}")
    logger.info(f"{'='*50}")

    # Print overall timing summary
    if all_times:
        all_elapsed = [t[2] for t in all_times]
        logger.info(f"Overall timing: min={min(all_elapsed):.2f}s, max={max(all_elapsed):.2f}s, mean={sum(all_elapsed)/len(all_elapsed):.2f}s")

    save_results(category_stats, results)

    # Generate timing histogram
    generate_timing_histogram(category_stats, results)

    summary_path = os.getenv("GITHUB_STEP_SUMMARY")
    if summary_path is not None:
        print_github_ci(total_points, category_stats, results)

    return total_points


def generate_timing_histogram(category_stats, results, folder='results'):
    """Generate histogram(s) of execution times using matplotlib."""
    if not MATPLOTLIB_AVAILABLE:
        logger.warning("matplotlib not available, skipping histogram generation. Install with: pip install matplotlib numpy")
        return

    folder = os.path.join(SCRIPT_DIR, '..', folder)
    if not os.path.exists(folder):
        os.makedirs(folder)

    timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")

    # Collect all times across categories
    all_times = []
    category_times_dict = {}

    for category_name, stats in category_stats.items():
        times = stats['timing'].get('times', [])
        if times:
            category_times_dict[category_name] = times
            all_times.extend(times)

    if not all_times:
        logger.info("No timing data available for histogram generation")
        return

    # Create figure with subplots
    num_categories = len(category_times_dict)
    fig_height = 4 + (num_categories * 3)  # Dynamic height based on categories
    fig, axes = plt.subplots(num_categories + 1, 1, figsize=(12, fig_height))

    if num_categories == 0:
        axes = [axes]
    elif num_categories == 1:
        axes = [axes[0], axes[1]]

    # Overall histogram
    ax_overall = axes[0]
    all_times_array = np.array(all_times)

    # Use log scale bins if there's wide variation
    if max(all_times) > 10 * min(all_times) and min(all_times) > 0:
        bins = np.logspace(np.log10(max(0.1, min(all_times))), np.log10(max(all_times) + 0.1), 30)
        ax_overall.set_xscale('log')
    else:
        bins = 30

    ax_overall.hist(all_times_array, bins=bins, edgecolor='black', alpha=0.7, color='steelblue')
    ax_overall.axvline(np.mean(all_times_array), color='red', linestyle='--', linewidth=2, label=f'Mean: {np.mean(all_times_array):.2f}s')
    ax_overall.axvline(np.median(all_times_array), color='green', linestyle='--', linewidth=2, label=f'Median: {np.median(all_times_array):.2f}s')
    ax_overall.set_xlabel('Execution Time (seconds)')
    ax_overall.set_ylabel('Frequency')
    ax_overall.set_title(f'Overall Execution Time Distribution (n={len(all_times)})')
    ax_overall.legend()
    ax_overall.grid(True, alpha=0.3)

    # Add text box with statistics
    stats_text = f'Min: {min(all_times):.2f}s\nMax: {max(all_times):.2f}s\nMean: {np.mean(all_times_array):.2f}s\nMedian: {np.median(all_times_array):.2f}s\nTotal: {sum(all_times):.2f}s'
    ax_overall.text(0.98, 0.95, stats_text, transform=ax_overall.transAxes, fontsize=9,
                    verticalalignment='top', horizontalalignment='right',
                    bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.5))

    # Per-category histograms
    colors = plt.cm.Set2(np.linspace(0, 1, num_categories))  # type: ignore
    for idx, (category_name, times) in enumerate(category_times_dict.items()):
        ax = axes[idx + 1]
        times_array = np.array(times)

        # Use same binning logic
        if max(times) > 10 * min(times) and min(times) > 0:
            cat_bins = np.logspace(np.log10(max(0.1, min(times))), np.log10(max(times) + 0.1), 25)
            ax.set_xscale('log')
        else:
            cat_bins = 25

        ax.hist(times_array, bins=cat_bins, edgecolor='black', alpha=0.7, color=colors[idx])
        ax.axvline(np.mean(times_array), color='red', linestyle='--', linewidth=2, label=f'Mean: {np.mean(times_array):.2f}s')
        ax.axvline(np.median(times_array), color='green', linestyle='--', linewidth=2, label=f'Median: {np.median(times_array):.2f}s')
        ax.set_xlabel('Execution Time (seconds)')
        ax.set_ylabel('Frequency')
        ax.set_title(f'{category_name} Execution Time Distribution (n={len(times)})')
        ax.legend()
        ax.grid(True, alpha=0.3)

        # Add stats box
        cat_stats_text = f'Min: {min(times):.2f}s\nMax: {max(times):.2f}s\nMean: {np.mean(times_array):.2f}s\nMedian: {np.median(times_array):.2f}s'
        ax.text(0.98, 0.95, cat_stats_text, transform=ax.transAxes, fontsize=9,
                verticalalignment='top', horizontalalignment='right',
                bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.5))

    plt.tight_layout()

    # Save histogram
    histogram_path = os.path.join(folder, f'timing_histogram_{timestamp}.png')
    plt.savefig(histogram_path, dpi=150, bbox_inches='tight')
    plt.close()
    logger.info(f"Timing histogram saved to: {histogram_path}")

    # Also generate a box plot for comparison
    if num_categories > 1:
        fig2, ax2 = plt.subplots(figsize=(10, 6))
        box_data = [times for times in category_times_dict.values()]
        box_labels = list(category_times_dict.keys())

        bp = ax2.boxplot(box_data, labels=box_labels, patch_artist=True)
        for patch, color in zip(bp['boxes'], colors):
            patch.set_facecolor(color)

        ax2.set_ylabel('Execution Time (seconds)')
        ax2.set_title('Execution Time Comparison by Category')
        ax2.grid(True, alpha=0.3, axis='y')

        # Rotate labels if they're long
        plt.xticks(rotation=45, ha='right')
        plt.tight_layout()

        boxplot_path = os.path.join(folder, f'timing_boxplot_{timestamp}.png')
        plt.savefig(boxplot_path, dpi=150, bbox_inches='tight')
        plt.close()
        logger.info(f"Timing boxplot saved to: {boxplot_path}")


def save_results(category_stats, results, folder='results'):
    """Save results with one JSON file per category, including timing data."""
    folder = os.path.join(SCRIPT_DIR, '..', folder)

    if not os.path.exists(folder):
        os.makedirs(folder)

    timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")

    # Save one file per category
    for category_name, stats in category_stats.items():
        # Convert Path keys to strings for JSON serialization
        category_results = results[category_name]
        results_serializable = {str(k): v for k, v in category_results.items()}

        # Prepare timing stats (exclude raw times array for cleaner JSON)
        timing_stats = {
            'min': stats['timing'].get('min'),
            'max': stats['timing'].get('max'),
            'mean': stats['timing'].get('mean'),
            'median': stats['timing'].get('median'),
            'total': stats['timing'].get('total'),
            'count': len(stats['timing'].get('times', []))
        }

        data = {
            'category': category_name,
            'points': stats['points'],
            'case_occurrences': stats['case_occurrences'],
            'execution_statuses': stats['execution_statuses'],
            'dse_errors': stats['dse_errors'],
            'witness_stats': stats['witness_stats'],
            'timing': timing_stats,
            'results': results_serializable
        }

        filename = f"results_{category_name}_{timestamp}.json"
        filepath = os.path.join(folder, filename)

        with open(filepath, 'w') as f:
            json.dump(data, f, indent=4)

        logger.info(f"[{category_name}] Results saved to {filepath}")


def print_github_ci(total_points, category_stats, results):
    """Print GitHub CI summary with per-category breakdown."""
    def bool_to_emoji(value):
        if value is None:
            return "❌"
        return "✅" if value else "❌"

    summary_path = os.getenv("GITHUB_STEP_SUMMARY")
    if summary_path is None:
        raise EnvironmentError("GITHUB_STEP_SUMMARY is not set. Are you running inside GitHub Actions?")

    with open(summary_path, "a") as f:
        # Header
        f.write("## 🧾 Symbolic Execution Summary\n\n")
        f.write(f"**Total Points (All Categories):** `{total_points}`\n\n")

        # Per-Category Summary
        f.write("### 📊 Points by Category\n")
        f.write("| Category | Points |\n")
        f.write("|----------|--------|\n")
        for category_name, stats in category_stats.items():
            f.write(f"| {category_name} | {stats['points']} |\n")
        f.write("\n")

        # Detailed breakdown per category
        for category_name, stats in category_stats.items():
            f.write(f"## {category_name}\n\n")

            # Case Occurrences
            if stats['case_occurrences']:
                f.write("### 📊 Case Occurrences\n")
                f.write("| Case | Count |\n")
                f.write("|------|-------|\n")
                for k, v in stats['case_occurrences'].items():
                    f.write(f"| {k} | {v} |\n")
                f.write("\n")

            # Execution Statuses
            if stats['execution_statuses']:
                f.write("### ⚙️ Execution Statuses\n")
                f.write("| Status | Count |\n")
                f.write("|--------|-------|\n")
                for k, v in stats['execution_statuses'].items():
                    f.write(f"| {k} | {v} |\n")
                f.write("\n")

            # DSE Errors
            f.write(f"**DSE Errors:** `{stats['dse_errors']}`\n\n")

            # Timing Stats
            timing = stats.get('timing', {})
            if timing.get('min') is not None:
                f.write("### ⏱️ Timing Statistics\n")
                f.write("| Metric | Value |\n")
                f.write("|--------|-------|\n")
                f.write(f"| Min | {timing['min']:.2f}s |\n")
                f.write(f"| Max | {timing['max']:.2f}s |\n")
                f.write(f"| Mean | {timing['mean']:.2f}s |\n")
                f.write(f"| Median | {timing['median']:.2f}s |\n")
                f.write(f"| Total | {timing['total']:.2f}s |\n")
                f.write("\n")

            # Witness Stats
            if stats['witness_stats'] and any(stats['witness_stats'].values()):
                f.write("### 🧷 Witness Stats\n")
                f.write("| Validated | Count |\n")
                f.write("|-----------|-------|\n")
                for validated, count in stats['witness_stats'].items():
                    f.write(f"| {bool_to_emoji(validated)} | {count} |\n")
                f.write("\n")


def select_target(ver_tasks: list[VerificationTask], target_name: str) -> Optional[VerificationTask]:
    """Select a verification task by target name."""
    for ver_task in ver_tasks:
        command = ver_task.get('command')
        if command and str(command['target']) == target_name:
            return ver_task
    return None


def run_single_target(ver_tasks: list[VerificationTask]):
    target = "autostub/Boolean_public_static_int_java_lang_Boolean_compare_boolean_boolean"

    ver_task = select_target(ver_tasks, target)
    if ver_task is None:
        logger.error(f"Target {target} not found.")
        return
    logger.info(f"Running single target: {target}")
    ver_task["command"]["log_dir"] = Path(str(ver_task["command"]["log_dir"]).replace('/logs/', '/logs-debug/')) # type: ignore
    target_execution(ver_task)



if __name__ == "__main__":
    mode = Mode.PARALLEL  # Mode.SINGLE_TARGET

    # Configure which properties/categories to run (None = all)
    selected_categories = [
        VerificationCategory.VALID_ASSERT_PRP,
        #VerificationCategory.NO_RUNTIME_EXCEPTION_PRP
    ]

    logger.info(f"Running target execution script in {mode.value} mode...")
    test_dir = SCRIPT_DIR.parent / "sv-benchmarks"
    logger.info(f"Base directory: {test_dir}")
    ver_tasks: list[VerificationTask] = extract_testcases(test_dir)
    logger.info(f"Extracted {len(ver_tasks)} test cases.")

    # Filter by selected categories if specified
    if selected_categories is not None: # type: ignore
        ver_tasks = [task for task in ver_tasks if task['category'] in selected_categories]
        logger.info(f"Filtered to {len(ver_tasks)} test cases for categories: {[c.value for c in selected_categories]}")

    if mode == Mode.SINGLE_TARGET: # type: ignore
        config_file = "swat-debug.cfg"
    elif mode == Mode.PARALLEL:
        config_file = "sv-comp.cfg"
    else:
        raise ValueError(f"Invalid mode: {mode}")
    ver_tasks_with_commands = generate_commands(ver_tasks, config_file)#[:10]
    logger.info(f"Generated {len(ver_tasks_with_commands)} commands.")

    # Check port availability before starting tests (sanity check)
    # Note: Command generation already skips occupied ports, but this serves as a final validation
    ports_available, occupied_ports = check_port_availability(ver_tasks_with_commands)
    if not ports_available:
        logger.warning(f"WARNING: {len(occupied_ports)} ports became occupied between command generation and execution.")
        logger.warning(f"Occupied ports: {occupied_ports}")
        logger.warning("This may cause some tests to fail. Consider restarting the script.")

    if mode == Mode.SINGLE_TARGET: # type: ignore
        run_single_target(ver_tasks_with_commands)
    elif mode == Mode.PARALLEL:
        run_parallel(ver_tasks_with_commands)
    else:
        raise ValueError(f"Invalid mode: {mode}")

    logger.info("Done.")

