import base64
from contextlib import contextmanager
import os
import enum
import logging
import shlex
import subprocess
import time
from typing import List, Optional, Tuple
from pathlib import Path
from .dtypes import Verdict, ExpectedVerdict, VerificationCategory, VerificationTask, Command

logger = logging.getLogger(__name__)
SCRIPT_DIR = Path(__file__).resolve().parent.parent  # Now in lib/, so go up one more level
WITNESS_CREATION_PATH = (SCRIPT_DIR.parent / 'WitnessCreator' / 'build' / 'libs' / 'WitnessCreator.jar')
WITNESS_VALIDATION_PATH = (SCRIPT_DIR.parent / 'wit4java' / 'bin' / 'wit4java')
# Output lines carrying witness information: nondet values and the violating location
WITNESS_MARKERS = ("[WITNESS]", "[WITNESS_TARGET]")
# Optional: command prefix of WitnessLint (e.g. "/path/venv/bin/python /path/witnesslinter.py");
# when set, the YAML witness is linted after creation. Proper validator setup is tracked in #47.
WITNESSLINT_ENV = 'SWAT_WITNESSLINT'

class ExecutionStatus(enum.Enum):
    SUCCESS = "success"
    ERROR = "error"
    TIMEOUT = "timeout"



def generate_and_validate_witness(ver_task: VerificationTask, output: List[str]) -> Tuple[Optional[bool], float, float]:
    """Extract witnesses from `output`, create witness files and validate them.

    Returns tuple of (validation_result, generation_time, validation_time):
        - validation_result: True/False on validation result or None if no witness was found
        - generation_time: Time spent generating witness (seconds)
        - validation_time: Time spent validating witness (seconds)
    """
    # Build an `info` dict compatible with the legacy implementation
    command_dict = ver_task.get('command', {})
    info = {
        'command': command_dict.get('command', []),
        'log_dir': str(command_dict.get('log_dir', SCRIPT_DIR))
    }

    out = extract_last_round(output)
    witnesses = extract_markers(out, WITNESS_MARKERS)
    if not witnesses:
        logger.debug("No witnesses found in output")
        return None, 0.0, 0.0

    witness_dir = Path(info['log_dir']) / 'witness'
    witness_dir.mkdir(parents=True, exist_ok=True)

    logger.info("Generating witness files")
    generation_start = time.perf_counter()
    generate_witness(witness_dir, "\n".join(witnesses), input_dirs(info), property_file(ver_task))
    generation_time = time.perf_counter() - generation_start

    logger.info("Validating witness")
    validation_start = time.perf_counter()
    lint_witness(witness_dir / 'witness.yml', input_dirs(info))
    validation_result = validate_witness(info)
    validation_time = time.perf_counter() - validation_start

    return validation_result, generation_time, validation_time


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
        
def validate_witness(info: dict) -> bool:
    """Run the external witness validator and return True if witness is correct."""
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

    cmd.extend(['--witness', str(Path(info['log_dir']) / 'witness' / 'witness.graphml')])
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



def extract_last_round(out: List[str], marker: str = "============================== ROUND") -> str:
    return "\n".join(out).split(marker)[-1]



def extract_markers(out: str, markers) -> List[str]:
    if isinstance(markers, str):
        markers = (markers,)
    res: List[str] = []
    for line in out.split("\n"):
        logger.debug(line)
        if any(marker in line for marker in markers):
            res.append(line)
    return res

def generate_witness(witness_dir: Path, witness: str, inputs: List[str], prp: Path) -> None:
    """Create witness.yml (format 2.3) and witness.graphml in `witness_dir` from the marker lines."""
    with pushd(str(SCRIPT_DIR.parent / 'WitnessCreator')):
        # base64 encode the string
        enc = base64.b64encode(witness.encode()).decode()
        cmd = [
            'java',
            '-jar',
            str(WITNESS_CREATION_PATH),
            enc,
            '--out', str(witness_dir),
            '--property', str(prp),
            '--producer-version', 'local',
        ]
        for folder in inputs:
            cmd.extend(['--input', folder])
        logger.info(f"Running witness creator: {cmd}")
        _, output = run_command_with_timeout(cmd)
        logger.info("Witness created")
        log_output(output)

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


