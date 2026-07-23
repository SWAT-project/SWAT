from .selection import extract_testcases
import datetime
import logging
import socket
from pathlib import Path
from pprint import pformat
from typing import Optional
from .dtypes import Command, VerificationTask

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

SCRIPT_DIR = Path(__file__).resolve().parent.parent  # Now in lib/, so go up one more level
BASE_PATH = SCRIPT_DIR.parents[2]
PYENV_PATH = SCRIPT_DIR / '.venv' / 'bin' / 'python3'


def is_port_available(port: int) -> bool:
    """
    Check if a port is available for binding.

    Args:
        port: The port number to check

    Returns:
        True if the port is available, False if occupied
    """
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        sock.bind(('127.0.0.1', port))
        sock.close()
        return True
    except OSError:
        return False


def generate_command(ver_task: VerificationTask, logging_dir: Path, port: int=8087, config_file:str = 'swat.cfg',
                     target_id: Optional[Path] = None) -> list[str]:


    test_case_dir = ver_task['file_path'].parent
    agent_path = BASE_PATH / 'symbolic-executor' / 'lib' / 'symbolic-executor.jar'
    config_path = SCRIPT_DIR / '..' / config_file
    library_path = BASE_PATH / 'libs' / 'java-library-path'

    base_command: list[str] = [str(PYENV_PATH), "-u", str(BASE_PATH / 'symbolic-explorer' / 'SymbolicExplorer.py'),
                    "-prp", ver_task['category'].value,
                    "--agent", str(agent_path),
                    "--config", str(config_path),
                    "-z3", str(library_path),
                    "--logdir", str(logging_dir),
                    "--mode", "sv-comp",
                    '--port', str(port),
                    "--classpath"]
    if target_id is not None:
        base_command[-1:-1] = ["--target-id", target_id.as_posix()]

    cp: list[str] = []
    for input_file in ver_task['input_files']:
        cp.append(str((test_case_dir / input_file).resolve()))
        cp.append(str(BASE_PATH / 'libs' / 'java-library-path' / 'com.microsoft.z3.jar'))
    return base_command + cp 




def new_run_timestamp() -> str:
    """Returns a timestamp identifying a single run, shared across all its outputs."""
    return datetime.datetime.now().strftime("%Y%m%d_%H%M%S")


def run_dir(run_timestamp: str) -> Path:
    """The directory holding a non-debug run's logs/ and results/."""
    return SCRIPT_DIR / '..' / 'runs' / f"run_{run_timestamp}"


def generate_commands(ver_tasks: list[VerificationTask], config_file: str = 'swat.cfg', run_timestamp: Optional[str] = None) -> list[VerificationTask]:

    port = 9000
    skipped_ports = []

    # A single timestamp ties a run's per-testcase logs to its results.
    if run_timestamp is None:
        run_timestamp = new_run_timestamp()

    # Debug runs keep a timestamped history per target; normal runs share one run dir.
    is_debug = 'debug' in config_file.lower()

    for ver_task in ver_tasks:
        # Find next available port
        while not is_port_available(port):
            skipped_ports.append(port)
            port += 1

            # Safety check: don't go beyond reasonable port range
            if port > 65535:
                raise RuntimeError("Ran out of available ports! Too many ports occupied.")

        # Extract unique identifier for the target (based on its relative path and the name of the .yml file)
        target_dir = ver_task['file_path'].parent

        # Get relative path from sv-benchmarks/java/
        sv_benchmarks_java = Path('sv-benchmarks') / 'java'
        rel_target_path = target_dir.relative_to(target_dir.parents[len(target_dir.parts) - list(target_dir.parts).index('sv-benchmarks') - 1] / sv_benchmarks_java)

        target_name = ver_task['file_path'].stem
        target = rel_target_path / target_name

        # Include category in log dir to avoid collisions when same file has multiple properties
        category_suffix = ver_task['category'].value.replace('.prp', '')
        testcase = f"{target_name}_{category_suffix}"
        if is_debug:
            # runs-debug/<rel>/<testcase>/run_<ts>/logs — grouped per target so a debug
            # session keeps the history of its runs together.
            logging_dir = SCRIPT_DIR / '..' / 'runs-debug' / rel_target_path / testcase / f"run_{run_timestamp}" / 'logs'
        else:
            # runs/run_<ts>/logs/<rel>/<testcase> — all testcases of a run share one run dir.
            logging_dir = run_dir(run_timestamp) / 'logs' / rel_target_path / testcase
        command: Command = {
            'target_dir': target_dir,
            'target': target,
            'log_dir': logging_dir,
            'command': generate_command(ver_task, logging_dir, port=port, config_file=config_file, target_id=target)
        }
        ver_task['command'] = command
        port += 1

    if skipped_ports:
        logger.warning(f"Skipped {len(skipped_ports)} occupied ports during command generation: {skipped_ports[:10]}{'...' if len(skipped_ports) > 10 else ''}")

    return ver_tasks
    
    
if __name__ == "__main__":
    logger.info("Generating commands...")
    task_dir = SCRIPT_DIR.parent / 'sv-benchmarks'
    logger.info(f"Base directory: {task_dir}")
    ver_tasks: list[VerificationTask] = extract_testcases(task_dir)
    logger.info(f"Extracted {len(ver_tasks)} test cases.")
    commands = generate_commands(ver_tasks)
    logger.info(f"Generated {len(commands)} commands.")
    logger.info("\nFirst 3 commands:")
    for i, command in enumerate(commands[:3], 1):
        logger.info(f"\nCommand {i}:\n{pformat(dict(command), width=100)}")
