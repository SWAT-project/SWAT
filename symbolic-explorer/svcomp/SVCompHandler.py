import glob
import re
import signal
import subprocess
import os, sys
from contextlib import contextmanager


from solver.SolverHandler import SATResult

from data.Database import Database

from strategy.StrategyService import StrategyService

from enum import Enum
from svcomp.SymbolicStorage import SymbolicStorage
import logging 
from log import verdict_logger
logging.basicConfig(stream=sys.stdout, level=logging.DEBUG)
    
logger = logging.getLogger(__name__)

class ExecutionStatus(Enum):
    SUCCESS = 1
    ERROR = 2
    TIMEOUT = 3
    CRASH = 4
    VIOLATION = 5

class Verdict(Enum):
    VIOLATION = "== ERROR"
    SAFE = "== OK"
    UNKNOWN = "== DONT-KNOW"
    NO_SYMBOLIC_VARS = "== NON-SYMBOLIC"

class Action(Enum):
    RANDOMNEXT = 1
    SYMBOLICNEXT = 2
    REPORTVERDICT = 3
    
class INPUTTYPE(Enum):
    RANDOM = 1
    SYMBOLIC = 2
    MAGIC = 3



class State:
    def __init__(self):
        self.verdict = Verdict.UNKNOWN
    
    
