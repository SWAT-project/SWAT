#!/usr/bin/env python3
"""
Compare the timing behavior of two or more SV-COMP runs in a set of plots.

Takes results_<prp>_<timestamp>.json files, or run directories (runs/run_<timestamp>/),
in which case the results file for --prp is picked from their results/ folder. The
first run is the baseline; pairwise plots compare every other run against it. Per-task stats.json files
(iteration and solver-call counts) are read from the run's logs/ directory
next to results/ when they exist.

Writes numbered SVGs, summary.md and a self-contained report.html into the
output directory, by default runs/comparison_<A>_vs_<B>[_vs_...]/ next to the runs,
named after the labels or, without labels, the run directories.

Usage:
    compare_runs.py BASELINE.json OTHER.json [...] [-o outdir] [--label NAME ...]
    compare_runs.py runs/run_A/ runs/run_B/ [...] [--prp valid-assert] [--label NAME ...]
"""

import argparse
import base64
import json
import math
import re
import statistics
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402
import numpy as np  # noqa: E402
from matplotlib.colors import SymLogNorm, LinearSegmentedColormap  # noqa: E402
from matplotlib.patches import Patch  # noqa: E402
from matplotlib.lines import Line2D  # noqa: E402

TIMING_INDEX = 6
RUNS_DIR = Path(__file__).resolve().parent.parent / "runs"

# Categorical order (runs); a run keeps its color by position on the command line.
RUN_COLORS = ["#2a78d6", "#eb6834", "#1baf7a", "#eda100",
              "#e87ba4", "#008300", "#4a3aa7", "#e34948"]
# Stages use the same categorical order; runs and stages never share one plot's color key.
STAGE_ORDER = ["static_pre_analysis", "symbolic_executor", "smt_solver", "symbolic_explorer",
               "witness_generation", "witness_validation"]
STAGE_COLORS = {"static_pre_analysis": "#2a78d6", "symbolic_executor": "#eb6834",
                "smt_solver": "#1baf7a", "symbolic_explorer": "#eda100",
                "witness_generation": "#e87ba4", "witness_validation": "#4a3aa7",
                "untracked": "#b5b4ad"}
STAGE_NAMES = {"static_pre_analysis": "static pre-analysis", "symbolic_executor": "symbolic executor",
               "smt_solver": "SMT solver", "symbolic_explorer": "symbolic explorer",
               "witness_generation": "witness generation", "witness_validation": "witness validation",
               "untracked": "outside explorer timer"}
# Verdict change of a task relative to the baseline (status colors, always paired with a marker shape).
CHANGE_STYLE = {"same": ("#8a8a85", "o", "same points"),
                "better": ("#0ca30c", "^", "more points"),
                "worse": ("#d03b3b", "v", "fewer points")}
TEXT = "#2b2b29"
MUTED = "#6f6e69"
GRID = "#e6e5e0"
EPS = 1e-4  # floor for log axes (seconds)


@dataclass
class Task:
    name: str
    suite: str
    case: str
    points: int
    status: str
    wall: float
    stages: Optional[dict]  # None when the explorer wrote no timing (e.g. harness timeout)
    iterations: Optional[int] = None
    solver_calls: Optional[int] = None

    @property
    def total(self) -> Optional[float]:
        return self.stages["total_time"] if self.stages else None

    @property
    def finished(self) -> bool:
        return self.stages is not None

    @property
    def untracked(self) -> Optional[float]:
        return max(0.0, self.wall - self.total) if self.finished else None

    def stage(self, s) -> Optional[float]:
        if not self.finished:
            return None
        if s == "untracked":
            return self.untracked
        return self.stages.get(s, 0.0)


@dataclass
class Run:
    label: str
    path: Path
    timeout: Optional[float]
    tasks: dict = field(default_factory=dict)
    color: str = ""

    def finished_totals(self):
        return sorted(t.total for t in self.tasks.values() if t.finished)


# ----------------------------------------------------------------------------- loading

def parse_timeout(run_dir: Path, tasks) -> Optional[float]:
    gitlog = run_dir / "gitlog.txt"
    if gitlog.exists():
        m = re.search(r"--testcase-timeout-s[ =](\d+)", gitlog.read_text())
        if m:
            return float(m.group(1))
    walls = [t.wall for t in tasks.values() if t.status == "timeout"]
    return float(round(min(walls))) if walls else None


def default_label(path: Path) -> str:
    run_dir = path.resolve().parent.parent
    name = run_dir.name if re.fullmatch(r"run_\d{8}_\d{6}", run_dir.name) else path.stem
    # Empty marker files next to logs/ and results/ (e.g. "high-timeout_SA") describe the run.
    markers = sorted(p.name for p in run_dir.iterdir() if p.is_file() and p.stat().st_size == 0) \
        if run_dir.is_dir() else []
    return f"{name} ({', '.join(markers)})" if markers else name


def load_run(path: Path, label: str) -> Run:
    with open(path) as f:
        results = json.load(f)["results"]
    m = re.match(r"results_(.+?)\.prp_", path.name)
    prp = m.group(1) if m else None
    logs = path.resolve().parent.parent / "logs"
    tasks = {}
    for name, entry in results.items():
        case, points, status, _err, _validated, wall, timing = entry[:7]
        stages = timing if isinstance(timing, dict) and "total_time" in timing else None
        task = Task(name, name.split("/")[0], case, points, status, wall, stages)
        stats_file = logs / f"{name}_{prp}" / "stats.json"
        if prp and stats_file.exists():
            try:
                perf = json.loads(stats_file.read_text()).get("performance", {})
                task.iterations = perf.get("symbolic_exec_iterations")
                task.solver_calls = perf.get("nr_solver_calls")
            except (json.JSONDecodeError, OSError):
                pass
        tasks[name] = task
    return Run(label, path, parse_timeout(path.resolve().parent.parent, tasks), tasks)


def active_stages(runs):
    used = [s for s in STAGE_ORDER
            if any(t.stage(s) for r in runs for t in r.tasks.values() if t.finished)]
    return used + ["untracked"]


