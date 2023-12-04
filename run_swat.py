# File: generate_commands.py

import base64
from contextlib import contextmanager
from enum import Enum
import os
import subprocess
import sys
import uuid

# get the dir of this file
SCRIPT_DIR = os.path.dirname(os.path.realpath(__file__))

BASE_PATH = f'{SCRIPT_DIR}/'
SWAT_PATH = f'{BASE_PATH}knife-fuzzer/'
SV_COMP_PATH = f'{SWAT_PATH}targets/sv-comp/'
WITNESS_PATH = f'{BASE_PATH}WitnessCreator/build/libs/WitnessCreator.jar'

class ExecutionStatus(Enum):
    SUCCESS = 1
    ERROR = 2
    TIMEOUT = 3
    CRASH = 4
    VIOLATION = 5

def run_command_with_timeout(cmd: [str]) -> (ExecutionStatus, dict):
        """Executes the given command and returns the status and message."""
        try:
            debug = False
            if debug:
                result = subprocess.run(cmd)
                output = None
            else:
                result = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
                output = {
                    'stdout': result.stdout.decode('utf-8') + result.stderr.decode('utf-8')
                }   
            print(f'{"=" * 50} Command Output {"=" * 50}')
            print(output['stdout'])
            print('=' * 113)
            if result.returncode == 0:
                return ExecutionStatus.SUCCESS, output
            elif "java.lang.AssertionError" in output['stdout']:
                return ExecutionStatus.VIOLATION, output
            else:
                return ExecutionStatus.ERROR, output
        except subprocess.TimeoutExpired:
            return ExecutionStatus.TIMEOUT, "Error - Timeout"
        except Exception as e:
            return ExecutionStatus.CRASH, str(e)


@contextmanager
def pushd(dirname):
    """Context manager to temporarily change the working directory."""
    original_dir = os.getcwd()
    os.chdir(dirname)
    try:
        yield
    finally:
        os.chdir(original_dir)
        
def generate_command(args):
    base_command = [f"{SWAT_PATH}/symbolic-explorer/venv/bin/python3.10", f"{SWAT_PATH}symbolic-explorer/SymbolicExplorer.py",
                    "-a", f"{SWAT_PATH}symbolic-executor/lib/symbolic-executor.jar",
                    "-c", f"{SV_COMP_PATH}swat.cfg",
                    "-z3", f"{BASE_PATH}local_z3_installation/lib",
                    "-cp"]
    # Split the line into arguments
    args = args[1:]
    # Discard the first argument and append the rest to the base command
    full_command = base_command + args
    return full_command

def compile_target(args, local_target_dir):
    cmd = [
        'bash',
        f'{SV_COMP_PATH}compile-target.sh',
    ]
    args[-1] = local_target_dir # overwrite target dir
    cmd = cmd + args
    print(cmd)
    run_command_with_timeout(cmd)


def extract_markers(out, marker):
    res = []
    for line in out.split("\n"):
        print(line)
        if marker in line:
            res.append(line)
    return res
    
def generate_witness(witness):
    with pushd(f'{BASE_PATH}WitnessCreator/'):
        # base64 encode the string
        enc = base64.b64encode(witness.encode())
        cmd = [
                'java',
                '-jar',
                WITNESS_PATH,
                enc
            ]
        print(cmd)
        run_command_with_timeout(cmd)

    # copy the witness to the target dir
    os.system(f'cp {BASE_PATH}WitnessCreator/witness.graphml .')
    os.system('cat witness.graphml')
        
    
def extract_last_round(out, marker="[ROUND BEGIN]"):
    return out.split(marker)[-1]
    
def copy_target(target_dir):
    # generate random target tmp name
    local_target_dir = f"target_{uuid.uuid4()}"

    os.system(f'rm -rf {local_target_dir} && cp -r {target_dir} {local_target_dir}')
    return local_target_dir
    
if __name__ == "__main__":
    cmd = generate_command(sys.argv[1:])
    local_target_dir = copy_target(sys.argv[-1])
    compile_target(sys.argv[1:], local_target_dir)

    path = local_target_dir
    log_dir = f'{path}/logs'
    cmd[-1] = f"{local_target_dir}/build"
    cmd += ["-d", log_dir]
    #delete the dir if it exists
    if os.path.exists(log_dir):
        os.system(f'rm -rf {log_dir}')
    os.makedirs(log_dir)
    with pushd(path):
        print(f'Running command in {path} --> {cmd}')
        # create folder logs
        status, output = run_command_with_timeout(cmd)
        if status == ExecutionStatus.TIMEOUT or status == ExecutionStatus.CRASH:
            print(f'Error: {output}')
            exit(1)
        
        out = extract_last_round(output['stdout'])
        witnesses = extract_markers(out, '[WITNESS]')
        verdict = extract_markers(out, '[VERDICT]')
        generate_witness("\n".join(witnesses))
        print(f'Witnesses:\n {witnesses}')
        print(f'Verdict:\n {verdict}')
