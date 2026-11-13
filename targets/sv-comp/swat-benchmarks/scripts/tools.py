"""real-tools: harnesses around unmodified open-source tools. The tools' sources are not in the
repository; fetch_tools.sh downloads them from Maven Central into java/real-tools/lib-<tool>/.

Each harness in tools/ has an @COND@ at its assert: the VIOLATION condition is satisfiable, the
SAFE one contradicts what the harness established before (see each entry's reason).
"""

from pathlib import Path

from task import Task

HERE = Path(__file__).resolve().parent

TOOLS = {
    # name: (lib folder, violating inputs, VIOLATION condition, SAFE condition, why SAFE)
    'getopt-minitar': (
        'lib-getopt',
        [('string', '-xv'), ('string', '-f'), ('string', 'x')],
        'optind == argc',
        'optind == argc && create',
        'doExtract() is only called when create is false (exactly one action, and it is not '
        'create or list).'),
    'opencsv-import': (
        'lib-opencsv',
        [('string', ',,')],
        'line.length() < 3',
        'line.length() < 2',
        'three fields need two separators outside quotes, so the line has at least 2 characters.'),
}


def all_tool_tasks():
    for name, (lib, witness, cond_v, cond_s, reason) in TOOLS.items():
        src = (HERE / 'tools' / f'{name}.java').read_text()
        for safe in (True, False):
            yield Task('real-tools', name, safe, src.replace('@COND@', cond_s if safe else cond_v),
                       witness, reason, libs=(lib,))