def common_finished(runs):
    names = set.intersection(*(set(n for n, t in r.tasks.items() if t.finished) for r in runs))
    return sorted(names)


def change(base: Task, other: Task) -> str:
    return "better" if other.points > base.points else "worse" if other.points < base.points else "same"


# ----------------------------------------------------------------------------- styling

def style_axes(ax, grid_axis="both"):
    ax.grid(True, which="major", axis=grid_axis, color=GRID, linewidth=0.8)
    ax.set_axisbelow(True)
    for side in ("top", "right"):
        ax.spines[side].set_visible(False)
    for side in ("left", "bottom"):
        ax.spines[side].set_color(MUTED)
    ax.tick_params(colors=MUTED, labelcolor=TEXT)


def fmt_s(x):
    if abs(x) >= 100:
        return f"{x:,.0f} s"
    if abs(x) >= 1:
        return f"{x:.1f} s"
    return f"{x:.2f} s"


def short_label(run: Run):
    return run.label


plt.rcParams.update({
    "font.size": 10, "axes.titlesize": 11, "axes.titleweight": "bold", "axes.titlelocation": "left",
    "axes.labelcolor": TEXT, "text.color": TEXT, "figure.facecolor": "white",
    "axes.facecolor": "white", "legend.frameon": False, "savefig.bbox": "tight",
    "svg.fonttype": "none",  # keep text as text in the SVGs
})


# ----------------------------------------------------------------------------- plots

def cactus(ax, runs, value, title, timeout_lines=True):
    lo = math.inf
    for r in runs:
        vals = sorted(max(EPS, v) for v in (value(t) for t in r.tasks.values()) if v is not None)
        if not vals:
            continue
        lo = min(lo, vals[0])
        ax.step([EPS] + vals, range(len(vals) + 1), where="post", color=r.color, linewidth=2,
                label=f"{short_label(r)}: {len(vals)}/{len(r.tasks)}")
    ax.set_xscale("log")
    if lo < math.inf:
        ax.set_xlim(left=lo / 1.5)
    if timeout_lines:
        for to in sorted({r.timeout for r in runs if r.timeout}):
            ax.axvline(to, color=MUTED, linewidth=1, linestyle=":")
            ax.text(to, 0.02, f" timeout {to:.0f} s", transform=ax.get_xaxis_transform(),
                    color=MUTED, fontsize=8, rotation=90, va="bottom", ha="right")
    total = max(len(r.tasks) for r in runs)
    ax.axhline(total, color=MUTED, linewidth=1, linestyle="--")
    ax.set_ylim(0, total * 1.05)
    ax.set_title(title)
    ax.set_ylabel("tasks finished (cumulative)")
    style_axes(ax)
    ax.legend(loc="lower right", fontsize=8)


def plot_cactus(runs, out):
    fig, axes = plt.subplots(1, 2, figsize=(14, 5.5), sharey=True)
    cactus(axes[0], runs, lambda t: t.total, "Explorer total_time")
    axes[0].set_xlabel("simulated timeout [s] (log)")
    cactus(axes[1], runs, lambda t: t.wall if t.finished else None,
           "Harness wall time (includes process start/stop)")
    axes[1].set_xlabel("simulated timeout [s] (log)")
    fig.suptitle("How many tasks finish within t seconds", x=0.01, ha="left", fontweight="bold")
    save(fig, out)


def plot_cactus_free_sa(runs, out):
    """Like the total_time cactus, but as if static pre-analysis took no time."""
    fig, ax = plt.subplots(figsize=(9, 5.5))
    for r in runs:  # the measured curves, faint, for reference
        vals = sorted(max(EPS, t.total) for t in r.tasks.values() if t.finished)
        ax.step([EPS] + vals, range(len(vals) + 1), where="post", color=r.color, linewidth=1,
                linestyle="--", alpha=0.6)
    cactus(ax, runs, lambda t: t.total - t.stage("static_pre_analysis") if t.finished else None,
           "Explorer total_time with static pre-analysis counted as 0 s")
    ax.set_xlabel("simulated timeout [s] (log)")
    handles, labels = ax.get_legend_handles_labels()
    handles += [Line2D([], [], color=MUTED, linewidth=2),
                Line2D([], [], color=MUTED, linewidth=1, linestyle="--")]
    labels += ["solid: pre-analysis free", "dashed: measured total_time"]
    ax.legend(handles, labels, loc="center right", fontsize=8)
    save(fig, out)


def plot_score(runs, out):
    """SV-COMP score if the timeout were t: tasks slower than t score 0 (unknown)."""
    fig, axes = plt.subplots(1, 2, figsize=(14, 5.5))
    for r in runs:
        fin = sorted((t for t in r.tasks.values() if t.finished), key=lambda t: t.total)
        xs = [EPS] + [max(EPS, t.total) for t in fin]
        score = np.concatenate([[0], np.cumsum([t.points for t in fin])])
        correct = np.concatenate([[0], np.cumsum([t.points > 0 for t in fin])])
        wrong = np.concatenate([[0], np.cumsum([t.points < 0 for t in fin])])
        axes[0].step(xs, score, where="post", color=r.color, linewidth=2,
                     label=f"{short_label(r)}: {score[-1]} pts")
        axes[1].step(xs, correct, where="post", color=r.color, linewidth=2,
                     label=f"{short_label(r)}: {correct[-1]} correct")
        axes[1].step(xs, wrong, where="post", color=r.color, linewidth=1.2, linestyle="--")
        if len(fin):
            axes[0].annotate(f"{score[-1]}", (xs[-1], score[-1]), xytext=(4, 0),
                             textcoords="offset points", va="center", fontsize=8, color=TEXT)
    for ax in axes:
        ax.set_xscale("log")
        ax.set_xlabel("simulated timeout [s] (log)")
        lo = min(min(r.finished_totals()) for r in runs)
        ax.set_xlim(left=max(EPS, lo) / 1.5)
        style_axes(ax)
    axes[0].set_title("SV-COMP score at simulated timeout")
    axes[0].set_ylabel("points")
    axes[0].legend(loc="lower right", fontsize=8)
    axes[1].set_title("Correct (solid) and wrong (dashed) verdicts")
    axes[1].set_ylabel("tasks")
    handles, labels = axes[1].get_legend_handles_labels()
    handles += [Line2D([], [], color=MUTED, linewidth=1.2, linestyle="--")]
    labels += ["wrong verdicts (points < 0)"]
    axes[1].legend(handles, labels, loc="center right", fontsize=8)
    save(fig, out)


