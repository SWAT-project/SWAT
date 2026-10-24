#!/usr/bin/env python3
"""
Runs the SA experiments on the applications: for each case, builds the application with its
inserted bug switched on (-Pbug=..., see the application's build.gradle), and runs the endpoint with
and without the static pre-analysis (http_sa_experiment.py). Writes <out>/campaign.json and
<out>/campaign.md.

    ./http_sa_campaign.py                       # all cases
    ./http_sa_campaign.py --cases pfe-csv-quote t1-d5 --time-limit 300 --search-order coverage
"""

import argparse
import json
import shutil
import subprocess
import sys
from pathlib import Path

import http_sa_experiment as exp

APPS = Path(__file__).resolve().parent
PFE = 'swat-artificial/personal-finance-engine-v2'
PFE_IMPORT = 'POST|/accounts/import?accountId=acc-1&strategy=<string_9>|{"csvPayload":"<string_34>"}'
PFE_IMPORT_ENTRY = 'de.uzl.its.targets.finance_engine.controller.AccountsController#importCsv'

OPTA = 'optaplanner-spring'
OPTA_MAINT = ('GET|/optaplanner/maintenanceSchedulingProblem?weekListSize=<int_0>&crewListSize=<int_1>'
              '&nbOfJobAreas=<int_2>&nbOfJobTargets=<int_3>')
OPTA_MAINT_ENTRY = 'de.uzl.its.targets.optaplanner_spring.controller.OptaplannerEndpoints#maintenanceSchedulingProblem'
# optaplanner itself not instrumented: with it, the SA graph takes minutes and has thousands of
# target points (optaplanner's own asserts)
OPTA_CFG = 'swat-app-only.cfg'

# name: (application dir, bug or None, request template, SA entry, expected verdict[, swat.cfg])
CASES = {
    't1-d5': ('swat-artificial/target-1', None,
              'GET|/e1/depth/5?dateString=<string_10>&hour=<int_11>&weekday=<boolean_12>&token=<string_13>&id=<long_14>',
              'de.uzl.its.targets.artificial.target1.controller.NestedIfs#d5', 'VIOLATION'),
    'pfe-import-nobug': (PFE, 'none', PFE_IMPORT, PFE_IMPORT_ENTRY, 'SAFE'),
    'pfe-csv-quote': (PFE, 'csv-escaped-quote', PFE_IMPORT, PFE_IMPORT_ENTRY, 'VIOLATION'),
    'pfe-header': (PFE, 'header-one-column', PFE_IMPORT, PFE_IMPORT_ENTRY, 'VIOLATION'),
    'pfe-merge': (PFE, 'merge-duplicate', PFE_IMPORT, PFE_IMPORT_ENTRY, 'VIOLATION'),
    'opta-nobug': (OPTA, 'none', OPTA_MAINT, OPTA_MAINT_ENTRY, 'SAFE', OPTA_CFG),
    'opta-validity': (OPTA, 'validity-bound', OPTA_MAINT, OPTA_MAINT_ENTRY, 'VIOLATION', OPTA_CFG),
    'opta-generator': (OPTA, 'generator-tags', OPTA_MAINT, OPTA_MAINT_ENTRY, 'VIOLATION', OPTA_CFG),
    'pfe-filter': (PFE, 'filter-query',
                   'GET|/accounts/acc-1/transactions?minAmount=<long_5>&queryText=<string_7>',
                   'de.uzl.its.targets.finance_engine.controller.AccountsController#getTransactions', 'VIOLATION'),
}


def gradle_project(app_dir: str) -> str:
    return ':targets:applications:' + app_dir.replace('/', ':')


def build(app_dir: str, bug: str | None, dest: Path) -> Path:
    """Builds the application's boot jar (with the bug on) and copies it to dest."""
    cmd = [str(exp.ROOT / 'gradlew'), '-q', f'{gradle_project(app_dir)}:bootJar']
    if bug is not None:
        cmd.append(f'-Pbug={bug}')
    subprocess.run(cmd, cwd=exp.ROOT, check=True)
    jars = [j for j in (APPS / app_dir / 'build' / 'libs').glob('*.jar') if not j.name.endswith('-plain.jar')]
    assert len(jars) == 1, jars
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy(jars[0], dest)
    return dest


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--cases', nargs='+', default=list(CASES), choices=list(CASES))
    p.add_argument('--modes', default='nosa,sa')
    p.add_argument('--out', default=str(APPS / 'http-sa-runs'))
    p.add_argument('--time-limit', type=float, default=300)
    p.add_argument('--search-order', default='dfs')
    p.add_argument('--no-stop-on-violation', action='store_true',
                   help='Keep exploring after the first violation (default: stop there)')
    p.add_argument('--app-port', type=int, default=18080)
    p.add_argument('--explorer-port', type=int, default=18078)
    args = p.parse_args()
    out = Path(args.out)

    results = {}
    results_file = out / 'campaign.json'
    if results_file.exists():
        results = json.loads(results_file.read_text())
    for name in args.cases:
        app_dir, bug, template, entry, expected, *cfg = CASES[name]
        cfg = cfg[0] if cfg else 'swat.cfg'
        jar = build(app_dir, bug, out / 'jars' / f'{name}.jar')
        run_args = argparse.Namespace(
            jar=str(jar), cfg=str(APPS / app_dir / cfg), template=template, sa_entry=entry,
            label=f'{name}_{args.search_order}', out=str(out), time_limit=args.time_limit,
            search_order=args.search_order, stop_on_violation=not args.no_stop_on_violation,
            app_port=args.app_port, explorer_port=args.explorer_port, app_heap='4g',
            python=str(exp.VENV_PYTHON if exp.VENV_PYTHON.exists() else sys.executable))
        classpath = exp.sa_classpath(jar, Path(run_args.cfg), out)
        for mode in args.modes.split(','):
            stats = exp.run(run_args, mode == 'sa', classpath)
            stats['expected'] = expected
            results[f'{name}|{args.search_order}|{mode}|{int(args.time_limit)}'] = stats
            print(exp.summary(name, mode, stats), flush=True)
            results_file.write_text(json.dumps(results, indent=2))
    write_table(results, out / 'campaign.md')


def write_table(results: dict, path: Path):
    rows = ['| case | order | limit (s) | mode | verdict | expected | requests | solver calls | '
            'first violation (s) | wall (s) | SA (s) | desyncs |',
            '|---|---|---|---|---|---|---|---|---|---|---|---|']
    for key, s in sorted(results.items()):
        name, order, mode, limit = key.split('|')
        sa = s.get('sa') or {}
        sa_time = f'{sa["duration_s"]:.1f}' if sa.get('duration_s') is not None else ''
        rows.append(f'| {name} | {order} | {limit} | {mode} | {s.get("verdict")} | {s.get("expected")} | '
                    f'{s.get("requests")} | {s.get("solver_calls")} | {s.get("first_violation_s") or ""} | '
                    f'{s.get("wall_s")} | {sa_time} | {s.get("sa_desyncs", "")} |')
    path.write_text('\n'.join(rows) + '\n')


if __name__ == '__main__':
    main()
