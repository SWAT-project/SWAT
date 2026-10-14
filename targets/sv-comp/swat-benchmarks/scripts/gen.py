#!/usr/bin/env python3
"""Generates the swat-benchmarks tasks (sources and .yml files) into ../java/.

    ./gen.py                 # all suites
    ./gen.py dispatcher ...  # only these suites

Generated suite folders are replaced as a whole, so stale tasks do not linger.
"""

import shutil
import sys
from pathlib import Path

from families import all_family_tasks
from soundness import all_soundness_tasks

JAVA_DIR = Path(__file__).resolve().parent.parent / 'java'


def main(suites):
    tasks = [*all_family_tasks(), *all_soundness_tasks()]
    if suites:
        tasks = [t for t in tasks if t.suite in suites]
    for suite in sorted({t.suite for t in tasks}):
        shutil.rmtree(JAVA_DIR / suite, ignore_errors=True)
    for t in tasks:
        t.write(JAVA_DIR)
    print(f'Wrote {len(tasks)} tasks into {len({t.suite for t in tasks})} suites.')


if __name__ == '__main__':
    main(sys.argv[1:])
