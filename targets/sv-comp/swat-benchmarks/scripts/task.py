"""A generated verification task and how it is written to disk."""

from dataclasses import dataclass
from pathlib import Path

YML = '''format_version: '2.0'
input_files:
  - ../common/
  - {name}/
properties:
  - property_file: ../properties/valid-assert.prp
    expected_verdict: {verdict}
options:
  language: Java
'''


def ints(values):
    return [('int', v) for v in values]


@dataclass
class Task:
    suite: str
    base: str          # task name without the _true/_false suffix
    safe: bool
    source: str        # Main.java
    # The inputs of a violating run, in the order the Verifier.nondet* calls happen, as
    # (type, value) pairs. For a SAFE task: the paired VIOLATION task's inputs, which must not
    # fail here (the minimal difference is what makes the task safe).
    witness: list
    safe_reason: str   # why the SAFE variant is safe
    extra_files: dict = None  # further sources next to Main.java: {file name: text}

    @property
    def name(self) -> str:
        return f'{self.base}_{"true" if self.safe else "false"}'

    def header(self) -> str:
        values = ' '.join(f'{t}:{v}' for t, v in self.witness)
        if self.safe:
            return (f'// Expected verdict: SAFE. {self.safe_reason}\n'
                    f'// Paired VIOLATION inputs (must not fail here): {values}\n')
        return f'// Expected verdict: VIOLATION. Violating inputs (nondet call order): {values}\n'

    def write(self, java_dir: Path) -> Path:
        suite_dir = java_dir / self.suite
        task_dir = suite_dir / self.name
        task_dir.mkdir(parents=True, exist_ok=True)
        (task_dir / 'Main.java').write_text(self.header() + self.source)
        for fname, text in (self.extra_files or {}).items():
            (task_dir / fname).write_text(text)
        (suite_dir / f'{self.name}.yml').write_text(
            YML.format(name=self.name, verdict='true' if self.safe else 'false'))
        return task_dir
