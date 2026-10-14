#!/usr/bin/env python3
"""Compares SV-COMP runs task by task, e.g. with and without static pre-analysis.

    ./compare.py runs/run_A runs/run_B [...] [--csv out.csv] [--suite S] [--only-diff]

Per task and run: the result case (expected -> actual), wall time, rounds, solver calls, the SA
duration, and from explorer.log the DFS visits (one `[DFS]` line per branch visited, summed over
all rounds), how many of them had a side marked uninteresting (pruned), and how many had no SA
node (`/None`: the walker gave up there). Rounds of a task that timed out (no stats.json) are
counted from the ROUND headers in explorer.log.
"""

import argparse
import csv
import glob
import json
import re
import sys
from pathlib import Path

SHORT = {'safe -> safe': 'ok', 'violation -> violation': 'ok', 'violation -> safe': 'FALSE-SAFE',
         'safe -> violation': 'FALSE-VIOL', 'safe -> unknown': 'unk', 'violation -> unknown': 'unk',
         'safe -> crash': 'crash', 'violation -> crash': 'crash'}

DFS_RE = re.compile(r'\[DFS\] @\S+?/(\S+) .*skip_is_interesting=(\w+), branch_is_interesting=(\w+)')


def explorer_stats(log: Path) -> dict:
    visits = pruned = none = rounds = 0
    if log.exists():
        with open(log, errors='replace') as f:
            for line in f:
                if '[DFS]' in line:
                    m = DFS_RE.search(line)
                    if not m:
                        continue
                    visits += 1
                    if m.group(1) == 'None':
                        none += 1
                    if 'False' in (m.group(2), m.group(3)):
                        pruned += 1
                elif ' ROUND (' in line:
                    rounds += 1
    return {'visits': visits, 'pruned': pruned, 'none': none, 'log_rounds': rounds}


def load_run(run: Path) -> dict:
    info = json.loads((run / 'run_info.json').read_text()) if (run / 'run_info.json').exists() else {}
    mode = info.get('sa_mode', '?')
    if info.get('sa_rank_distance'):
        mode += '+rank'
    tasks = {}
    for f in glob.glob(str(run / 'results' / 'results_valid-assert.prp_*.json')):
        for name, r in json.load(open(f))['results'].items():
            log_dir = run / 'logs' / f'{name}_valid-assert'
            row = {'case': r[0], 'status': r[2], 'time': r[5]}
            stats_file = log_dir / 'stats.json'
            ex = explorer_stats(log_dir / 'explorer.log')
            if stats_file.exists():
                s = json.loads(stats_file.read_text())
                row['rounds'] = s['performance']['symbolic_exec_iterations']
                row['solver'] = s['performance']['nr_solver_calls']
                sa = s.get('static_analysis') or {}
                row['sa_s'] = sa.get('duration_s')
                row['sa_status'] = sa.get('status')
                row['context_loss'] = s['soundness'].get('symbolic_context_loss')
            else:
                row['rounds'] = ex['log_rounds']
                row['solver'] = None
            row.update(ex)
            tasks[name] = row
    return {'name': run.name, 'mode': mode, 'tasks': tasks}


def fmt(v, spec=''):
    return '-' if v is None else format(v, spec)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('runs', nargs='+', type=Path)
    ap.add_argument('--csv', type=Path)
    ap.add_argument('--suite')
    ap.add_argument('--only-diff', action='store_true', help='only tasks whose result case differs')
    args = ap.parse_args()
    runs = [load_run(r) for r in args.runs]
    names = sorted(set().union(*(r['tasks'] for r in runs)))
    if args.suite:
        names = [n for n in names if n.split('/')[0] == args.suite]

    head = f'{"task":48}' + ''.join(f' | {r["mode"][:14]:14} {"time":>6} {"rnds":>5} {"solv":>5} {"vis":>7} {"prun":>6} {"none":>6}' for r in runs)
    print(head)
    print('-' * len(head))
    rows = []
    for n in names:
        cells = [r['tasks'].get(n) for r in runs]
        if args.only_diff and len({c and c['case'] for c in cells}) == 1:
            continue
        line = f'{n:48}'
        for c in cells:
            if c is None:
                line += f' | {"(missing)":14} {"":>6} {"":>5} {"":>5} {"":>7} {"":>6} {"":>6}'
                continue
            tag = SHORT.get(c['case'], c['case'])
            if c['status'] == 'timeout':
                tag = 'TIMEOUT' if tag in ('crash', 'unk') else tag + '/TO'
            line += (f' | {tag:14} {c["time"]:6.1f} {fmt(c["rounds"]):>5} {fmt(c["solver"]):>5}'
                     f' {c["visits"]:7} {c["pruned"]:6} {c["none"]:6}')
        print(line)
        for r, c in zip(runs, cells):
            if c:
                rows.append({'task': n, 'run': r['name'], 'mode': r['mode'], **c})
    for r in runs:
        cases = [t['case'] for n, t in r['tasks'].items() if n in names]
        bad = sum(1 for c in cases if c == 'violation -> safe')
        print(f'{r["name"]} ({r["mode"]}): {len(cases)} tasks, '
              f'{sum(1 for c in cases if SHORT.get(c) == "ok")} correct, {bad} false SAFE, '
              f'{sum(1 for c in cases if c == "safe -> violation")} false VIOLATION')
    if args.csv:
        with open(args.csv, 'w', newline='') as f:
            w = csv.DictWriter(f, fieldnames=sorted({k for row in rows for k in row}))
            w.writeheader()
            w.writerows(rows)


if __name__ == '__main__':
    sys.exit(main())
