"""Validation of the witness files the Symbolic Explorer writes for violation verdicts.

The explorer writes `witness.yml` (format 2.3) and `witness.graphml` to `<log_dir>/witness/` via the
`witnesscreator` package. This module only checks them:

- YAML: WitnessLint, if `SWAT_WITNESSLINT` names a linter command (e.g.
  `"/path/venv/bin/python /path/witnesslint/witnesslinter.py"`); WitnessLint does not check for a
  target segment, so the number of `target` waypoints is checked here (exactly one is expected).
- GraphML: wit4java (the result is what `target_execution` reports as `validated`).

A proper validator setup is tracked in #47.
"""
from contextlib import contextmanager
import os
import enum
import logging
import re
import shlex
import subprocess
import time
from typing import List, Optional, Tuple
from pathlib import Path
from .dtypes import VerificationTask

logger = logging.getLogger(__name__)
SCRIPT_DIR = Path(__file__).resolve().parent.parent  # Now in lib/, so go up one more level
WITNESS_VALIDATION_PATH = (SCRIPT_DIR.parent / 'wit4java' / 'bin' / 'wit4java')
WITNESSLINT_ENV = 'SWAT_WITNESSLINT'
WITNESS_DIR_NAME = 'witness'
TARGET_WAYPOINT = re.compile(r'^\s*type:\s*["\']?target["\']?\s*$')

class ExecutionStatus(enum.Enum):
    SUCCESS = "success"
    ERROR = "error"
    TIMEOUT = "timeout"


def witness_dir(ver_task: VerificationTask) -> Path:
    """The directory the explorer writes the witness files of the task to."""
    command_dict = ver_task.get('command') or {}
    return Path(str(command_dict.get('log_dir', SCRIPT_DIR))) / WITNESS_DIR_NAME


def validate_witness(ver_task: VerificationTask) -> Tuple[Optional[bool], float]:
    """Check the witness files the explorer wrote for a violation verdict.

    Returns (validation_result, validation_time):
        - validation_result: wit4java's verdict on witness.graphml (True/False), or None if no
          witness was written
        - validation_time: time spent validating (seconds)
    """
    command_dict = ver_task.get('command', {})
    info = {
        'command': command_dict.get('command', []),
        'log_dir': str(command_dict.get('log_dir', SCRIPT_DIR))
    }
    directory = witness_dir(ver_task)
    yaml_file = directory / 'witness.yml'
    graphml_file = directory / 'witness.graphml'
    if not yaml_file.exists() and not graphml_file.exists():
        logger.warning(f"No witness files found in {directory}")
        return None, 0.0

    logger.info("Validating witness")
    validation_start = time.perf_counter()
    check_yaml_witness(yaml_file, input_dirs(info))
    validation_result = validate_graphml_witness(info, graphml_file) if graphml_file.exists() else None
    validation_time = time.perf_counter() - validation_start

    return validation_result, validation_time


def input_dirs(info: dict) -> List[str]:
    """The task's source directories (classpath entries that are directories)."""
    try:
        classpath_index = info['command'].index('--classpath')
        classpath_entries = info['command'][classpath_index + 1:]
    except ValueError:
        classpath_entries = []
    return [folder for folder in classpath_entries if Path(folder).is_dir()]


def property_file(ver_task: VerificationTask) -> Path:
    """The property file of the task, e.g. sv-benchmarks/java/properties/valid-assert.prp."""
    return ver_task['file_path'].parent.parent / 'properties' / ver_task['category'].value


def log_output(output: List[str]):
    for line in output:
        logger.info(line.strip())


def validate_graphml_witness(info: dict, witness_file: Path) -> bool:
    """Run wit4java on the GraphML witness and return True if it reports the witness as correct."""
    cmd: List[str] = [str(WITNESS_VALIDATION_PATH), '--packages']
    main_folder = ''

    # find classpath entries (items after --classpath)
    try:
        classpath_index = info['command'].index('--classpath')
        classpath_entries = info['command'][classpath_index + 1:]
    except ValueError:
        classpath_entries = []

    for folder in classpath_entries:
        try:
            if 'Main.java' in os.listdir(folder):
                main_folder = folder
            else:
                cmd.append(folder)
        except Exception:
            # ignore non-folders or missing paths
            continue

    cmd.extend(['--witness', str(witness_file)])
    if main_folder:
        cmd.append(main_folder)

    _, output = run_command_with_timeout(cmd)
    logger.info("Witness Validation Output")
    log_output(output)
    for line in output:
        if "wit4java: Witness Correct" in line:
            return True
    return False


@contextmanager
def pushd(dirname):
    """Context manager to temporarily change the working directory."""
    original_dir = os.getcwd()
    logger.info(f"Changing directory to: {dirname}")
    os.chdir(dirname)
    try:
        yield
    finally:
        os.chdir(original_dir)


def count_target_waypoints(witness_file: Path) -> int:
    """The number of `target` waypoints in a YAML witness (a violation witness needs exactly one)."""
    with open(witness_file, encoding='utf-8') as f:
        return sum(1 for line in f if TARGET_WAYPOINT.match(line))


def check_yaml_witness(witness_file: Path, inputs: List[str]) -> Optional[bool]:
    """Check the YAML witness: exactly one target waypoint, and WitnessLint if configured.

    Returns True iff all performed checks passed, False if one failed, None if there is no file.
    """
    if not witness_file.exists():
        logger.warning(f"No YAML witness at {witness_file}")
        return None
    targets = count_target_waypoints(witness_file)
    ok = targets == 1
    logger.info(f"YAML witness target waypoints: {targets} ({'ok' if ok else 'expected exactly one'}) ({witness_file})")
    lint = lint_witness(witness_file, inputs)
    if lint is False:
        ok = False
    return ok


def lint_witness(witness_file: Path, inputs: List[str]) -> Optional[bool]:
    """Run WitnessLint on the YAML witness if SWAT_WITNESSLINT is set; True iff the witness is well-formed."""
    linter = os.environ.get(WITNESSLINT_ENV)
    if not linter or not witness_file.exists():
        return None
    cmd = shlex.split(linter) + ['--expectViolationWitness', '--expectedWitnessVersion', '2',
                                 '--witness', str(witness_file)] + inputs
    status, output = run_command_with_timeout(cmd)
    log_output(output)
    ok = status == ExecutionStatus.SUCCESS and any('finished with exit code 0' in line for line in output)
    logger.info(f"WitnessLint: {'well-formed' if ok else 'FAULTY'} ({witness_file})")
    return ok

def run_command_with_timeout(cmd: list[str], timeout: int = 180) -> tuple[ExecutionStatus, list[str]]:
    """Executes the given command and returns output from both STDOUT and STDERR."""

    logger.info(f'[WITNESS] Running: {cmd}')
    output = []
    with subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            universal_newlines=True,
            bufsize=1
    ) as proc:
        try:
            # Read output and wait for process to finish, with a timeout
            stdout, _ = proc.communicate(timeout=timeout)
            output = stdout.splitlines()

            return ExecutionStatus.SUCCESS, output


        except subprocess.TimeoutExpired:
            proc.kill()
            stdout, _ = proc.communicate()
            output = stdout.splitlines()
            return ExecutionStatus.TIMEOUT, output

        except Exception as e:
            logger.critical(f'[SVCOMP] Exception: {e}')
            proc.kill()
            output = [str(e)]
            return ExecutionStatus.ERROR, output + [str(e)]


