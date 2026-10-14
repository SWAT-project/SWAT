#!/usr/bin/env python3
"""Generates the swat-benchmarks tasks (sources and .yml files) into ../java/.

    ./gen.py                 # all suites
    ./gen.py dispatcher ...  # only these suites

Generated suite folders are emptied first (except lib-* folders), so stale tasks do not linger.
"""

import shutil
import sys
from pathlib import Path

from families import all_family_tasks
from soundness import all_soundness_tasks
from tools import all_tool_tasks

JAVA_DIR = Path(__file__).resolve().parent.parent / 'java'


def main(suites):
    tasks = [*all_family_tasks(), *all_soundness_tasks(), *all_tool_tasks()]
    if suites:
        tasks = [t for t in tasks if t.suite in suites]
    for suite in sorted({t.suite for t in tasks}):
        # Keep the libraries fetch_tools.sh put there.
        for entry in (JAVA_DIR / suite).glob('*'):
            if not entry.name.startswith('lib-'):
                shutil.rmtree(entry) if entry.is_dir() else entry.unlink()
    for t in tasks:
        t.write(JAVA_DIR)
    print(f'Wrote {len(tasks)} tasks into {len({t.suite for t in tasks})} suites.')


if __name__ == '__main__':
    main(sys.argv[1:])
