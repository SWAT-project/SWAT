

from contextlib import contextmanager
from enum import Enum
import os
import subprocess
import numpy as np
import sys
import logging as logger
logger.basicConfig(stream=sys.stdout, level=logger.DEBUG)
# get the dir of this file
SCRIPT_DIR = os.path.dirname(os.path.realpath(__file__))
BASE_PATH = os.path.realpath(f"{SCRIPT_DIR}/../../")
ARGS_FILE = f"{SCRIPT_DIR}/tests.txt"

class ExecutionStatus(Enum):
    SUCCESS = 1
    ERROR = 2
    TIMEOUT = 3
    CRASH = 4
    VIOLATION = 5



def run_command_with_timeout(cmd: [str], timeout: int = 900) -> (ExecutionStatus, dict):
    """Executes the given command and returns the status and message."""
    try:
        logger.info(f'Running symbolic-explorer: {cmd}')
        stdout = []
        #with subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, bufsize=1, universal_newlines=True) as proc:
        with subprocess.Popen(cmd, bufsize=1, universal_newlines=True) as proc:
            proc.wait(timeout=timeout)
            for line in proc.stdout:
                
                logger.info(f'[SYMBOLIC EXPLORER]: ' + line.replace("\n", ""))
                stdout.append(line)

        if proc.returncode == 0:
            return ExecutionStatus.SUCCESS, stdout
        elif "java.lang.AssertionError" in stdout:
            return ExecutionStatus.VIOLATION, stdout
        else:
            return ExecutionStatus.ERROR, stdout
    except subprocess.TimeoutExpired:
        return ExecutionStatus.TIMEOUT, stdout
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
        
