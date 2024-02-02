# File: generate_commands.py

from contextlib import contextmanager
from enum import Enum
import os
import subprocess
import logging

# get the dir of this file
SCRIPT_DIR = os.path.dirname(os.path.realpath(__file__))
BASE_PATH = os.path.realpath(f"{SCRIPT_DIR}/../../")
ARGS_FILE = f"{SCRIPT_DIR}/tests.txt"
LOG_FILE = f"{SCRIPT_DIR}/emulate-svcomp.log"


class ExecutionStatus(Enum):
    OK = 1
    VIOLATION = 2
    DONT_KNOW = 3
    TIMEOUT = 4
    CRASH = 5


def setup_logger(name, log_file, level=logging.INFO):
    formatter = logging.Formatter("%(asctime)s [%(threadName)-12.12s] [%(levelname)-5.5s]  %(message)s")

    fileHandler = logging.FileHandler(log_file)
    fileHandler.setFormatter(formatter)

    consoleHandler = logging.StreamHandler()
    consoleHandler.setFormatter(formatter)
    logger = logging.getLogger(name)
    logger.setLevel(level)
    logger.addHandler(fileHandler)
    # logger.addHandler(consoleHandler)

    return logger


def run_command_with_timeout(cmd: [str], timeout: int = 90) -> (ExecutionStatus, dict):
    """Executes the given command and returns the status and message."""
    try:
        result = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=timeout)
        output = result.stdout.decode('utf-8')
        print(f'{"=" * 50} STDOUT {"=" * 50}')
        print(output)
        print('=' * 113)
        if "== ERROR" in output:
            return ExecutionStatus.VIOLATION, output
        elif "== OK" in output:
            return ExecutionStatus.OK, output
        elif "== DONT-KNOW" in output:
            return ExecutionStatus.DONT_KNOW, output
        else:
            return ExecutionStatus.CRASH, output
    except subprocess.TimeoutExpired:
        return ExecutionStatus.TIMEOUT, ""
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
        
def generate_commands(input_file):
    base_command = ["python3", f"{BASE_PATH}/symbolic-explorer/SymbolicExplorer.py",
                    "-a", f"{BASE_PATH}/symbolic-executor/lib/symbolic-executor.jar",
                    "-c", f"{SCRIPT_DIR}/swat.cfg",
                    "-z3", f"{BASE_PATH}/libs/",#local_z3_installation/lib",
                    "-cp"]
    cmds = []
    verds = []
    with open(input_file, 'r') as f:
        for line in f:
            # Split the line into arguments
            args = line.strip().split()
            program = SCRIPT_DIR + '/' + args[0]
            correct_verdict = args[1]
            arg_base = ['../../sv-benchmarks/java/properties/assert_java.prp', '../../sv-benchmarks/java/common']
            args = arg_base + [program]

            args = [arg.replace("../../sv-benchmarks/java/", SCRIPT_DIR + '/') for arg in args[1:]]
            # Discard the first argument and append the rest to the base command
            full_command = base_command + args
            #print(full_command)
            is_swat = False
            for x in full_command:
                if "swat-regression" in x:
                    is_swat = True
            if not is_swat:
                cmds.append(full_command)
                verds.append(correct_verdict)

    return cmds, verds


if __name__ == "__main__":
    commands, correct_verdicts = generate_commands(ARGS_FILE)
    # delete old log file:
    if os.path.exists(LOG_FILE):
        os.remove(LOG_FILE)
    logger = setup_logger("std", LOG_FILE)
    score = 0
    max_score=0
    scoring_types = {
        'CORRECT_OK': 0,
        'CORRECT_VIOLATION': 0,
        'INCORRECT_OK': 0,
        'INCORRECT_VIOLATION': 0,
        'DONT-KNOW': 0,
        'TIMEOUT': 0,
        'CRASH': 0
    }

    for i, cmd in enumerate(commands):
        path = f'{cmd[-1]}'
        log_dir = f'{path}/logs'
        cmd[-1] = f"{cmd[-1]}/build"
        cmd += ["-d", log_dir]
        #delete the dir if it exists
        if os.path.exists(log_dir):
            os.system(f'rm -rf {log_dir}')
        os.makedirs(log_dir)
        with pushd(path):
            print(f'Running command in {path} --> {cmd}')
            # create folder logs
            status, output = run_command_with_timeout(cmd)
            if status == ExecutionStatus.TIMEOUT:
                scoring_types['TIMEOUT'] += 1
                logger.error(str(cmd[-3]) + ": Timeout")
            elif status == ExecutionStatus.CRASH:
                scoring_types['CRASH'] += 1
                logger.error(str(cmd[-3]) + ": Engine Crashed")
            elif status == ExecutionStatus.DONT_KNOW:
                scoring_types['DONT-KNOW'] += 1
                logger.warning(str(cmd[-3]) + ": No Solution but " + correct_verdicts[i] + " wanted.")
            elif status == ExecutionStatus.VIOLATION:     
                if correct_verdicts[i] == "true":
                    scoring_types['INCORRECT_VIOLATION'] += 1
                    score = score - 16
                    logger.critical(str(cmd[-3]) + ": -16 Expected " + " " + correct_verdicts[i] + ", found " + str(status))     
                elif correct_verdicts[i] == "false":
                    scoring_types['CORRECT_VIOLATION'] += 1
                    score = score + 1
                    logger.info(str(cmd[-3]) + ": +1 Correct Result = " + correct_verdicts[i])
                else:
                    logger.error(str(cmd[-3]) + ": Unexpected verdict " + correct_verdicts[i])
                    exit(1)
            elif status == ExecutionStatus.OK:
                if correct_verdicts[i] == "true":
                    scoring_types['CORRECT_OK'] += 1
                    score = score + 2
                    logger.info(str(cmd[-3]) + ": +2 Correct Result = " + correct_verdicts[i])
                elif correct_verdicts[i] == "false":
                    scoring_types['INCORRECT_OK'] += 1
                    score = score - 32    
                    logger.critical(str(cmd[-3]) + ": -32 Expected " + " " + correct_verdicts[i] + ", found " + str(status))     
                
                else:
                    logger.error(str(cmd[-3]) + ": Unexpected verdict " + correct_verdicts[i])
                    exit(1)
            else: 
                logger.error(str(cmd[-3]) + ": Unexpected status " + str(status))
                exit(1)
        max_score = max_score + 2 if correct_verdicts[i] == "true" else max_score  + 1
        logger.info(f"ROUND: ({i}/{len(commands)}) SCORE: + {str(score)} / {str(max_score)}")
    logger.info(f"FINAL SCORE: {str(score)}")
    logger.info(f"Score distribution: {str(scoring_types)}")


