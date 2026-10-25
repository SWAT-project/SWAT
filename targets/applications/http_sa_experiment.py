#!/usr/bin/env python3
"""
Runs SWAT's HTTP driver on one endpoint of a Spring Boot application, with and without the static
pre-analysis (SA), and prints what each run found.

Every run starts the instrumented application afresh (so no state carries over between runs), runs
the explorer in http mode against it, and stops the application again. The explorer's log and
http_stats.json are kept in <out>/<label>_<sa|nosa>/.

The SA graph is built from the application's own classes (BOOT-INF/classes of the boot jar) and the
library jars the executor instruments (swat.cfg's includePackages): with all of Spring's jars the
extractor takes minutes. Calls into the other libraries are leaves of the graph either way; what is
lost is telling application methods that override their methods apart (callbacks).

Example:
    ./http_sa_experiment.py --jar swat-artificial/target-1/build/libs/target-1-0.0.1-SNAPSHOT.jar \\
        --cfg swat-artificial/target-1/swat.cfg \\
        --template 'GET|/e1/depth/2?a=<string_1>&b=<int_2>' \\
        --sa-entry de.uzl.its.targets.artificial.target1.controller.NestedIfs#d2 --label d2
"""

import argparse
import json
import os
import signal
import subprocess
import sys
import time
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]  # the SWAT repository
EXPLORER = ROOT / 'symbolic-explorer' / 'SymbolicExplorer.py'
AGENT = ROOT / 'symbolic-executor' / 'lib' / 'symbolic-executor.jar'
Z3_DIR = ROOT / 'libs' / 'java-library-path'
CFG_EXTRACTION = ROOT.parent / 'cfg-extraction'
VENV_PYTHON = ROOT / 'targets' / 'sv-comp' / 'scripts' / '.venv' / 'bin' / 'python3'


def instrumented_packages(cfg: Path) -> list[str]:
    """The packages swat.cfg instruments (instrumentation.includePackages), as path prefixes."""
    for line in cfg.read_text().splitlines():
        if line.strip().startswith('instrumentation.includePackages='):
            return [p.strip() for p in line.split('=', 1)[1].split(':') if p.strip()]
    return []


def sa_classpath(jar: Path, cfg: Path, out: Path) -> list[Path]:
    """
    The classpath for the SA graph: the application's classes from the Spring Boot jar
    (BOOT-INF/classes), plus the library jars of BOOT-INF/lib with classes in a package the
    executor instruments (e.g. gson for the gson endpoints), all extracted under out.
    """
    dest = out / 'classes' / jar.stem
    marker = dest / '.extracted'
    packages = instrumented_packages(cfg)
    if not marker.exists() or marker.stat().st_mtime < jar.stat().st_mtime:
        with zipfile.ZipFile(jar) as z:
            for name in z.namelist():
                if name.startswith('BOOT-INF/classes/') and name.endswith('.class'):
                    z.extract(name, dest)
                elif name.startswith('BOOT-INF/lib/') and name.endswith('.jar'):
                    z.extract(name, dest)
        marker.touch()
    classpath = [dest / 'BOOT-INF' / 'classes']
    for lib in sorted((dest / 'BOOT-INF' / 'lib').glob('*.jar')):
        with zipfile.ZipFile(lib) as z:
            if any(n.startswith(p) for n in z.namelist() for p in packages):
                classpath.append(lib)
    return classpath


def start_app(args, logfile: Path) -> subprocess.Popen:
    cmd = ['java', f'-Xmx{args.app_heap}', f'-Dconfig.path={Path(args.cfg).resolve()}',
           f'-Dexplorer.port={args.explorer_port}', '-Dsolver.mode=HTTP',
           f'-Djava.library.path={Z3_DIR}', f'-javaagent:{AGENT}', '-ea',
           '-jar', str(Path(args.jar).resolve()), f'--server.port={args.app_port}']
    log = open(logfile, 'w')
    proc = subprocess.Popen(cmd, stdout=log, stderr=subprocess.STDOUT, cwd=logfile.parent,
                            start_new_session=True)
    deadline = time.time() + 120
    while time.time() < deadline:
        if proc.poll() is not None:
            raise RuntimeError(f'the application exited with {proc.returncode}, see {logfile}')
        if 'Started ' in logfile.read_text(errors='replace'):
            return proc
        time.sleep(0.5)
    stop(proc)
    raise RuntimeError(f'the application did not start within 120 s, see {logfile}')