class SVCompHandler:
    def __init__(self):
        self.state = State()
        self.symbolicStorage = SymbolicStorage()
        self.shutdown_flag = False
    
    @contextmanager
    def pushd(self, dirname):
        """Context manager to temporarily change the working directory."""
        original_dir = os.getcwd()
        os.chdir(dirname)
        try:
            yield
        finally:
            os.chdir(original_dir)

    def build_command(self, javaagent:str, config:str, Z3_DIR: str, mem:int=32) -> [str]:
        """Builds the Java command list with given parameters."""
        cmd = [
            'java',
            f'-Xmx{mem}g',
            f'-Dswat.cfg={config}',
            f'-javaagent:{javaagent}',
            f"-Djava.library.path={Z3_DIR}",
            '-cp',
            f'build:{Z3_DIR}',
            '-ea',
            'Main'
        ]
        if self.symbolicStorage.input_type == INPUTTYPE.SYMBOLIC:
        
            sym_vars = []
            for var in self.symbolicStorage.vars.values():
                val = var.newValue if var.newValue is not None else var.value
                sym_vars.append(f'-Dswat.input.{var.dType.value}_{var.idx}={val}')
            cmd[3:3] = sym_vars
        return cmd



    def run_command_with_timeout(self, cmd: [str], timeout: int = 60) -> (ExecutionStatus, dict):
        """Executes the given command and returns the status and message."""
        
        logger.info(f'[SVCOMP] Java Output Begin')
        try:
            stdout =  []
            with subprocess.Popen(cmd, stdout=subprocess.PIPE,stderr=subprocess.STDOUT, bufsize=1, universal_newlines=True) as proc:
                for line in proc.stdout:
                    logger.info(f'[SWAT] --> {line.strip()}')
                    stdout.append(line)
            logger.info(f'[SVCOMP] Java Output End')
            for l in stdout:
                if '*** java.lang.instrument ASSERTION FAILED ***' in l:
                    return ExecutionStatus.ERROR, stdout
            if proc.returncode == 0:
                return ExecutionStatus.SUCCESS, stdout
            else:
                for l in stdout:
                    if "java.lang.AssertionError" in l:
                        return ExecutionStatus.VIOLATION, stdout
                return ExecutionStatus.ERROR, stdout
        except subprocess.TimeoutExpired:
            return ExecutionStatus.TIMEOUT, stdout
        except Exception as e:
            logger.critical(f'[SVCOMP] Exception: {e}')
            return ExecutionStatus.CRASH, str(e)




    def determine_next_step(self, status: ExecutionStatus, stdout: [str] ) -> Action:
        """Determines the next step based on the execution status."""
        if status == ExecutionStatus.SUCCESS:
            for line in stdout:
                if re.match(r'.*\[DSE\] Initializing symbolic tracking for the following value .*', line):
                    return Action.SYMBOLICNEXT
            
            self.state.verdict = Verdict.NO_SYMBOLIC_VARS


            return Action.REPORTVERDICT
        
        elif status == ExecutionStatus.VIOLATION:

            self.state.verdict = Verdict.VIOLATION

            return Action.REPORTVERDICT
        
        elif status == ExecutionStatus.TIMEOUT or status == ExecutionStatus.CRASH or status == ExecutionStatus.ERROR:
            self.state.verdict = Verdict.UNKNOWN
            return Action.REPORTVERDICT
        
        else: 
            self.state.verdict = Verdict.UNKNOWN
            return Action.REPORTVERDICT
        
    def run_testcase(self, base_dir: str, classpath: [str], agentpath: str, configpath: str, z3path) -> Verdict:
        """Runs the testcase using the constructed Java command."""
        db = Database.instance()

        next_step = Action.RANDOMNEXT
        idx = 0
        while True:
            
            logger.info(f'[SVCOMP] ============================= ROUND ({idx}) =============================')
            idx +=1 
            command = self.build_command(javaagent=agentpath, config=configpath, Z3_DIR=z3path)
            logger.info(f'[SVCOMP] Execution command: {command}')

            status, output = self.run_command_with_timeout(cmd=command, timeout=60)
            logger.info(f'[STATUS] {status}')
            next_step = self.determine_next_step(status, output)
            

            if next_step == Action.REPORTVERDICT:
                logger.info(f'[SVCOMP] Next step: REPORTVERDICT')
                return self.state.verdict
            
                
            elif next_step == Action.SYMBOLICNEXT:
                logger.info(f'[SVCOMP] Next step: SYMBOLIC EXPLORATION')
                
                next_step = self.retrieve_solution()
                if next_step == Action.REPORTVERDICT:
                    return self.state.verdict
                elif next_step == Action.SYMBOLICNEXT:
                    self.symbolicStorage.input_type = INPUTTYPE.SYMBOLIC
                
         
            

    def retrieve_solution(self):
        possible_branches = StrategyService.select_branch(endpoint_id=0)
        logger.info(f'[SYMBOLIC EXPLORATION] Found {len(possible_branches)} possible branches')
        symbolic_vars = None
        sat = None
        branch_found = False
        for branch in possible_branches:
            if not StrategyService.is_symbolic_branch(branch):
                continue
            branch_found = True
            sat, sol = StrategyService.solve_branch(branch)
             
            if sat == SATResult.SAT:
                symbolic_vars = branch.inputs
                break
       
        if not branch_found or sat == SATResult.UNSAT:
            self.state.verdict = Verdict.SAFE
            logger.info(f'[SYMBOLIC EXPLORATION] No symbolic branch found or UNSAT')
            return Action.REPORTVERDICT
        
        if sat == SATResult.UNKNOWN:
            logger.info(f'[SYMBOLIC EXPLORATION] SAT result is UNKNOWN')
            self.state.verdict = Verdict.UNKNOWN
            return Action.REPORTVERDICT
        
        sol_viz = [f'{key}: {val["plain_value"]}' for key, val in sol.items()]
        logger.info(f'[SYMBOLIC EXPLORATION] Found new solution: {sol_viz}')
        self.symbolicStorage.register_vars(symbolic_vars)
        self.symbolicStorage.store_solution(sol)
        return Action.SYMBOLICNEXT
        
    def check_missing_invocations(self):
        for l in glob.glob("logs/invocation-*.log"):
            with open(l, "r") as f:
                if f.read().strip() != "":
                    return True
        return False
        
    def run(self, basedir, classpath: [str], agentpath: str, configpath:str, z3dir:str):
        Database.instance().add_endpoint(0)
        logger.info(f'[SVCOMP] Beginning testcase analysis]')
        verdict = self.run_testcase(basedir, classpath, agentpath, configpath, z3dir)
        
        if (verdict == Verdict.SAFE) and self.check_missing_invocations():
            verdict = Verdict.UNKNOWN

        if verdict == Verdict.NO_SYMBOLIC_VARS:
            verdict = Verdict.SAFE

        verdict_logger.info(f'[VERDICT] {verdict.value}')

        
        self.kill_current_process()
        
        
    def kill_current_process(self):
        pid = os.getpid()  
        os.kill(pid, signal.SIGTERM)  # Send termination signal
        # os.kill(pid, signal.SIGKILL)  # Use this for a more forceful kill if needed


