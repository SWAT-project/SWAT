#!/usr/bin/env python3
"""Plots rounds and wall time against the family parameter n, with and without SA, and writes
the same numbers as Markdown tables (families.md).

    ../../scripts/.venv/bin/python3 report.py ../../runs/run_SA ../../runs/run_NOSA --out ../report

One small panel per family variant, SAFE tasks only (their search is exhaustive, so it shows the
whole search space; VIOLATION tasks stop early). A task that timed out is drawn as a hollow marker
at its last round count / at the timeout: a lower bound.
"""

import argparse
import re
import sys
from collections import defaultdict
from pathlib import Path

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.ticker as ticker

sys.path.insert(0, str(Path(__file__).resolve().parent))
from compare import load_run  # noqa: E402

FAMILIES = ['dispatcher', 'validation-gauntlet', 'error-handling', 'dead-callees',
            'virtual-dispatch', 'irrelevant-prefix']
STYLE = {  # mode -> (label, color, marker, linestyle); color is not the only cue
    'none': ('no SA', '#2a78d6', 'o', '--'),
    'sequential': ('SA', '#eb6834', 's', '-'),
}
INK, MUTED, GRID = '#1a1a19', '#6b6a63', '#e6e5e0'


def series(run):
    """{(family, variant): {n: row}} for the SAFE tasks of a run."""
    out = defaultdict(dict)
    for name, row in run['tasks'].items():
        m = re.fullmatch(r'([\w-]+)/(\w+)_n(\d+)_true', name)
        if m and m.group(1) in FAMILIES:
            out[(m.group(1), m.group(2))][int(m.group(3))] = row
    return out


def plot(runs, metric, ylabel, out: Path):
    data = [(r['mode'], series(r)) for r in runs]
    panels = sorted({k for _, s in data for k in s}, key=lambda k: (FAMILIES.index(k[0]), k[1]))
    cols = 4
    rows = (len(panels) + cols - 1) // cols
    fig, axes = plt.subplots(rows, cols, figsize=(3.2 * cols, 2.5 * rows), squeeze=False)
    for ax, key in zip(axes.flat, panels):
        for mode, s in data:
            label, color, marker, ls = STYLE[mode]
            pts = sorted(s.get(key, {}).items())
            xs = [n for n, _ in pts]
            ys = [max(row[metric] or 0, 1) for _, row in pts]
            ax.plot(xs, ys, color=color, linestyle=ls, linewidth=2, label=label)
            done = [(x, y) for (x, y), (_, row) in zip(zip(xs, ys), pts) if row['status'] != 'timeout']
            to = [(x, y) for (x, y), (_, row) in zip(zip(xs, ys), pts) if row['status'] == 'timeout']
            if done:
                ax.scatter(*zip(*done), color=color, marker=marker, s=40, zorder=3,
                           edgecolors='white', linewidths=1.5)
            if to:
                ax.scatter(*zip(*to), facecolors='white', edgecolors=color, marker=marker, s=40,
                           linewidths=2, zorder=3)
        ax.set_title(f'{key[0]}/{key[1]}', fontsize=9, color=INK)
        ns = sorted({n for _, s in data for n in s.get(key, {})})
        if all(n & (n - 1) == 0 for n in ns):
            ax.set_xscale('log', base=2)
        ax.set_xticks(ns)
        ax.xaxis.set_major_formatter(ticker.FormatStrFormatter('%d'))
        ax.xaxis.set_minor_locator(ticker.NullLocator())
        ax.set_yscale('log')
        ax.yaxis.set_major_locator(ticker.LogLocator(subs=(1, 2, 5)))
        ax.yaxis.set_major_formatter(ticker.FuncFormatter(lambda v, _: f'{v:g}'))
        ax.yaxis.set_minor_formatter(ticker.NullFormatter())
        ax.tick_params(colors=MUTED, labelsize=7)
        for sp in ('top', 'right'):
            ax.spines[sp].set_visible(False)
        for sp in ('left', 'bottom'):
            ax.spines[sp].set_color(GRID)
        ax.grid(True, color=GRID, linewidth=0.6)
        ax.set_xlabel('n', fontsize=8, color=MUTED)
        ax.set_ylabel(ylabel, fontsize=8, color=MUTED)
    for ax in list(axes.flat)[len(panels):]:
        ax.axis('off')
    handles, labels = axes.flat[0].get_legend_handles_labels()
    fig.legend(handles, labels, loc='upper center', ncol=2, frameon=False, fontsize=9)
    fig.text(0.5, 0.005, 'SAFE tasks. Hollow marker: timed out (lower bound).', ha='center',
             fontsize=8, color=MUTED)
    fig.tight_layout(rect=(0, 0.02, 1, 0.96))
    fig.savefig(out, dpi=130, facecolor='#fcfcfb')
    print(f'wrote {out}')


def table(runs, out: Path):
    """One Markdown table per family: rounds and time per variant and n, SAFE and VIOLATION."""
    by_mode = {r['mode']: r['tasks'] for r in runs}
    sa, nosa = by_mode['sequential'], by_mode['none']
    lines = ['Rounds (wall time in s) without SA -> with SA. TO: timed out (rounds so far).', '']

    def cell(row):
        if row is None:
            return '-'
        r = f'TO({row["rounds"]})' if row['status'] == 'timeout' else str(row['rounds'])
        tag = {'violation -> safe': ' **false SAFE**', 'safe -> unknown': ' unk',
               'violation -> unknown': ' unk'}.get(row['case'], '')
        return f'{r} ({row["time"]:.0f}){tag}'

    for fam in FAMILIES:
        keys = sorted({re.fullmatch(r'[\w-]+/(\w+)_n(\d+)_(true|false)', n).groups()
                       for n in sa if n.startswith(fam + '/')}, key=lambda k: (k[0], int(k[1]), k[2]))
        if not keys:
            continue
        lines += [f'### {fam}', '', '| variant | n | SAFE: no SA | SAFE: SA | VIOLATION: no SA | VIOLATION: SA |',
                  '|---|---:|---|---|---|---|']
        for variant, n in sorted({(k[0], int(k[1])) for k in keys}):
            name = f'{fam}/{variant}_n{n:02d}'
            lines.append(f'| {variant} | {n} | {cell(nosa.get(name + "_true"))} | {cell(sa.get(name + "_true"))} '
                         f'| {cell(nosa.get(name + "_false"))} | {cell(sa.get(name + "_false"))} |')
        lines.append('')
    out.write_text('\n'.join(lines))
    print(f'wrote {out}')


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('runs', nargs=2, type=Path, help='run with SA, run without')
    ap.add_argument('--out', type=Path, default=Path('.'))
    args = ap.parse_args()
    runs = [load_run(r) for r in args.runs]
    args.out.mkdir(parents=True, exist_ok=True)
    plot(runs, 'rounds', 'rounds', args.out / 'rounds.png')
    plot(runs, 'time', 'wall time [s]', args.out / 'time.png')
    table(runs, args.out / 'families.md')


if __name__ == '__main__':
    main()