def stop(proc: subprocess.Popen):
    if proc.poll() is None:
        os.killpg(proc.pid, signal.SIGKILL)
    proc.wait()


def run(args, with_sa: bool, classpath: list[Path]) -> dict:
    run_dir = Path(args.out) / f'{args.label}_{"sa" if with_sa else "nosa"}'
    run_dir.mkdir(parents=True, exist_ok=True)
    for f in ('http_stats.json', 'explorer.log'):
        (run_dir / f).unlink(missing_ok=True)
    app = start_app(args, run_dir / 'app.log')
    try:
        cmd = [str(args.python), str(EXPLORER), '-m', 'http', '--http-port', str(args.app_port),
               '-u', args.template, '-p', str(args.explorer_port), '-l', str(run_dir),
               '--time-limit', str(args.time_limit), '--search-order', args.search_order]
        if args.stop_on_violation:
            cmd.append('--stop-on-violation')
        if with_sa:
            cmd += ['--sa-path', str(CFG_EXTRACTION), '--sa-entry', args.sa_entry, '-cp', *map(str, classpath)]
        with open(run_dir / 'explorer.out', 'w') as out:
            # The explorer ends itself with SIGTERM once it has a verdict.
            subprocess.run(cmd, stdout=out, stderr=subprocess.STDOUT, cwd=ROOT / 'symbolic-explorer',
                           timeout=args.time_limit + 300)
    except subprocess.TimeoutExpired:
        pass
    finally:
        stop(app)
    stats_file = run_dir / 'http_stats.json'
    stats = json.loads(stats_file.read_text()) if stats_file.exists() else {'verdict': 'NO_STATS'}
    stats['run_dir'] = str(run_dir)
    return stats


def summary(label: str, mode: str, s: dict) -> str:
    sa = s.get('sa') or {}
    return (f'{label:24} {mode:5} verdict={s.get("verdict"):10} requests={s.get("requests")} '
            f'solver_calls={s.get("solver_calls")} first_violation={s.get("first_violation_s")}s'
            f'@{s.get("first_violation_request")} wall={s.get("wall_s")}s desyncs={s.get("sa_desyncs")} '
            f'sa={sa.get("status")}')


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--jar', required=True, help='The Spring Boot jar of the application')
    p.add_argument('--cfg', required=True, help="The application's swat.cfg")
    p.add_argument('--template', required=True, help='METHOD|URL[|BODY] with symbolic placeholders, see HTTPDriver')
    p.add_argument('--sa-entry', required=True, help='The endpoint method, as pkg.Class#method')
    p.add_argument('--label', default='run')
    p.add_argument('--modes', default='nosa,sa', help='Comma-separated: nosa, sa')
    p.add_argument('--out', default=str(ROOT / 'targets' / 'applications' / 'http-sa-runs'))
    p.add_argument('--time-limit', type=float, default=300)
    p.add_argument('--search-order', default='dfs')
    p.add_argument('--stop-on-violation', action='store_true')
    p.add_argument('--app-port', type=int, default=18080)
    p.add_argument('--explorer-port', type=int, default=18078)
    p.add_argument('--app-heap', default='4g')
    p.add_argument('--python', default=str(VENV_PYTHON if VENV_PYTHON.exists() else sys.executable))
    args = p.parse_args()

    args.out = str(Path(args.out).resolve())
    classpath = sa_classpath(Path(args.jar).resolve(), Path(args.cfg).resolve(), Path(args.out))
    print(f'SA classpath: {" ".join(p.name for p in classpath)}', flush=True)
    results = {}
    for mode in args.modes.split(','):
        results[mode] = run(args, mode == 'sa', classpath)
        print(summary(args.label, mode, results[mode]), flush=True)
    with open(Path(args.out) / f'{args.label}_summary.json', 'w') as f:
        json.dump(results, f, indent=2)


if __name__ == '__main__':
    main()