def plot_stage_cactus(runs, stages, out):
    n = len(stages)
    cols = 3
    rows = math.ceil(n / cols)
    fig, axes = plt.subplots(rows, cols, figsize=(15, 4.3 * rows), sharey=True, squeeze=False)
    for ax, s in zip(axes.flat, stages):
        cactus(ax, runs, lambda t, s=s: t.stage(s), STAGE_NAMES[s], timeout_lines=False)
        ax.set_xlabel("time in stage [s] (log)")
        ax.get_legend().remove()
    for ax in list(axes.flat)[n:]:
        ax.axis("off")
    handles = [Line2D([], [], color=r.color, linewidth=2) for r in runs]
    fig.legend(handles, [short_label(r) for r in runs], loc="upper right", ncol=len(runs), fontsize=9)
    fig.suptitle("Per-stage cactus: tasks whose stage took <= t (finished tasks only)",
                 x=0.01, ha="left", fontweight="bold")
    fig.tight_layout(rect=(0, 0, 1, 0.95))
    save(fig, out)


def pair_points(base: Run, other: Run, value):
    """(x, y, change, name) for every task in both runs; unfinished tasks get value None."""
    pts = []
    for name, bt in base.tasks.items():
        ot = other.tasks.get(name)
        if ot is None:
            continue
        pts.append((value(bt), value(ot), change(bt, ot), name))
    return pts


def scatter_pair(ax, base, other, value, label, unit="s", to_band=True, annotate=6, below="faster"):
    pts = pair_points(base, other, value)
    fin = [(x, y) for x, y, _, _ in pts if x is not None and y is not None]
    if not fin:
        ax.text(0.5, 0.5, "no data", transform=ax.transAxes, ha="center")
        return
    lo = max(EPS, min(min(max(EPS, x), max(EPS, y)) for x, y in fin)) / 1.5
    hi = max(max(x, y) for x, y in fin)
    to_pos = hi * 2.5  # tasks that did not finish sit in a band beyond every finished value
    for key, (color, marker, text) in CHANGE_STYLE.items():
        sel = [(x, y) for x, y, c, _ in pts if c == key and (x is not None or y is not None)]
        if not sel:
            continue
        xs = [max(EPS, x) if x is not None else to_pos for x, _ in sel]
        ys = [max(EPS, y) if y is not None else to_pos for _, y in sel]
        ax.scatter(xs, ys, s=22 if key == "same" else 38, c=color, marker=marker,
                   alpha=0.55 if key == "same" else 0.9, edgecolors="white", linewidths=0.6,
                   label=f"{text} ({len(sel)})", zorder=3 if key != "same" else 2)
    lim = (lo, to_pos * 1.6 if to_band else hi * 1.5)
    xs_line = np.array(lim)
    ax.plot(xs_line, xs_line, color=MUTED, linewidth=1)
    for f, txt in ((2, "2× slower"), (0.5, "2× faster"), (10, "10× slower"), (0.1, "10× faster")):
        ax.plot(xs_line, xs_line * f, color=MUTED, linewidth=0.7, linestyle=":")
    ax.set_xscale("log")
    ax.set_yscale("log")
    ax.set_xlim(lim)
    ax.set_ylim(lim)
    if to_band:
        for axis_line in (ax.axvline, ax.axhline):
            axis_line(to_pos, color=GRID, linewidth=10, zorder=1)
        ax.text(to_pos, lo * 1.1, "not\nfinished", ha="center", va="bottom", fontsize=7, color=MUTED)
        ax.text(lo * 1.1, to_pos * 1.15, "not finished", ha="left", va="bottom", fontsize=7, color=MUTED)
    # Name the tasks with the largest change among those that matter (> 10 units in either run).
    cand = [(abs(math.log(max(EPS, y) / max(EPS, x))), x, y, n) for x, y, _, n in pts
            if x is not None and y is not None and max(x, y) > 10]
    for _, x, y, n in sorted(cand, reverse=True)[:annotate]:
        ax.annotate(n.split("/")[-1], (x, y), xytext=(5, -3), textcoords="offset points",
                    fontsize=7, color=TEXT)
    ax.set_xlabel(f"{short_label(base)}  [{unit}] (log)")
    ax.set_ylabel(f"{short_label(other)}  [{unit}] (log)")
    ax.set_title(f"{label}\n(below the diagonal: {below} than baseline; dotted: 2× and 10×)",
                 fontsize=10)
    style_axes(ax)
    ax.legend(loc="lower right", fontsize=8)


def plot_scatter(runs, out):
    base, others = runs[0], runs[1:]
    fig, axes = plt.subplots(1, len(others), figsize=(7.5 * len(others), 7), squeeze=False)
    for ax, other in zip(axes.flat, others):
        scatter_pair(ax, base, other, lambda t: t.total, "Per-task total_time vs baseline")
    save(fig, out)


