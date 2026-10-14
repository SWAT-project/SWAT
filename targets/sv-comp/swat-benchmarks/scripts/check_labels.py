#!/usr/bin/env python3
"""Checks every task's label by running it (with -ea) on its witness inputs, through a stub Verifier
that replays them (stub/). A VIOLATION task must fail an assert; a SAFE task must not fail on its
paired VIOLATION task's inputs (the minimal difference is what makes it safe; that it is safe for
all inputs is argued in the task's header comment). Also checks that all inputs 0, SWAT's first
round, reach no assert, so every violation needs at least one flipped branch.

    ./check_labels.py [suite...]     (run ./build.sh first)
"""

import re
import subprocess
import sys
import tempfile
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

HERE = Path(__file__).resolve().parent
JAVA_DIR = HERE.parent / 'java'


def run(task_dir: Path, stub: Path, inputs: str) -> str:
    cmd = ['java', '-ea', f'-Dinputs={inputs}', '-cp', f'{stub}:{JAVA_DIR / "common"}:{task_dir}', 'Main']
    p = subprocess.run(cmd, capture_output=True, text=True, timeout=60)
    if 'java.lang.AssertionError' in p.stderr:
        return 'assert'
    return 'ok' if p.returncode == 0 else f'exit {p.returncode}: {p.stderr.strip()[-300:]}'


def check(task_dir: Path, stub: Path) -> str | None:
    src = (task_dir / 'Main.java').read_text()
    m = re.search(r'(?:Violating inputs \(nondet call order\)|Paired VIOLATION inputs \(must not fail here\)): (.*)', src)
    inputs = ','.join(m.group(1).split())
    safe = task_dir.name.endswith('_true')
    exits = 'System.exit' in src  # the gauntlet's exit variant ends the program on rejection
    got = run(task_dir, stub, inputs)
    if got != ('ok' if safe else 'assert'):
        return f'{task_dir.relative_to(JAVA_DIR)}: witness {inputs} gives {got}'
    zero = run(task_dir, stub, '')
    if zero != 'ok' and not (exits and zero.startswith('exit 1')):
        return f'{task_dir.relative_to(JAVA_DIR)}: all-zero inputs give {zero}'
    return None


def main(suites):
    with tempfile.TemporaryDirectory() as tmp:
        stub = Path(tmp)
        subprocess.run(['javac', '-nowarn', '-d', str(stub), '-cp', str(JAVA_DIR / 'common'),
                        str(HERE / 'stub/org/sosy_lab/sv_benchmarks/Verifier.java')], check=True)
        dirs = sorted(d for d in JAVA_DIR.glob('*/*') if (d / 'Main.java').exists()
                      and (not suites or d.parent.name in suites))
        with ThreadPoolExecutor(32) as pool:
            problems = [p for p in pool.map(lambda d: check(d, stub), dirs) if p]
    for p in problems:
        print(p)
    print(f'{len(dirs) - len(problems)}/{len(dirs)} labels confirmed.')
    return 1 if problems else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
