import base64
from contextlib import contextmanager
import os
import enum
import logging
import shutil
import subprocess
import time
from typing import List, Optional, Tuple
from pathlib import Path
from .dtypes import Verdict, ExpectedVerdict, VerificationCategory, VerificationTask, Command

logger = logging.getLogger(__name__)
SCRIPT_DIR = Path(__file__).resolve().parent.parent  # Now in lib/, so go up one more level
WITNESS_CREATION_PATH = (SCRIPT_DIR.parent / 'WitnessCreator' / 'build' / 'libs' / 'WitnessCreator.jar')
WITNESS_VALIDATION_PATH = (SCRIPT_DIR.parent / 'wit4java' / 'bin' / 'wit4java')

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
    witnesses = extract_markers(out, "[WITNESS]")
    if not witnesses:
        logger.debug("No witnesses found in output")
        return None, 0.0, 0.0

    witness_dir = assemble_files(info)

    logger.info("Generating witness files")
    generation_start = time.perf_counter()
    generate_witness(witness_dir, "\n".join(witnesses))
    generation_time = time.perf_counter() - generation_start

    logger.info("Validating witness")
    validation_start = time.perf_counter()
    validation_result = validate_witness(info)
    validation_time = time.perf_counter() - validation_start

    return validation_result, generation_time, validation_time


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



def extract_markers(out: str, marker: str) -> List[str]:
    res: List[str] = []
    for line in out.split("\n"):
        logger.debug(line)
        if marker in line:
            res.append(line)
    return res

def generate_witness(witness_dir: str, witness: str) -> None:
    with pushd(str(SCRIPT_DIR.parent / 'WitnessCreator')):
        # base64 encode the string
        enc = base64.b64encode(witness.encode()).decode()
        cmd = [
            'java',
            '-jar',
            str(WITNESS_CREATION_PATH),
            enc,
            str(witness_dir)
        ]
        logger.info(f"Running witness creator: {cmd}")
        _, output = run_command_with_timeout(cmd)
        logger.info("Witness created")
        log_output(output)

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


def assemble_files(info: dict) -> str:
    witness_dir = Path(info['log_dir']) / 'witness'
    witness_dir.mkdir(parents=True, exist_ok=True)

    try:
        classpath_index = info['command'].index('--classpath')
        classpath_entries = info['command'][classpath_index + 1:]
    except ValueError:
        classpath_entries = []

    for folder in classpath_entries:
        folder_path = Path(folder)
        if not folder_path.exists():
            continue
        for src in folder_path.rglob('*.java'):
            # Preserve relative path structure (package directories) for WitnessCreator
            rel_path = src.relative_to(folder_path)
            dst = witness_dir / rel_path
            dst.parent.mkdir(parents=True, exist_ok=True)
            logger.info(f"Copying {src} to {dst}")
            shutil.copy(str(src), str(dst))

    return str(witness_dir)
    
    