def plot_speedup(runs, out):
    base, others = runs[0], runs[1:]
    fig, axes = plt.subplots(len(others), 2, figsize=(15, 5.5 * len(others)), squeeze=False)
    for row, other in zip(axes, others):
        names = common_finished([base, other])
        ratios = np.array([max(EPS, other.tasks[n].total) / max(EPS, base.tasks[n].total) for n in names])
        bt = np.array([base.tasks[n].total for n in names])
        order = np.argsort(ratios)
        ax = row[0]
        ax.plot(np.arange(len(ratios)), ratios[order], color=other.color, linewidth=2)
        ax.axhline(1, color=MUTED, linewidth=1)
        ax.set_yscale("log")
        faster = int((ratios < 1 / 1.1).sum())
        slower = int((ratios > 1.1).sum())
        gmean = float(np.exp(np.log(ratios).mean()))
        ax.fill_between(np.arange(len(ratios)), ratios[order], 1, where=ratios[order] < 1,
                        color=other.color, alpha=0.12, step=None)
        ax.fill_between(np.arange(len(ratios)), ratios[order], 1, where=ratios[order] > 1,
                        color=MUTED, alpha=0.12)
        ax.text(0.02, 0.95, f"{faster} tasks >10% faster\n{slower} tasks >10% slower\n"
                f"{len(ratios) - faster - slower} within ±10%\ngeometric mean ratio {gmean:.2f}",
                transform=ax.transAxes, va="top", fontsize=9)
        ax.set_title(f"Speedup profile: {short_label(other)}\ndivided by baseline {short_label(base)}",
                     fontsize=10)
        ax.set_xlabel("tasks finished in both runs, sorted by ratio")
        ax.set_ylabel("total_time ratio (log, <1 = faster)")
        style_axes(ax)

        ax = row[1]
        cs = [CHANGE_STYLE[change(base.tasks[n], other.tasks[n])] for n in names]
        for key, (color, marker, text) in CHANGE_STYLE.items():
            sel = [i for i, n in enumerate(names) if change(base.tasks[n], other.tasks[n]) == key]
            if sel:
                ax.scatter(np.maximum(EPS, bt[sel]), ratios[sel], s=20, c=color, marker=marker,
                           alpha=0.6 if key == "same" else 0.9, edgecolors="white", linewidths=0.5,
                           label=f"{text} ({len(sel)})")
        del cs
        # Running geometric mean over baseline-time buckets shows where the change pays off.
        edges = np.logspace(np.log10(max(EPS, bt.min())), np.log10(bt.max()), 16)
        mids, gms = [], []
        for a, b in zip(edges[:-1], edges[1:]):
            sel = (bt >= a) & (bt < b)
            if sel.sum() >= 3:
                mids.append(math.sqrt(a * b))
                gms.append(float(np.exp(np.log(ratios[sel]).mean())))
        ax.plot(mids, gms, color=TEXT, linewidth=2, label="geometric mean per time bucket")
        ax.axhline(1, color=MUTED, linewidth=1)
        ax.set_xscale("log")
        ax.set_yscale("log")
        ax.set_title("Ratio vs baseline time:\nfor which task sizes does it pay off?", fontsize=10)
        ax.set_xlabel(f"{short_label(base)} total_time [s] (log)")
        ax.set_ylabel("total_time ratio (log, <1 = faster)")
        style_axes(ax)
        ax.legend(loc="upper right", fontsize=8)
    save(fig, out)


BUCKETS = [("all tasks finished in every run", 0, math.inf), ("baseline < 10 s", 0, 10),
           ("baseline 10–100 s", 10, 100), ("baseline >= 100 s", 100, math.inf)]


def plot_stage_totals(runs, stages, out):
    names = common_finished(runs)
    base = runs[0]
    fig, axes = plt.subplots(len(BUCKETS), 1, figsize=(13, 1.1 + 1.15 * len(runs) * len(BUCKETS)),
                             squeeze=False)
    for ax, (title, lo, hi) in zip(axes.flat, BUCKETS):
        sel = [n for n in names if lo <= base.tasks[n].total < hi]
        ys = np.arange(len(runs))[::-1]
        grand = []
        for y, r in zip(ys, runs):
            left = 0.0
            for s in stages:
                v = sum(r.tasks[n].stage(s) for n in sel)
                ax.barh(y, v, left=left, color=STAGE_COLORS[s], height=0.62, edgecolor="white",
                        linewidth=2, hatch="//" if s == "untracked" else None)
                left += v
            grand.append(left)
        xmax = max(grand) if grand and max(grand) > 0 else 1
        for y, r, g in zip(ys, runs, grand):
            left = 0.0
            for s in stages:
                v = sum(r.tasks[n].stage(s) for n in sel)
                if v / xmax > 0.07:
                    ax.text(left + v / 2, y, fmt_s(v), ha="center", va="center", fontsize=8, color=TEXT)
                left += v
            ax.text(g + xmax * 0.01, y, fmt_s(g), va="center", fontsize=9, fontweight="bold")
        ax.set_yticks(ys, [short_label(r) for r in runs])
        ax.set_xlim(0, xmax * 1.12)
        ax.set_title(f"{title}  ({len(sel)} tasks)")
        style_axes(ax, grid_axis="x")
        ax.set_xlabel("summed time [s]")
    handles = [Patch(facecolor=STAGE_COLORS[s], hatch="//" if s == "untracked" else None,
                     edgecolor="white") for s in stages]
    fig.legend(handles, [STAGE_NAMES[s] for s in stages], loc="upper center",
               ncol=len(stages), fontsize=9, bbox_to_anchor=(0.5, 1.0))
    fig.suptitle("Where the time goes: summed stage time, tasks finished in every run, bucketed by "
                 "baseline total_time", x=0.01, y=1.04, ha="left", fontweight="bold")
    fig.tight_layout(rect=(0, 0, 1, 0.97))
    save(fig, out)


