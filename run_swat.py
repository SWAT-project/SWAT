from contextlib import contextmanager
import logging, os, enum, subprocess, sys, base64, uuid

import concurrent.futures, json, datetime
from typing import List

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)
SCRIPT_DIR = os.path.dirname(os.path.realpath(__file__))
WITNESS_PATH = os.path.join(SCRIPT_DIR, 'WitnessCreator/build/libs/WitnessCreator.jar')


class ExecutionStatus(enum.Enum):
    SUCCESS = "success"
    ERROR = "error"
    TIMEOUT = "timeout"

class Verdict(enum.Enum):
    VIOLATION = "== ERROR"
    SAFE = "== OK"
    UNKNOWN = "== DONT-KNOW"
    NO_SYMBOLIC_VARS = "== NON-SYMBOLIC"

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

    def determine_result(output: List[str]) -> Verdict:
        verdict = None
        for line in output:
            if "[VERDICT] == OK" in line:
                verdict = Verdict.SAFE
                break
            elif "[VERDICT] == ERROR" in line:
                verdict = Verdict.VIOLATION
                break
            elif "[VERDICT] == DONT-KNOW" in line:
                verdict = Verdict.UNKNOWN
                break
        return verdict






def run_command_with_timeout(cmd: list[str], timeout: int = 180) -> tuple[ExecutionStatus, list[str]]:
    """Executes the given command and returns output from both STDOUT and STDERR."""

    logger.info(f'[TARGET EXECUTION]: Running command: {cmd}')
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





def generate_command(args):
    config_file = 'sv-comp.cfg'

    agent_path = os.path.join(SCRIPT_DIR, 'symbolic-executor', 'lib', 'symbolic-executor.jar')
    config_path = os.path.join(SCRIPT_DIR, config_file)
    library_path = os.path.join(SCRIPT_DIR, 'libs', 'java-library-path')
    logging_dir =  os.path.join(SCRIPT_DIR, 'logs')

    python3_path = os.path.join(SCRIPT_DIR, '.venv_ubuntu_24_04_1__x86_64/bin/python3')

    base_command = [ python3_path, "-u", os.path.join(SCRIPT_DIR, 'symbolic-explorer', 'SymbolicExplorer.py'),
                     "--agent", agent_path,
                     "--config", config_path,
                     "-z3", library_path,
                     "--logdir", logging_dir,
                     "--mode", "sv-comp",
                     '--port', "8000",
                     "--classpath"]
    print("CP args: " + str(args))
    for arg in args[1:]:
        print("CP arg: " + arg)
        print("CP arg path: " + os.path.join(SCRIPT_DIR, arg))
        base_command.append(os.path.join(SCRIPT_DIR, arg))
    return base_command


def copy_target(target_dir):
    # generate random target tmp name
    local_target_dir = f"target_{uuid.uuid4()}"

    os.system(f'rm -rf {local_target_dir} && cp -r {target_dir} {local_target_dir}')
    print(f"Target copied from {target_dir} to: {local_target_dir}")
    return local_target_dir

def compile_target(args, local_target_dir):
    cmd = [
        'bash',
        f'compile-target.sh',
    ]
    args[-1] = local_target_dir # overwrite target dir
    cmd = cmd + args
    print(cmd)
    _, output = run_command_with_timeout(cmd)
    log_output(output)


def extract_last_round(out, marker="============================== ROUND"):

    return "\n ".join(out).split(marker)[-1]


def extract_markers(out, marker):
    res = []
    for line in out.split("\n"):
        print(line)
        if marker in line:
            res.append(line)
    return res

def generate_witness(witness):
    witness_dir = os.path.join(SCRIPT_DIR, 'WitnessCreator/')
    with pushd(witness_dir):
        # base64 encode the string
        enc = base64.b64encode(witness.encode())
        cmd = [
            'java',
            '-jar',
            WITNESS_PATH,
            enc,
            witness_dir
        ]
        print(cmd)
        _, output = run_command_with_timeout(cmd)
        print("WITNESS CREATED")
        print(output)

def log_output(output: List[str]):
    for line in output:
        logger.info(line.strip())

if __name__ == "__main__":
    logger.info("Running SWAT Wrapper...")
    cmd = generate_command(sys.argv[1:])
    local_target_dir = copy_target(sys.argv[-1])
    compile_target(sys.argv[1:], local_target_dir)

    path = local_target_dir
    log_dir = f'{path}/logs'

    cmd[-1] = os.path.join(SCRIPT_DIR, local_target_dir, 'build') #f"{local_target_dir}/build"
    #delete the dir if it exists
    if os.path.exists(log_dir):
        os.system(f'rm -rf {log_dir}')
    os.makedirs(log_dir)
    with pushd(path):
        execution_status, output = run_command_with_timeout(cmd)
        log_output(output)
        print(f"Execution status: {execution_status}")
        if execution_status == ExecutionStatus.TIMEOUT or execution_status == ExecutionStatus.ERROR:
            print(f'Error: {output}')
            exit(1)


        out = extract_last_round(output)
        verdict = extract_markers(out, '[VERDICT]')
        if "== ERROR" in str(verdict):
            witnesses = extract_markers(out, '[WITNESS]')
            generate_witness("\n".join(witnesses))
            print(f'Witnesses:\n {witnesses}')
        print(f'Verdict:\n {verdict}')