from .selection import extract_testcases
import logging
import socket
from pathlib import Path
from pprint import pformat
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


def generate_command(ver_task: VerificationTask, logging_dir: Path, port: int=8087, config_file:str = 'swat.cfg') -> list[str]:


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

    cp: list[str] = []
    for input_file in ver_task['input_files']:
        cp.append(str((test_case_dir / input_file).resolve()))
        cp.append(str(BASE_PATH / 'libs' / 'java-library-path' / 'com.microsoft.z3.jar'))
    return base_command + cp 




def generate_commands(ver_tasks: list[VerificationTask], config_file: str = 'swat.cfg') -> list[VerificationTask]:

    port = 9000
    skipped_ports = []

    # Determine log directory base based on config file
    log_dir_base = 'logs-debug' if 'debug' in config_file.lower() else 'logs'

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
        logging_dir = SCRIPT_DIR / '..' / log_dir_base / rel_target_path / f"{target_name}_{category_suffix}"
        command: Command = {
            'target_dir': target_dir,
            'target': target,
            'log_dir': logging_dir,
            'command': generate_command(ver_task, logging_dir, port=port, config_file=config_file)
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