def plot_suite_heatmap(runs, stages, out):
    base, others = runs[0], runs[1:]
    suites = sorted({t.suite for t in base.tasks.values()})
    cols = stages + ["total_time"]
    fig, axes = plt.subplots(1, len(others), figsize=(12 * len(others), 0.45 * len(suites) + 2.5),
                             squeeze=False)
    cmap = LinearSegmentedColormap.from_list("div", ["#1c5cab", "#86b6ef", "#f2f1ec", "#f0a088", "#b8321f"])
    for ax, other in zip(axes.flat, others):
        names = common_finished([base, other])
        data = np.zeros((len(suites), len(cols)))
        counts = []
        for i, su in enumerate(suites):
            sel = [n for n in names if base.tasks[n].suite == su]
            for j, c in enumerate(cols):
                if c == "total_time":
                    data[i, j] = sum(other.tasks[n].total - base.tasks[n].total for n in sel)
                else:
                    data[i, j] = sum(other.tasks[n].stage(c) - base.tasks[n].stage(c) for n in sel)
            to_b = sum(1 for t in base.tasks.values() if t.suite == su and not t.finished)
            to_o = sum(1 for t in other.tasks.values() if t.suite == su and not t.finished)
            counts.append(f"{su}  ({len(sel)} tasks, not finished {to_b}→{to_o})")
        vmax = max(1.0, np.abs(data).max())
        im = ax.imshow(data, cmap=cmap, norm=SymLogNorm(linthresh=1, vmin=-vmax, vmax=vmax), aspect="auto")
        for i in range(len(suites)):
            for j in range(len(cols)):
                v = data[i, j]
                ax.text(j, i, f"{v:+,.0f}" if abs(v) >= 1 else f"{v:+.2f}", ha="center", va="center",
                        fontsize=8, color="white" if abs(v) > vmax / 8 and abs(v) > 30 else TEXT)
        ax.set_xticks(range(len(cols)), [STAGE_NAMES.get(c, "total") for c in cols], rotation=20, ha="right")
        ax.set_yticks(range(len(suites)), counts)
        for side in ax.spines.values():
            side.set_visible(False)
        ax.set_title(f"Summed time difference [s], {short_label(other)} − {short_label(base)}\n"
                     "blue = faster than baseline, red = slower; tasks finished in both runs")
        fig.colorbar(im, ax=ax, shrink=0.7, label="Δ seconds (symlog)")
    save(fig, out)


def plot_suite_dots(runs, out):
    names = common_finished(runs)
    suites = sorted({runs[0].tasks[n].suite for n in names},
                    key=lambda su: sum(runs[0].tasks[n].total for n in names if runs[0].tasks[n].suite == su))
    fig, axes = plt.subplots(1, 2, figsize=(14, 0.4 * len(suites) + 2), sharey=True)
    ys = np.arange(len(suites))
    for k, (ax, agg, title) in enumerate(zip(
            axes, (sum, statistics.median), ("Summed total_time per suite", "Median total_time per task"))):
        for i, su in enumerate(suites):
            vals = [agg([r.tasks[n].total for n in names if r.tasks[n].suite == su]) for r in runs]
            ax.plot([min(vals), max(vals)], [i, i], color=GRID, linewidth=3, zorder=1)
        for r in runs:
            vals = [agg([r.tasks[n].total for n in names if r.tasks[n].suite == su]) for su in suites]
            ax.scatter(np.maximum(EPS, vals), ys, s=60, color=r.color, edgecolors="white", linewidths=1.5,
                       zorder=3, label=short_label(r))
        ax.set_xscale("log")
        ax.set_title(title)
        ax.set_xlabel("seconds (log)")
        style_axes(ax, grid_axis="x")
    axes[0].set_yticks(ys, [f"{su} ({sum(1 for n in names if runs[0].tasks[n].suite == su)})" for su in suites])
    fig.legend(*axes[0].get_legend_handles_labels(), loc="lower center", ncol=len(runs), fontsize=9,
               bbox_to_anchor=(0.5, -0.04))
    fig.suptitle("Per suite, tasks finished in every run", x=0.01, ha="left", fontweight="bold")
    save(fig, out)


def plot_stage_share(runs, stages, out):
    fig, axes = plt.subplots(2 * len(runs), 1, figsize=(14, 4.2 * len(runs)), squeeze=False,
                             gridspec_kw={"height_ratios": [1, 2.2] * len(runs)})
    for k, r in enumerate(runs):
        fin = sorted((t for t in r.tasks.values() if t.finished), key=lambda t: t.total)
        x = np.arange(len(fin))
        top, ax = axes[2 * k, 0], axes[2 * k + 1, 0]
        top.plot(x, [max(EPS, t.total) for t in fin], color=r.color, linewidth=2)
        top.set_yscale("log")
        top.set_ylabel("total [s]")
        top.set_title(f"{short_label(r)}: stage share per task, tasks sorted by total_time")
        style_axes(top)
        top.set_xticklabels([])
        shares = []
        for s in stages:
            shares.append([t.stage(s) / max(EPS, t.total + t.untracked) for t in fin])
        ax.stackplot(x, shares, colors=[STAGE_COLORS[s] for s in stages], linewidth=0)
        ax.set_ylim(0, 1)
        ax.set_xlim(0, len(fin) - 1)
        top.set_xlim(0, len(fin) - 1)
        ax.set_ylabel("share of wall time")
        ax.set_xlabel("task rank (by total_time)")
        style_axes(ax)
        # Mark where the total crosses round numbers so rank maps back to seconds.
        for sec in (1, 10, 100, 1000):
            idx = np.searchsorted([t.total for t in fin], sec)
            if 0 < idx < len(fin):
                for a in (top, ax):
                    a.axvline(idx, color="white" if a is ax else GRID, linewidth=1, linestyle="--")
                ax.text(idx, 1.01, f"{sec} s", ha="center", va="bottom", fontsize=7, color=MUTED,
                        transform=ax.get_xaxis_transform())
    handles = [Patch(facecolor=STAGE_COLORS[s]) for s in stages]
    fig.legend(handles, [STAGE_NAMES[s] for s in stages], loc="upper center", ncol=len(stages), fontsize=9)
    fig.tight_layout(rect=(0, 0, 1, 0.97))
    save(fig, out)


def plot_iterations(runs, out):
    base, others = runs[0], runs[1:]
    if not any(t.iterations for t in base.tasks.values()):
        return False
    fig, axes = plt.subplots(len(others), 3, figsize=(19, 6.3 * len(others)), squeeze=False)
    for row, other in zip(axes, others):
        scatter_pair(row[0], base, other,
                     lambda t: t.iterations if t.finished and t.iterations is not None else None,
                     "Symbolic-execution iterations", unit="iterations", annotate=5, below="fewer")
        scatter_pair(row[1], base, other,
                     lambda t: t.solver_calls + 1 if t.finished and t.solver_calls is not None else None,
                     "Solver calls + 1", unit="calls + 1", annotate=5, below="fewer")
        scatter_pair(row[2], base, other,
                     lambda t: t.stage("symbolic_executor") / t.iterations
                     if t.finished and t.iterations else None,
                     "Executor time per iteration", unit="s/iteration", annotate=5, below="cheaper")
    save(fig, out)
    return True