def generate_command(input_file, pattern, use_debug_config):
    conf = 'swat-debug.cfg' if use_debug_config else 'swat.cfg'
    base_command = [ "python3", "-u", f"{BASE_PATH}/symbolic-explorer/SymbolicExplorer.py",
                    "-a", f"{BASE_PATH}/symbolic-executor/lib/symbolic-executor.jar",
                    "-c", f"{SCRIPT_DIR}/{conf}",
                    "-z3", f"{BASE_PATH}/libs/",
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
            args = [arg.replace("../../sv-benchmarks/java/", SCRIPT_DIR + '/') for arg in args[1:]]
            
            args = arg_base + [program]

            args = [arg.replace("../../sv-benchmarks/java/", SCRIPT_DIR + '/') for arg in args[1:]]
            
            # Discard the first argument and append the rest to the base command
            full_command = base_command + args
            #print(full_command)
            
            if np.sum([pattern in arg for arg in full_command]) == 0:
                continue
            cmds.append(full_command)
            verds.append(correct_verdict)
            
        if len(cmds) == 0:
            raise ValueError(f'No command found for pattern {pattern}')
        elif len(cmds) > 1:
            cmds = [cmds[0]]
            #raise ValueError(f'Multiple commands found for pattern {pattern}')
        return cmds
    

if __name__ == "__main__":
    # =========== Toggle logging ============
    use_debug_config = True
    # ============= Select case =============
    #target = 'swat-regression/loop01'
    #target = 'jdart-regression/double2long'
    #target = 'algorithms/BinaryTreeSearch-MemUnsat01'
    #target = 'jbmc-regression/StringContains01'
    #target = 'juliet-java/CWE369_Divide_by_Zero__float_connect_tcp_divide_01_bad' <-- not working yet
    #target = 'juliet-java/CWE369_Divide_by_Zero__float_connect_tcp_divide_81a_bad_version2'
    #target = 'securibench/Basic4' #<-- not working yet
    #target = 'algorithms/BellmanFord-FunSat02'#<-- not working yet: MULTINEWARRAY
    target = 'swat-regression/MULTINEWARRAY01'#<-- not working yet: MULTINEWARRAY
    target = 'algorithms/Tsp-FunSat01'#<-- not working yet: MULTINEWARRAY
    #target = 'algorithms/MergeSortIterative-FunUnsat01' #<-- unknown now: ArrIdxOOB uncaught
    #target = 'jayhorn-recursive/UnsatAddition02' #<-- unknown now: Timeout (ok)
    #target = 'jayhorn-recursive/UnsatAckermann01' #<-- unknown now: Timeout (ok: unlimited recursion)
    #target = 'jayhorn-recursive/UnsatFibonacci01' # <-- works
    #target = 'jayhorn-recursive/UnsatFibonacci02' # <-- works
    #target = 'securibench/Basic20' # <-- works
    #target = 'java-ranger-regression/TCAS_prop1'
    #target = 'swat-regression/MULTINEWARRAY01'
    target = 'jdart-regression/shifting3' # <-- works!
    #target = 'jdart-regression/shifting2' # <-- works!
    #target = 'securibench/Basic4' # <-- works!
    #target = 'jdart-regression/addition01' # <-- works, but not enough time!
    #target = 'java-ranger-regression/TCAS_prop1' # <-- Timeout
    
    
    # DONT-KNOW section - violation expected
    target = 'jbmc-regression/RegexMatches02' # <-- invocation (remains issue)
    target = 'jbmc-regression/NullPointerException3' # <-- Works: GETFIELD on Null threw exception
    target = 'jbmc-regression/StringBuilderChars05' # <-- Not Working: no symbolic handling for setCharAt + no bounds checking on the index
    target = 'jbmc-regression/TokenTest02' # invocation (remains issue) java/lang/String/split
    target = 'jbmc-regression/StringValueOf09' # invocation remains issue) java/lang/Double/parseDouble + no alphanumerical check
    target = 'jbmc-regression/StringValueOf08' # invocation remains issue) java/lang/Float/parseFloat + no alphanumerical check
    target = 'jbmc-regression/SubString02' # <-- missing bounds check for index
    target = 'jbmc-regression/SubString03' # <-- missing bounds check for index
    target = 'jbmc-regression/StaticCharMethods02' # <-- missing invocation: java/lang/Character/toLowerCase
    target = 'jbmc-regression/StaticCharMethods05' # <-- scanner.nextInt of Scanner(SymbolicString) doesnt work
    target = 'jbmc-regression/StringValueOf03' # <-- missing bounds check for String.charAt + likely missing invocation: String.valueOf(charArray, 3, 3);
    target = 'jbmc-regression/StringValueOf02'   # <-- missing bounds check for String.charAt + likely missing invocation: String.valueOf(charArray);
    target = 'jbmc-regression/StringCompare02' # <-- missing invocation: java/lang/String/regionMatches
    target = 'jdart-regression/radians' # <-- missing invocation: java/lang/Math/toRadians
    target = 'algorithms/BinaryTreeSearch-MemUnsat02' # <-- Works: GETFIELD on Null threw exception
    target = 'algorithms/RedBlackTree-FunSat01' # <-- Max recursion depth python reached --> DONT-KNOW (?)
    target = 'algorithms/RedBlackTree-FunUnsat01' # <-- Works in Emulate: Issue with argument ordering!
    target = 'algorithms/RedBlackTree-MemSat01' # <-- Max recursion depth python reached --> DONT-KNOW (?)
    target = 'algorithms/RedBlackTree-MemUnsat01'# <-- Max recursion depth python reached --> DONT-KNOW (?)
    target = 'algorithms/Tsp-MemUnsat01' # <-- Exception regarding MULTIANEWARRAY
    target = 'algorithms/Trie-MemUnsat01' # <-- Works: GETFIELD on Null threw exception
    target = 'algorithms/InsertionSort-MemUnsat01' # <-- Works, issue unclear
    target = 'algorithms/MergeSortIterative-FunUnsat01' # <-- Actual ArrIdxOOB exception (some handling?)
    target = 'jayhorn-recursive/UnsatAckermann01' # <-- Unlimited recursion
    target = 'securibench/Sanitizers5' # <-- missing invocation: URL.decode/ encode
    target = 'securibench/Basic7' # <-- missing invocation: String.toLowerCase + various StringBuffer methods
    target = 'securibench/Arrays9' # <-- missing MULTIANEWARRAY handling for String arrays
    target = 'securibench/Basic15' # <-- missing invocation: java/lang/StringBuffer/toString
    target = 'securibench/Basic35' # <-- unlimited loop, but potentially correct handling --> needs loop limit
    target = 'securibench/Collections13'# <-- missing invocation: misc array stuff
    target = 'securibench/Arrays10' # <-- missing MULTIANEWARRAY handling for String arrays (Still issue (ObjectarrayValue))
    target = 'securibench/Aliasing5'# missing invocation: java/lang/StringBuffer/toString + append
    target = 'java-ranger-regression/LoopCharAt' # <-- would need to hit loop iter 121, else timeout
    target = 'java-ranger-regression/printtokens_eqchk/prop2' #<-- works in emulation: weird paths encountered
    target = 'java-ranger-regression/nanoxml_eqchk/prop2'# <-- missing invocation: java/io/PushbackReader/read + java/lang/StringBuffer/toString + weird path
    target = 'java-ranger-regression/nanoxml_eqchk/prop3' # <-- missing invocation: java/io/PushbackReader/read + weird path
    target = 'java-ranger-regression/WBS/prop3' # <-- Works!
    target = 'java-ranger-regression/WBS/prop1' # <-- Works!
    target = 'java-ranger-regression/WBS/prop4' # <-- Timeout
    target = 'java-ranger-regression/siena_eqchk/prop2' # <-- Inverstigate !
    target = 'java-ranger-regression/replace5_eqchk/prop2' # <-- Timeout
    target = 'MinePump/spec1-5_product1' # <-- works
    target = 'MinePump/spec1-5_product49' # <-- missing invocation: MinePumpSystem/Environment$WaterLevelEnum/ordinal ! investigate as its a custom function
    target = 'MinePump/spec1-5_product56' # <-- missing invocation: MinePumpSystem/Environment$WaterLevelEnum/ordinal ! investigate as its a custom function
    target = 'MinePump/spec1-5_product54' # <-- unknown
    target = 'MinePump/spec1-5_product55' # <-- unknown
    target = 'MinePump/spec1-5_product50' # <-- unknown
    target = 'MinePump/spec1-5_product52' # <-- unknown
    target = 'MinePump/spec1-5_product53' # <-- unknown
    target = 'jpf-regression/ExException_false' # <-- ToDo: here we again need constraints on uncaught exceptions
    target = 'rtems-lock-model/lock-00-01-10' # <-- not compiled?

    
    # DONT-KNOW section - no violation expected
    target = 'java-ranger-regression/printtokens_eqchk/prop1' # <-- Timeout likely
    target = 'java-ranger-regression/nanoxml_eqchk/prop1' # <-- missing invocation: java/io/PushbackReader/read + weird path
    target = 'java-ranger-regression/WBS/prop2' # <-- Timeout
    target = 'java-ranger-regression/siena_eqchk/prop1' # <-- max recursion issue
    target = 'java-ranger-regression/replace5_eqchk/prop1' # <-- timeout
    target = 'jbmc-regression/RegexMatches01' # <-- works, was no symbolic var but invocation issues
    target = 'jbmc-regression/NullPointerException1' # <-- works, was no symbolic var but invocation issues
    target = 'jbmc-regression/bitwise1' # <-- cannot solve sat formula
    target = 'jbmc-regression/TokenTest01' # <-- works, was no symbolic var but invocation issues
    target = 'jbmc-regression/StringBuilderAppend01' # <-- works, was no symbolic var but invocation issues
    target = 'jbmc-regression/RegexSubstitution01' # <-- works, was no symbolic var but invocation issues
    target = 'jbmc-regression/RegexSubstitution03' # <-- works, was no symbolic var but invocation issues
    target = 'jbmc-regression/StringBuilderChars01' # <-- works, was no symbolic var but invocation issues
    target = 'jbmc-regression/StaticCharMethods01' # <-- works, was no symbolic var but invocation issues
    target = 'jbmc-regression/enum1' # <-- works, was no symbolic var but invocation issues
    target = 'jbmc-regression/calc' # <-- missing invocation java/lang/Integer/parseInt   (easy)
    target = 'jbmc-regression/StaticCharMethods06' # <-- missing invocation java/lang/Character/equals  (easy)
    target = 'jbmc-regression/StringValueOf01' # <-- works, was no symbolic var but invocation issues
    target = 'jbmc-regression/Validate01' # <-- works, was no symbolic var but invocation issues
    target = 'jbmc-regression/StringBuilderInsertDelete01' # <-- works, was no symbolic var but invocation issues
    target = 'jbmc-regression/charArray' # <--java/lang/String/toCharArray  (potentially easy?)
    target = 'jbmc-regression/CharSequenceToString' # <-- missing invocation java/lang/CharSequence/toString  (easy)
    target = 'jbmc-regression/StringMiscellaneous04' # <-- works, was no symbolic var but invocation issues
    target = 'jbmc-regression/StringCompare01' # <-- works, was no symbolic var but invocation issues
    target = 'jbmc-regression/StringIndexMethods01' # <-- works, was no symbolic var but invocation issues
    target = 'jdart-regression/URLDecoder01' # <-- missing invocation java/net/URLDecoder/decode 
    target = 'algorithms/RedBlackTree-MemSat01' # <-- ret = copy.deepcopy(self.tree[endpoint_id]) silently crashes!
    target = 'algorithms/RedBlackTree-FunSat01' # <-- likely same fate as previous
    target = 'jayhorn-recursive/SatPrimes01' # <-- Timeout now
    target = 'securibench/Basic6' # <-- missing invocation java/lang/String/toLowerCase
    target = 'securibench/Collections9' # <-- some exception in target: java.lang.IndexOutOfBoundsException
    target = 'securibench/Collections8' # <-- missing invocation java/util/ArrayList/addAll
    target = 'securibench/Sanitizers3' # <-- missing invocations
    target = 'securibench/StrongUpdates5' # <-- exception in target
    target = 'securibench/Basic8' # <--missing invocation java/util/BitSet/get
    target = 'java-ranger-regression/apachecli_eqchk' # <-- MULTINEWARRAY type C required
    target = 'java-ranger-regression/infusion/prop8' # <-- timeout
    target = 'java-ranger-regression/infusion/prop9' # <-- timeout
    target = 'java-ranger-regression/infusion/prop10' # <-- likely timeout (untested)
    #target = 'java-ranger-regression/infusion/prop11' # <-- likely timeout (untested)
    #target = 'java-ranger-regression/infusion/prop12' # <-- likely timeout (untested)
    #target = 'java-ranger-regression/infusion/prop13' # <-- likely timeout (untested)
    #target = 'java-ranger-regression/infusion/prop1' # <-- likely timeout (untested)
    #target = 'java-ranger-regression/infusion/prop2' # <-- likely timeout (untested)
    #target = 'java-ranger-regression/infusion/prop3' # <-- likely timeout (untested)
    #target = 'java-ranger-regression/infusion/prop4' # <-- likely timeout (untested)
    #target = 'java-ranger-regression/infusion/prop5' # <-- likely timeout (untested)
    #target = 'java-ranger-regression/infusion/prop6' # <-- likely timeout (untested)
    #target = 'java-ranger-regression/infusion/prop7' # <-- likely timeout (untested)
    target = 'java-ranger-regression/alarm/prop1' # <-- timeout
    #target = 'java-ranger-regression/alarm/prop2' # <-- likely timeout (untested)
    #target = 'java-ranger-regression/alarm/prop3' # <-- likely timeout (untested)
    #target = 'java-ranger-regression/alarm/prop4' # <-- likely timeout (untested)
    #target = 'java-ranger-regression/alarm/prop5' # <-- likely timeout (untested)
    #target = 'java-ranger-regression/alarm/prop6' # <-- likely timeout (untested)
    #target = 'java-ranger-regression/alarm/prop8' # <-- likely timeout (untested)
    #target = 'java-ranger-regression/alarm/prop9' # <-- likely timeout (untested)
    target = 'MinePump/spec1-5_product57' # <-- missing invocation (user defined function): MinePumpSystem/Environment$WaterLevelEnum/ordinal
    target = 'MinePump/spec1-5_product58' # <-- missing invocation (user defined function): MinePumpSystem/Environment$WaterLevelEnum/ordinal
    target = 'MinePump/spec1-5_product59' # <-- missing invocation (user defined function): MinePumpSystem/Environment$WaterLevelEnum/ordinal
    target = 'MinePump/spec1-5_product60' # <-- missing invocation (user defined function): MinePumpSystem/Environment$WaterLevelEnum/ordinal
    target = 'MinePump/spec1-5_product61' # <--
    #target = 'MinePump/spec1-5_product62' # <--
    #target = 'MinePump/spec1-5_product63' # <--
    #target = 'MinePump/spec1-5_product64' # <--
    target = 'jpf-regression/ExSymExeComplexMath_true' # <-- works

    target = 'java-ranger-regression/WBS/prop1'
    #target = 'juliet-java/CWE369_Divide_by_Zero__float_connect_tcp_divide_81a_bad'
    
    
    target = 'swat-regression/MULTINEWARRAY01'#<-- working!
    #target = 'swat-regression/MULTINEWARRAY02'#<-- timeout
    #target = 'swat-regression/MULTINEWARRAY03'#<-- working!
    #target = 'swat-regression/MULTINEWARRAY04'#<-- working!
    #target = 'swat-regression/MULTINEWARRAY05'#<-- working!
    #target = 'swat-regression/IASTORE05'#<--working!
    #target = 'java-ranger-regression/apachecli_eqchk' # <-- MULTINEWARRAY type C required

    # target = 'algorithms/Tsp-MemUnsat01' <-- Some Excpetion in engine

    
    #target = 'jbmc-regression/tableswitch1' # <-- invocation (remains issue)
    #target = 'jbmc-regression/StringValueOf01'
    # =======================================
    
    commands = generate_command(ARGS_FILE, target,use_debug_config)
    for cmd in commands:
        path = f'{cmd[-1]}'
        log_dir = f'{path}/logs'
        cmd[-1] = f"{cmd[-1]}/build"
        cmd += ["-d", log_dir]
        #delete the dir if it exists
        if os.path.exists(log_dir):
            os.system(f'rm -rf {log_dir}')
        os.makedirs(log_dir)
        with pushd(path):
            # create folder logs
            status, output = run_command_with_timeout(cmd)
            if status == ExecutionStatus.TIMEOUT:
                logger.info(f'{target}: TIMEOUT')
        break # only run one command for now
