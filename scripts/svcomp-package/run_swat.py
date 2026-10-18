from contextlib import contextmanager
import logging, os, enum, shutil, subprocess, sys, uuid

import concurrent.futures, json, datetime
from typing import List, Optional

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)
SCRIPT_DIR = os.path.dirname(os.path.realpath(__file__))
ORIGINAL_CWD = os.getcwd()
# The explorer writes the violation witness (witness.yml, witness.graphml) here; BenchExec collects
# it via the `**/witness.*` result-files pattern of the benchmark definition.
WITNESS_DIR = os.path.join(SCRIPT_DIR, 'witness')


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

def determine_result(output: List[str], property_file: str) -> Verdict:
    """Parse the verdict for a specific property from the output."""
    verdict = None
    for line in output:
        if f"[VERDICT {property_file}]" in line:
            if "== OK" in line:
                verdict = Verdict.SAFE
                break
            elif "== ERROR" in line:
                verdict = Verdict.VIOLATION
                break
            elif "== DONT-KNOW" in line:
                verdict = Verdict.UNKNOWN
                break
    return verdict






def run_command_with_timeout(cmd: list[str], timeout: int | None = None) -> tuple[ExecutionStatus, list[str]]:
    """Executes the given command and returns output from both STDOUT and STDERR.

    timeout=None (the default) imposes no internal cutoff so SWAT can use the full
    SV-COMP time budget; benchexec enforces the wall/CPU limit externally.
    """

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





def generate_command(args, property_file: str, property_path: str, input_paths: List[str]):
    """The explorer command; `property_path` and `input_paths` are the verifier's arguments as given
    by BenchExec (relative to the original working directory) and end up in the witness."""
    config_file = 'sv-comp.cfg'

    agent_path = os.path.join(SCRIPT_DIR, 'symbolic-executor', 'lib', 'symbolic-executor.jar')
    config_path = os.path.join(SCRIPT_DIR, config_file)
    library_path = os.path.join(SCRIPT_DIR, 'libs', 'java-library-path')
    logging_dir =  os.path.join(SCRIPT_DIR, 'logs')

    # Same interpreter (and PYTHONPATH, via the inherited environment) as run-swat.sh set up.
    base_command = [ sys.executable, "-u", os.path.join(SCRIPT_DIR, 'symbolic-explorer', 'SymbolicExplorer.py'),
                     "-prp", property_file,
                     "--agent", agent_path,
                     "--config", config_path,
                     "-z3", library_path,
                     "--logdir", logging_dir,
                     "--mode", "sv-comp",
                     '--port', "8000",
                     "--witness-dir", WITNESS_DIR,
                     "--witness-property", property_path,
                     "--witness-base-dir", ORIGINAL_CWD,
                     "--producer-version", swat_version(),
                     "--witness-input", *input_paths,
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


def extract_markers(out, markers):
    if isinstance(markers, str):
        markers = (markers,)
    res = []
    for line in out.split("\n"):
        if any(marker in line for marker in markers):
            res.append(line)
    return res

def swat_version():
    """The package version from BUILD_INFO.txt, written by scripts/package-svcomp.sh."""
    try:
        with open(os.path.join(SCRIPT_DIR, 'BUILD_INFO.txt')) as f:
            for line in f:
                if line.startswith('version='):
                    return line[len('version='):].strip()
    except OSError:
        pass
    return 'unknown'

def print_witness():
    """Print the witness file contents for validation on the benchmark server."""
    witness_file_path = os.path.join(WITNESS_DIR, 'witness.yml')
    if os.path.exists(witness_file_path):
        print("\n" + "="*80)
        print("WITNESS FILE CONTENTS:")
        print("="*80)
        with open(witness_file_path, 'r') as f:
            print(f.read())
        print("="*80)
        print("END OF WITNESS FILE")
        print("="*80 + "\n")
    else:
        print(f"WARNING: Witness file not found at {witness_file_path}")

def log_output(output: List[str]):
    for line in output:
        logger.info(line.strip())

if __name__ == "__main__":
    logger.info("Running SWAT Wrapper...")

    # Look for property file in arguments
    # SV-COMP framework passes the .prp file directly as first argument
    property_file = None
    property_path = None
    for arg in sys.argv[1:]:
        if arg.endswith('.prp'):
            property_file = os.path.basename(arg)
            property_path = arg
            logger.info(f"Found property file in arguments: {property_file}")
            break

    if property_file is None:
        logger.error(f"ERROR: No .prp file found in arguments.")
        logger.error(f"Arguments: {sys.argv}")
        exit(1)

    logger.info(f"Using property file: {property_file}")

    # The task's input files/directories, as given (everything but the property file)
    input_paths = [arg for arg in sys.argv[1:] if arg != property_path]

    target_dir = sys.argv[-1]
    cmd = generate_command(sys.argv[1:], property_file, property_path, input_paths)
    # Exactly one witness per run: drop whatever an earlier run left behind
    shutil.rmtree(WITNESS_DIR, ignore_errors=True)
    local_target_dir = copy_target(target_dir)
    compile_target(sys.argv[1:], local_target_dir)

    path = local_target_dir
    log_dir = f'{path}/logs'

    cmd[-1] = os.path.join(SCRIPT_DIR, local_target_dir, 'build') #f"{local_target_dir}/build"
    # The CPU time limit covers the whole run, so the explorer's budget starts with what copying
    # and compiling the task took (the memory and CPU limits are the explorer's defaults).
    t = os.times()
    cmd[cmd.index('--classpath'):cmd.index('--classpath')] = [
        '--cpu-used', f'{t.user + t.system + t.children_user + t.children_system:.1f}']
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
        verdict = extract_markers(out, f'[VERDICT {property_file}]')
        if "== ERROR" in str(verdict):
            # The explorer wrote the witness from the trace of the violating execution
            print_witness()
        print(f'Verdict:\n {verdict}')