def plot_top_deltas(runs, stages, out, top=30):
    base, others = runs[0], runs[1:]
    fig, axes = plt.subplots(1, len(others), figsize=(12 * len(others), 0.32 * top + 2), squeeze=False)
    for ax, other in zip(axes.flat, others):
        names = common_finished([base, other])
        names = sorted(names, key=lambda n: abs(other.tasks[n].total + other.tasks[n].untracked
                                                 - base.tasks[n].total - base.tasks[n].untracked),
                       reverse=True)[:top][::-1]
        ys = np.arange(len(names))
        for i, n in enumerate(names):
            pos, neg = 0.0, 0.0
            for s in stages:
                d = other.tasks[n].stage(s) - base.tasks[n].stage(s)
                if d >= 0:
                    ax.barh(i, d, left=pos, color=STAGE_COLORS[s], height=0.7, edgecolor="white",
                            linewidth=1, hatch="//" if s == "untracked" else None)
                    pos += d
                else:
                    ax.barh(i, d, left=neg, color=STAGE_COLORS[s], height=0.7, edgecolor="white",
                            linewidth=1, hatch="//" if s == "untracked" else None)
                    neg += d
            net = pos + neg
            ax.plot([net], [i], marker="D", color=TEXT, markersize=4, zorder=4)
            b, o = base.tasks[n], other.tasks[n]
            ax.text(1.01, i, f"{fmt_s(b.wall)} → {fmt_s(o.wall)}", transform=ax.get_yaxis_transform(),
                    va="center", fontsize=7, color=MUTED)
        ax.axvline(0, color=MUTED, linewidth=1)
        ax.set_xscale("symlog", linthresh=10)
        ax.set_yticks(ys, names, fontsize=8)
        ax.set_xlabel(f"Δ seconds per stage, {short_label(other)} − baseline (symlog)")
        ax.set_title(f"Top {len(names)} tasks by |Δ wall time| (◆ = net change; right column: wall time)")
        style_axes(ax, grid_axis="x")
        handles = [Patch(facecolor=STAGE_COLORS[s], hatch="//" if s == "untracked" else None,
                         edgecolor="white") for s in stages]
        ax.legend(handles, [STAGE_NAMES[s] for s in stages], loc="lower right", fontsize=8)
    save(fig, out)


def plot_sa_payoff(runs, out):
    """For a pair where one run spends real time in static pre-analysis and the other does not."""
    base, others = runs[0], runs[1:]
    pairs = []
    for other in others:
        names = common_finished([base, other])
        sa_b = sum(base.tasks[n].stage("static_pre_analysis") for n in names)
        sa_o = sum(other.tasks[n].stage("static_pre_analysis") for n in names)
        if max(sa_b, sa_o) > 10 * max(min(sa_b, sa_o), 1e-9):
            pairs.append((base, other) if sa_b > sa_o else (other, base))
    if not pairs:
        return False
    fig, axes = plt.subplots(1, len(pairs), figsize=(8 * len(pairs), 7), squeeze=False)
    for ax, (sa, nosa) in zip(axes.flat, pairs):
        names = common_finished([sa, nosa])
        cost = np.array([sa.tasks[n].stage("static_pre_analysis") for n in names])
        rest = lambda t: t.total - t.stage("static_pre_analysis")  # noqa: E731
        saved = np.array([rest(nosa.tasks[n]) - rest(sa.tasks[n]) for n in names])
        pays = saved > cost
        ax.scatter(np.maximum(EPS, cost[~pays]), saved[~pays], s=18, c=CHANGE_STYLE["same"][0], alpha=0.5,
                   edgecolors="white", linewidths=0.5, label=f"does not pay off ({(~pays).sum()})")
        ax.scatter(np.maximum(EPS, cost[pays]), saved[pays], s=34, c=sa.color, marker="^",
                   edgecolors="white", linewidths=0.5, label=f"pays off: saved > cost ({pays.sum()})")
        xs = np.logspace(np.log10(max(EPS, cost.min())), np.log10(cost.max() * 1.5), 50)
        ax.plot(xs, xs, color=MUTED, linewidth=1)
        ax.text(xs[-1], xs[-1], " saved = cost", fontsize=8, color=MUTED, va="bottom", ha="right")
        ax.axhline(0, color=MUTED, linewidth=0.8, linestyle=":")
        ax.set_xscale("log")
        ax.set_yscale("symlog", linthresh=1)
        order = np.argsort(-np.abs(saved))[:6]
        for i in order:
            ax.annotate(names[i].split("/")[-1], (max(EPS, cost[i]), saved[i]), xytext=(5, 0),
                        textcoords="offset points", fontsize=7)
        net = float(saved.sum() - cost.sum())
        ax.text(0.98, 0.97, f"pre-analysis cost: {fmt_s(cost.sum())}\n"
                f"saved in other stages: {fmt_s(saved.sum())}\nnet: {fmt_s(net)}",
                transform=ax.transAxes, va="top", ha="right", fontsize=9,
                bbox=dict(facecolor="white", edgecolor=GRID))
        nf_sa = sum(1 for t in sa.tasks.values() if not t.finished)
        nf_no = sum(1 for t in nosa.tasks.values() if not t.finished)
        ax.set_title(f"Does static pre-analysis pay off per task?\n{short_label(sa)} vs {short_label(nosa)} "
                     f"(unfinished: {nf_sa} vs {nf_no})")
        ax.set_xlabel("static pre-analysis time [s] (log)")
        ax.set_ylabel("time saved in all other stages [s] (symlog)")
        style_axes(ax)
        ax.legend(loc="lower right", fontsize=8)
    save(fig, out)
    return True


