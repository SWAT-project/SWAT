#!/usr/bin/env python3
"""Checks the invariant of class preparation over SV-COMP benchmark tasks.

For every distinct task classpath (the ``input_files`` of each task's ``.yml``):

1. ``Prepare --no-mark`` writes the rewritten classes;
2. the agent loads every class of the original classpath (``LoadClasses``) and dumps what its
   load-time rewrites produce (``-Dswat.dumpRewritten``);
3. both must name the same classes, with identical bytes;
4. ``Prepare`` (marked) again, and the agent loads the prepared classpath: it must rewrite nothing
   (a prepared class is never rewritten twice).

Usage (from SWAT/): scripts/check-prepare.py [--workers N] [--limit N] <benchmark dir>...
Needs symbolic-executor/lib/symbolic-executor.jar (./gradlew :symbolic-executor:copyJar).
"""
import argparse
import concurrent.futures
import glob
import hashlib
import os
import shutil
import subprocess
import sys
import tempfile

import yaml

SWAT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAR = os.path.join(SWAT, 'symbolic-executor', 'lib', 'symbolic-executor.jar')
CONFIG = os.path.join(SWAT, 'targets', 'sv-comp', 'sv-comp.cfg')
NATIVE = os.path.join(SWAT, 'libs', 'java-library-path')


def classpaths(bench_dirs):
    seen = {}
    for d in bench_dirs:
        for yml in sorted(glob.glob(os.path.join(d, '**', '*.yml'), recursive=True)):
            try:
                with open(yml) as f:
                    task = yaml.safe_load(f)
            except Exception:
                continue
            if not isinstance(task, dict) or 'input_files' not in task:
                continue
            base = os.path.dirname(yml)
            cp = tuple(os.path.normpath(os.path.join(base, p)) for p in task['input_files'])
            seen.setdefault(cp, yml)
    return seen


def classes(root):
    out = {}
    for dirpath, _, files in os.walk(root):
        for f in files:
            if f.endswith('.class'):
                p = os.path.join(dirpath, f)
                out[os.path.relpath(p, root)] = p
    return out


def read(p):
    with open(p, 'rb') as f:
        return f.read()


def run(cmd):
    r = subprocess.run(cmd, capture_output=True, text=True, timeout=600)
    if r.returncode != 0:
        raise RuntimeError(f'{cmd[-3:]} failed ({r.returncode}): {r.stderr[-2000:]}{r.stdout[-2000:]}')
    return r.stdout


def prepare(out, cp, mark=True):
    return run(['java', f'-Dconfig.path={CONFIG}', '-cp', JAR, 'de.uzl.its.swat.prepare.Prepare',
                *([] if mark else ['--no-mark']), out, *cp])


def load(cp, dump):
    return run(['java', f'-javaagent:{JAR}', f'-Djava.library.path={NATIVE}', f'-Dconfig.path={CONFIG}',
                f'-Dswat.dumpRewritten={dump}', '-cp', ':'.join(cp),
                'de.uzl.its.swat.prepare.LoadClasses', *cp])


def check(cp):
    """Problems found for one classpath (empty if the invariant holds)."""
    problems = []
    tmp = tempfile.mkdtemp(prefix='check-prepare-')
    try:
        a, b, c, d = (os.path.join(tmp, x) for x in 'abcd')
        prepare(a, cp, mark=False)
        load(list(cp), b)
        # Prepare's output per class name (the first entry that has it), and which of them differ
        # from the original
        prepared, changed = {}, set()
        for i, entry in enumerate(cp):
            if not os.path.isdir(entry):
                continue
            orig = classes(entry)
            for rel, p in sorted(classes(os.path.join(a, str(i))).items()):
                name = rel[:-6]
                if name in prepared:
                    continue
                prepared[name] = p
                if rel in orig and read(orig[rel]) != read(p):
                    changed.add(name)
        dumped = {rel[:-6]: p for rel, p in classes(b).items()}
        for name in sorted(set(dumped) | changed):
            if name not in dumped:
                problems.append(f'{name}: changed by Prepare, but the agent did not rewrite it')
            elif name not in prepared:
                problems.append(f'{name}: rewritten by the agent, but not in the prepared classpath')
            elif read(prepared[name]) != read(dumped[name]):
                problems.append(f'{name}: prepared bytes differ from the agent\'s '
                                f'({hashlib.sha1(read(prepared[name])).hexdigest()[:8]} vs '
                                f'{hashlib.sha1(read(dumped[name])).hexdigest()[:8]})')
        rewritten = dumped
        # Prepared and marked: the agent must not rewrite anything again
        import json
        prepared_cp = json.loads(prepare(c, cp).strip().splitlines()[-1])['classpath']
        load(prepared_cp, d)
        again = sorted(classes(d))
        if again:
            problems.append(f'rewritten again although prepared: {again[:5]}')
        if not rewritten and not problems:
            problems.append('nothing was rewritten (unexpected for a task)')
    except Exception as e:  # noqa: BLE001
        problems.append(f'error: {e}')
    finally:
        shutil.rmtree(tmp, ignore_errors=True)
    return problems


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('bench_dirs', nargs='+')
    ap.add_argument('--workers', type=int, default=8)
    ap.add_argument('--limit', type=int, default=0)
    args = ap.parse_args()
    cps = classpaths(args.bench_dirs)
    items = list(cps.items())[:args.limit or None]
    print(f'{len(items)} distinct classpaths', flush=True)
    failed = 0
    with concurrent.futures.ThreadPoolExecutor(args.workers) as ex:
        for (cp, yml), problems in zip(items, ex.map(lambda it: check(it[0]), items)):
            if problems:
                failed += 1
                print(f'FAIL {yml}')
                for p in problems[:10]:
                    print(f'    {p}')
    print(f'{len(items) - failed} ok, {failed} failed')
    sys.exit(1 if failed else 0)


if __name__ == '__main__':
    main()
