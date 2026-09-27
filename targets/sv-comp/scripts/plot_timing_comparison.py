#!/usr/bin/env python3
"""
Compare the timing behavior of two or more SV-COMP runs.

For each result file, plots the cumulative number of test cases whose
`total_time` is <= t, for a simulated timeout t on the x-axis. Test cases
without a `total_time` (e.g. real timeouts) never count as finished.

Usage:
    plot_timing_comparison.py RESULTS.json [RESULTS.json ...] [-o out.png]
                              [--label NAME ...] [--linear]
"""

import argparse
import json
import re
from pathlib import Path

import matplotlib.pyplot as plt

TIMING_INDEX = 6

# Fixed categorical order; runs keep their color by position on the command line.
COLORS = ["#2a78d6", "#eb6834", "#1baf7a", "#eda100",
          "#e87ba4", "#008300", "#4a3aa7", "#e34948"]


def load_total_times(path):
    """Return (sorted finished total_times, total number of test cases)."""
    with open(path) as f:
        results = json.load(f)["results"]
    times = sorted(
        entry[TIMING_INDEX]["total_time"]
        for entry in results.values()
        if isinstance(entry[TIMING_INDEX], dict) and "total_time" in entry[TIMING_INDEX]
    )
    return times, len(results)


def default_label(path):
    """Use the run directory name (run_<timestamp>) if it can be found."""
    for part in reversed(Path(path).resolve().parts):
        if re.fullmatch(r"run_\d{8}_\d{6}", part):
            return part
    return Path(path).stem


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("results", nargs="+", help="results_<prop>_<timestamp>.json files")
    parser.add_argument("-o", "--output", default="timing_comparison.png",
                        help="output image (default: timing_comparison.png)")
    parser.add_argument("--label", action="append",
                        help="label per result file, in order (default: run directory name)")
    parser.add_argument("--linear", action="store_true", help="use a linear x-axis instead of log")
    parser.add_argument("--show", action="store_true", help="open an interactive window")
    args = parser.parse_args()

    if len(args.results) > len(COLORS):
        parser.error(f"at most {len(COLORS)} result files are supported")
    if args.label and len(args.label) != len(args.results):
        parser.error("--label must be given once per result file")
    labels = args.label or [default_label(p) for p in args.results]

    fig, ax = plt.subplots(figsize=(10, 6))
    total_cases = set()
    min_time, max_time = float("inf"), 0.0
    for path, label, color in zip(args.results, labels, COLORS):
        times, total = load_total_times(path)
        total_cases.add(total)
        if times:
            min_time, max_time = min(min_time, times[0]), max(max_time, times[-1])
        counts = range(1, len(times) + 1)
        ax.step([0.0] + times, [0] + list(counts), where="post",
                color=color, linewidth=2, label=f"{label} ({len(times)}/{total} finished)")

    if not args.linear:
        ax.set_xscale("log")
        ax.set_xlim(left=min_time / 1.5 if max_time else 0.1)
    else:
        ax.set_xlim(left=0)
    ax.set_xlim(right=max_time * 1.1)
    ax.set_ylim(0, max(total_cases) * 1.05)
    if len(total_cases) == 1:
        ax.axhline(total_cases.pop(), color="#999999", linewidth=1, linestyle="--",
                   label="total test cases")
    else:
        print(f"Warning: result files contain different numbers of test cases: {sorted(total_cases)}")

    ax.set_xlabel("Simulated timeout [s]" + ("" if args.linear else " (log scale)"))
    ax.set_ylabel("Test cases finished (cumulative)")
    ax.set_title("SWAT runtime comparison (total_time)")
    ax.grid(True, which="major", color="#e0e0e0", linewidth=0.8)
    ax.grid(True, which="minor", axis="x", color="#f0f0f0", linewidth=0.5)
    ax.set_axisbelow(True)
    for side in ("top", "right"):
        ax.spines[side].set_visible(False)
    ax.legend(loc="lower right", frameon=False)
    fig.tight_layout()

    fig.savefig(args.output, dpi=150)
    print(f"Saved plot to {args.output}")
    if args.show:
        plt.show()


if __name__ == "__main__":
    main()