def plot_overhead(runs, out):
    """Wall time not covered by the explorer's own timer, per task."""
    fig, axes = plt.subplots(1, 2, figsize=(14, 5.5))
    for r in runs:
        fin = [t for t in r.tasks.values() if t.finished]
        axes[0].scatter([max(EPS, t.total) for t in fin], [max(EPS, t.untracked) for t in fin], s=14,
                        color=r.color, alpha=0.6, edgecolors="none", label=short_label(r))
    cactus(axes[1], runs, lambda t: t.untracked, "Cactus of time outside the explorer timer",
           timeout_lines=False)
    axes[1].set_xlabel("wall time − total_time [s] (log)")
    ax = axes[0]
    ax.set_xscale("log")
    ax.set_yscale("log")
    ax.set_xlabel("explorer total_time [s] (log)")
    ax.set_ylabel("wall time − total_time [s] (log)")
    ax.set_title("Time outside the explorer's timer vs total_time")
    lo, hi = ax.get_xlim()
    ax.plot([lo, hi], [lo, hi], color=MUTED, linewidth=0.8, linestyle=":")
    style_axes(ax)
    ax.legend(loc="upper left", fontsize=8)
    save(fig, out)


def save(fig, out):
    fig.savefig(out)
    plt.close(fig)


# ----------------------------------------------------------------------------- summary

def summary_md(runs, stages):
    base = runs[0]
    lines = ["# Run comparison", "", "| run | timeout | finished | score | correct | wrong | "
             "median total | mean total | summed total | summed wall |",
             "|---|---|---|---|---|---|---|---|---|---|"]
    for r in runs:
        fin = [t for t in r.tasks.values() if t.finished]
        tot = [t.total for t in fin]
        lines.append(
            f"| {r.label} | {r.timeout or '?'} s | {len(fin)}/{len(r.tasks)} | "
            f"{sum(t.points for t in r.tasks.values())} | {sum(t.points > 0 for t in r.tasks.values())} | "
            f"{sum(t.points < 0 for t in r.tasks.values())} | {statistics.median(tot):.2f} s | "
            f"{statistics.mean(tot):.1f} s | {sum(tot):,.0f} s | {sum(t.wall for t in fin):,.0f} s |")
    names = common_finished(runs)
    lines += ["", f"Stage sums over the {len(names)} tasks finished in every run:", "",
              "| run | " + " | ".join(STAGE_NAMES[s] for s in stages) + " |",
              "|---|" + "---|" * len(stages)]
    for r in runs:
        lines.append(f"| {r.label} | " + " | ".join(
            f"{sum(r.tasks[n].stage(s) for n in names):,.1f} s" for s in stages) + " |")
    for other in runs[1:]:
        lines += ["", f"## {other.label} vs {base.label}", ""]
        only_b = [n for n, t in base.tasks.items() if t.finished and n in other.tasks and not other.tasks[n].finished]
        only_o = [n for n, t in other.tasks.items() if t.finished and n in base.tasks and not base.tasks[n].finished]
        lines.append(f"- Finished only in baseline ({len(only_b)}): " + ", ".join(
            f"`{n}` ({fmt_s(base.tasks[n].total)})" for n in sorted(only_b)))
        lines.append(f"- Finished only in {other.label} ({len(only_o)}): " + ", ".join(
            f"`{n}` ({fmt_s(other.tasks[n].total)})" for n in sorted(only_o)))
        changed = [n for n in base.tasks if n in other.tasks and base.tasks[n].case != other.tasks[n].case]
        lines += ["", "| task | baseline | time | other | time |", "|---|---|---|---|---|"]
        for n in sorted(changed):
            b, o = base.tasks[n], other.tasks[n]
            ts = lambda t: fmt_s(t.total) if t.finished else t.status  # noqa: E731
            lines.append(f"| `{n}` | {b.case} ({b.points:+d}) | {ts(b)} | {o.case} ({o.points:+d}) | {ts(o)} |")
        if not changed:
            lines.append("| (no verdict changes) | | | | |")
    return "\n".join(lines) + "\n"


CAPTIONS = {
    "01b_cactus_free_sa.svg": "Same cactus with each task's static pre-analysis time subtracted: what the "
                              "runs would look like if the pre-analysis were free. Dashed: the measured curves.",
    "01_cactus.svg": "Cumulative finished tasks over a simulated timeout, for the explorer's total_time "
                     "and for the harness wall time.",
    "02_score.svg": "SV-COMP score if the timeout were t: a task slower than t counts as unknown (0 points).",
    "03_stage_cactus.svg": "One cactus per stage. Shows which stage makes the difference and at which scale.",
    "04_scatter.svg": "Every task, baseline time vs other run time (log-log). Dotted lines: 2x and 10x. "
                      "Tasks that did not finish sit in the grey band; markers show verdict changes.",
    "05_speedup.svg": "Left: sorted per-task ratio. Right: ratio against baseline time with a bucketed "
                      "geometric mean, showing for which task sizes the change helps.",
    "06_stage_totals.svg": "Stacked summed stage time on the tasks finished in every run, split by "
                           "baseline task size.",
    "07_suite_heatmap.svg": "Summed per-stage time difference per suite (symlog color).",
    "08_suite_dots.svg": "Sum and median of total_time per suite for each run.",
    "09_stage_share.svg": "Stage share of each task's wall time, tasks sorted by total_time.",
    "10_iterations.svg": "Iterations, solver calls and executor time per iteration, per task (from stats.json).",
    "11_top_deltas.svg": "The tasks with the largest wall-time change, broken down by stage.",
    "12_sa_payoff.svg": "Per task: static pre-analysis cost vs the time it saved in the other stages.",
    "13_overhead.svg": "Wall time the explorer's timer does not cover (process start, stats.json, shutdown).",
}


def write_html(outdir: Path, images, summary):
    parts = ["<!doctype html><html><head><meta charset='utf-8'><title>Run comparison</title>",
             "<style>body{font:14px/1.5 system-ui,sans-serif;max-width:1500px;margin:24px auto;padding:0 16px;"
             "color:#2b2b29;background:#fff}img{max-width:100%;border:1px solid #e6e5e0}"
             "table{border-collapse:collapse;font-size:13px}td,th{border:1px solid #e6e5e0;padding:3px 8px;"
             "text-align:left}code{font-size:12px}h2{margin-top:40px}</style></head><body>"]
    parts.append(md_to_html(summary))
    for img in images:
        data = base64.b64encode((outdir / img).read_bytes()).decode()
        parts.append(f"<h2>{img}</h2><p>{CAPTIONS.get(img, '')}</p><img src='data:image/svg+xml;base64,{data}'>")
    parts.append("</body></html>")
    (outdir / "report.html").write_text("\n".join(parts))


def md_to_html(md):
    """Just enough Markdown for summary.md: headings, bullet lines, pipe tables, `code`."""
    out, in_table = [], False
    esc = lambda s: re.sub(r"`([^`]*)`", r"<code>\1</code>", s.replace("&", "&amp;").replace("<", "&lt;"))  # noqa: E731
    for line in md.splitlines():
        if line.startswith("|"):
            cells = [c.strip() for c in line.strip("|").split("|")]
            if all(set(c) <= {"-"} for c in cells):
                continue
            if not in_table:
                out.append("<table>")
                in_table = True
                out.append("<tr>" + "".join(f"<th>{esc(c)}</th>" for c in cells) + "</tr>")
            else:
                out.append("<tr>" + "".join(f"<td>{esc(c)}</td>" for c in cells) + "</tr>")
            continue
        if in_table:
            out.append("</table>")
            in_table = False
        if line.startswith("## "):
            out.append(f"<h2>{esc(line[3:])}</h2>")
        elif line.startswith("# "):
            out.append(f"<h1>{esc(line[2:])}</h1>")
        elif line.startswith("- "):
            out.append(f"<p>• {esc(line[2:])}</p>")
        elif line:
            out.append(f"<p>{esc(line)}</p>")
    if in_table:
        out.append("</table>")
    return "\n".join(out)


def resolve_results(arg: str, prp: str) -> Path:
    """Return the results file for a results file or a run directory (or its results/ folder)."""
    path = Path(arg)
    if path.is_file():
        return path
    if not path.is_dir():
        raise SystemExit(f"error: {arg} is neither a results file nor a directory")
    results_dir = path / "results" if (path / "results").is_dir() else path
    matches = sorted(results_dir.glob(f"results_{prp}.prp_*.json"))
    if len(matches) != 1:
        found = ", ".join(p.name for p in sorted(results_dir.glob("results_*.json"))) or "none"
        raise SystemExit(f"error: expected one results_{prp}.prp_*.json in {results_dir}, "
                         f"found {len(matches)} (results files there: {found})")
    return matches[0]


def default_outdir(paths, labels) -> Path:
    """runs/comparison_<A>_vs_<B>..., from the labels or else the run directory names."""
    if labels:
        names = labels
    else:
        names = [p.resolve().parent.parent.name.removeprefix("run_") for p in paths]
    names = [re.sub(r"[^A-Za-z0-9._-]+", "-", n).strip("-") or "run" for n in names]
    return RUNS_DIR / ("comparison_" + "_vs_".join(names))


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("results", nargs="+",
                        help="results_<prp>_<timestamp>.json files or run directories; the first is the baseline")
    parser.add_argument("--prp", default="valid-assert",
                        help="property whose results file is used for run directories (default: valid-assert)")
    parser.add_argument("-o", "--outdir",
                        help="output directory (default: runs/comparison_<A>_vs_<B>..., from the labels "
                             "or the run directory names)")
    parser.add_argument("--label", action="append", help="label per run, in order")
    args = parser.parse_args()

    if len(args.results) < 2:
        parser.error("need at least two runs")
    if len(args.results) > len(RUN_COLORS):
        parser.error(f"at most {len(RUN_COLORS)} runs are supported")
    if args.label and len(args.label) != len(args.results):
        parser.error("--label must be given once per run")
    paths = [resolve_results(p, args.prp) for p in args.results]
    labels = args.label or [default_label(p) for p in paths]
    runs = [load_run(p, lbl) for p, lbl in zip(paths, labels)]
    for r in runs:
        if not any(t.finished for t in r.tasks.values()):
            parser.error(f"{r.path} has no finished tasks with timing data ({len(r.tasks)} tasks in total)")
    for r, c in zip(runs, RUN_COLORS):
        r.color = c
    timeouts = {r.timeout for r in runs}
    if len(timeouts) > 1:
        print(f"Warning: runs used different timeouts {sorted(t or 0 for t in timeouts)}; "
              "only compare runs made with the same timeout.")

    outdir = Path(args.outdir) if args.outdir else default_outdir(paths, args.label)
    outdir.mkdir(parents=True, exist_ok=True)
    stages = active_stages(runs)

    images = []

    def emit(name, fn, *a):
        path = outdir / name
        if fn(*a, path) is not False:
            images.append(name)
            print(f"wrote {path}")

    emit("01_cactus.svg", plot_cactus, runs)
    emit("01b_cactus_free_sa.svg", plot_cactus_free_sa, runs)
    emit("02_score.svg", plot_score, runs)
    emit("03_stage_cactus.svg", plot_stage_cactus, runs, stages)
    emit("04_scatter.svg", plot_scatter, runs)
    emit("05_speedup.svg", plot_speedup, runs)
    emit("06_stage_totals.svg", plot_stage_totals, runs, stages)
    emit("07_suite_heatmap.svg", plot_suite_heatmap, runs, stages)
    emit("08_suite_dots.svg", plot_suite_dots, runs)
    emit("09_stage_share.svg", plot_stage_share, runs, stages)
    emit("10_iterations.svg", plot_iterations, runs)
    emit("11_top_deltas.svg", plot_top_deltas, runs, stages)
    emit("12_sa_payoff.svg", plot_sa_payoff, runs)
    emit("13_overhead.svg", plot_overhead, runs)

    summary = summary_md(runs, stages)
    (outdir / "summary.md").write_text(summary)
    write_html(outdir, images, summary)
    print(f"wrote {outdir / 'summary.md'} and {outdir / 'report.html'}")


if __name__ == "__main__":
    main()
