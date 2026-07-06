import glob
import signal
import subprocess
import os
import time
from contextlib import contextmanager
from typing import List, Tuple
from pathlib import Path


from solver.SolverHandler import SATResult

from data.Database import Database
from data.BinaryExecutionTree.Node import Node

from strategy.StrategyService import StrategyService

from enum import Enum
from svcomp.SymbolicStorage import SymbolicStorage
from timing.TimingManager import TimingManager


import log
logger = log.get_logger()
verdict_logger = log.get_verdict_logger()
import platform


# The (unique) endpoint ID for the SV-COMP target. As each target is handled separately, this ID is always 0.
ENDPOINT_ID = 0

# A branch guarded by an uninterpreted pure_ UF can diverge on concrete replay (the solver cannot
# drive the UF to the required value). We re-open and retry such a branch - the injected observed
# input->output pairs pin the previous input's real output, forcing a fresh solver input - but only
# up to this many attempts, after which it
# is abandoned as unverified (forcing UNKNOWN). This bounds exploration and guarantees termination.
MAX_BRANCH_ATTEMPTS = 3
    

class ExecutionStatus(Enum):
    SUCCESS = 1
    ERROR = 2
    TIMEOUT = 3

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

class VerificationCategory(Enum):
    VALID_ASSERT_PRP = "valid-assert.prp"
    NO_RUNTIME_EXCEPTION_PRP = "no-runtime-exception.prp"
    NO_DEADLOCK_PRP = "no-deadlock.prp"



class State:
    def __init__(self):
        self.verdict = Verdict.UNKNOWN
        self.branch_to_explore: Node | None = None
        # Node gids we solved and intended to explore, but abandoned after the concrete re-run kept
        # diverging (the predicted branch was never actually taken - e.g. a branch guarded by an
        # uninterpreted pure_ UF the solver cannot drive to the required value). While any remain, a
        # SAFE verdict is unsound: that branch was not actually explored.
        self.unverified_branches: set = set()
        # Per-branch (node gid) count of solve+re-run attempts that diverged; drives attempt-based
        # prioritization and the re-open/abandon bound.
        self.branch_attempts: dict = {}
    
    
class SVCompDriver:
    def __init__(self, args):
        self.args = args
        self.state = State()
        self.symbolicStorage = SymbolicStorage()
        self.shutdown_flag = False
        self.verification_category = VerificationCategory(self.args.property)
    
    @contextmanager
    def pushd(self, dirname):
        """Context manager to temporarily change the working directory."""
        original_dir = os.getcwd()
        os.chdir(dirname)
        try:
            yield
        finally:
            os.chdir(original_dir)

    def build_command(self,javaagent:str, config:str, Z3_DIR: str, port, cp,  java_path:str = 'java', mem:int=32) -> List[str]:
        """Builds the Java command list with given parameters."""

        classpath_separator = ";" if platform.system() == "Windows" else ":"
        cmd = [
            java_path,
            f'-Xmx{mem}g',
            f'-Dconfig.path={config}',
            f'-Dexplorer.port={port}',
            f'-javaagent:{javaagent}',
            f"-Djava.library.path={Z3_DIR}",
            '-cp',
            f'{classpath_separator.join(cp)}{classpath_separator}{Z3_DIR}',
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


    def run_command_with_timeout(self, cmd: List[str]) -> Tuple[ExecutionStatus, List[str]]:
        """ Executes the given command and returns output from both STDOUT and STDERR.
            Todo: What about timeouts?
        """
        logger.info(f'[SVCOMP] Executing target')
        output = []
        with subprocess.Popen(cmd, stdout=subprocess.PIPE,stderr=subprocess.STDOUT, bufsize=1, universal_newlines=True) as proc:        
            try:
                stdout, _ = proc.communicate() #timeout=timeout
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
                return ExecutionStatus.ERROR, output + [str(e)]


    def determine_next_step(self, status: ExecutionStatus, output: List[str] ) -> Action:
        """Determines the next step based on the execution status."""
        if status == ExecutionStatus.SUCCESS:

            
            for l in output:
                if "java.lang.AssertionError: [SWAT]" in l:
                    # Internal assertion error in the DSE 
                    logger.critical(f'[SVCOMP] SWAT Assertion failed: {l}')
                    self.state.verdict = Verdict.UNKNOWN
                    return Action.REPORTVERDICT
                if "java.lang.AssertionError" in l and self.verification_category == VerificationCategory.VALID_ASSERT_PRP:
                    logger.info(f'[SVCOMP] Target Assertion failed: {l}')
                    self.state.verdict = Verdict.VIOLATION
                    return Action.REPORTVERDICT
                if "[SWAT] Uncaught top-level RuntimeException in symbolic execution" in l and self.verification_category == VerificationCategory.NO_RUNTIME_EXCEPTION_PRP:
                    logger.info(f'[SVCOMP] Uncaught Exception: {l}')
                    self.state.verdict = Verdict.VIOLATION
                    return Action.REPORTVERDICT
                if ("[SWAT] Uncaught top-level RuntimeException in symbolic execution" in l or "[SWAT] Uncaught top-level checked exception in symbolic execution" in l) and self.verification_category == VerificationCategory.VALID_ASSERT_PRP:
                    logger.info(f'[SVCOMP] Uncaught Exception: {l}')
                    Database.instance().record_uncaught_exception(ENDPOINT_ID)
                if "[SWAT Exception]:" in l:
                    logger.info(f'[SVCOMP] SWAT Exception: {l}')
                    self.state.verdict = Verdict.UNKNOWN
                    return Action.REPORTVERDICT


            if len(Database.instance().get_tree(ENDPOINT_ID).symbolic_vars) == 0:
                # No symbolic vars found, safe to assume no violation
                logger.info(f'[SVCOMP] No symbolic vars found')
                self.state.verdict = Verdict.NO_SYMBOLIC_VARS
                return Action.REPORTVERDICT
        
            logger.info(f'[SVCOMP] Found {len(Database.instance().get_tree(ENDPOINT_ID).symbolic_vars)} symbolic vars')

            # No assertion error found but symbolic variables present, continue with symbolic exploration
            return Action.SYMBOLICNEXT
        
        elif status == ExecutionStatus.TIMEOUT or status == ExecutionStatus.ERROR:
            # Todo: Maybe allow for a retry?
            self.state.verdict = Verdict.UNKNOWN
            return Action.REPORTVERDICT
        
        
    def log_output(self, output: List[str]):
        logger.info(f'[SVCOMP] Java Output Begin')
        for line in output:
            logger.info(f'[SWAT] --> {line.strip()}')
        logger.info(f'[SVCOMP] Java Output End')    

    def run_testcase(self, java_path, agentpath: str, configpath: str, z3path, port, cp) -> Verdict:
        """Runs the testcase using the constructed Java command."""

        next_step = Action.RANDOMNEXT
        round_idx = 0

        while True:
            logger.info(f'[SVCOMP] {"="*73}')
            logger.info(f'[SVCOMP] {"="*30} ROUND ({round_idx:03}) {"="*30}')
            logger.info(f'[SVCOMP] {"="*73}')

            # Build the java command to run DSE
            command: List[str] = self.build_command(javaagent=agentpath, config=configpath, Z3_DIR=z3path, port=port, cp=cp, java_path=java_path)

            logger.debug(f'[SVCOMP] Executing command: {command}')

            # Run the target with DSE - Time the symbolic executor
            status: ExecutionStatus
            output: List[str]
            executor_start = time.perf_counter()
            status, output = self.run_command_with_timeout(cmd=command)
            executor_duration = time.perf_counter() - executor_start
            TimingManager.instance().record_executor_time(executor_duration)

            # check if the target explored the branch as expected
            if self.state.branch_to_explore:
                diverged = False
                for branch in Database.instance().get_trace(-1): # most recent trace
                    if branch.id == self.state.branch_to_explore.id:
                        # We solved this branch to flip it; if the re-run took the same direction, the
                        # predicted branch was not actually explored (an uninterpreted pure_ UF the
                        # solver could not drive to the required value).
                        if branch.has_branched == (self.state.branch_to_explore.branched is not None):
                            logger.warning(f'[SVCOMP] Branch {branch.id} diverged: predicted direction not taken.')
                            diverged = True
                        break
                else:
                    logger.warning(f'[SVCOMP] Branch {self.state.branch_to_explore.id} diverged: does not appear in trace.')
                    diverged = True
                if diverged:
                    self.handle_divergence(self.state.branch_to_explore)
            self.state.branch_to_explore = None

            self.log_output(output)
            logger.info(f'[STATUS] {status}')
            next_step: Action = self.determine_next_step(status, output)

            round_idx += 1

            if next_step == Action.REPORTVERDICT:
                logger.info(f'[SVCOMP] Finised testcase analysis, reporting verdict')
                return self.state.verdict


            elif next_step == Action.SYMBOLICNEXT:
                logger.info(f'[SVCOMP] Next step: SYMBOLIC EXPLORATION')

                # Explorer time is computed as residual in TimingManager.get_aggregates()
                # (total_time - executor_time - solver_time - witness_times)
                next_step = self.retrieve_solution()

                if next_step == Action.REPORTVERDICT:
                    return self.state.verdict
                elif next_step == Action.SYMBOLICNEXT:
                    self.symbolicStorage.input_type = INPUTTYPE.SYMBOLIC
                
         
            

    def handle_divergence(self, branch: Node):
        """A solved branch diverged on concrete replay. Re-open it for another attempt (bounded), or
        abandon it as unverified once the attempt cap is reached (forcing UNKNOWN)."""
        gid = branch.gid
        self.state.branch_attempts[gid] = self.state.branch_attempts.get(gid, 0) + 1
        attempts = self.state.branch_attempts[gid]
        if attempts < MAX_BRANCH_ATTEMPTS:
            # Not actually explored - re-open so it is selectable again (yields a different solver
            # input because the injected observed pairs pin the previous input's real output). Bounded by the attempt cap.
            Database.instance().remove_solution(gid)
            logger.info(f'[SVCOMP] Re-opening diverged branch {branch.id} (gid {gid}), attempt {attempts}/{MAX_BRANCH_ATTEMPTS}')
        else:
            self.state.unverified_branches.add(gid)
            logger.warning(f'[SVCOMP] Abandoning diverged branch {branch.id} (gid {gid}) after {attempts} attempts; verdict cannot be SAFE.')

    def retrieve_solution(self):
        possible_branches = StrategyService.select_branch(endpoint_id=ENDPOINT_ID)
        # Prefer branches tried fewer times so a repeatedly-diverging branch does not starve the rest;
        # exclude any that hit the attempt cap (abandoned -> unverified -> UNKNOWN).
        possible_branches = [b for b in possible_branches
                             if self.state.branch_attempts.get(b.gid, 0) < MAX_BRANCH_ATTEMPTS]
        possible_branches.sort(key=lambda b: self.state.branch_attempts.get(b.gid, 0))
        logger.info(f'[SYMBOLIC EXPLORATION] Found {len(possible_branches)} possible branches')
        logger.info(f'[SYMBOLIC EXPLORATION] Possible branch IDs: {[b.id for b in possible_branches]}')
        symbolic_vars = None
        sat = None
        # A solver UNKNOWN (e.g. timeout) anywhere in this round means that branch was neither
        # explored nor proven infeasible, so a SAFE conclusion is not backed - even if a later
        # branch returns UNSAT.
        unknown_seen = False
        branch_found = False
        for branch in possible_branches:
            #logger.info(f'[SYMBOLIC EXPLORATION] Checking branch {branch.id}, kind={branch.kind}')
            if not StrategyService.is_symbolic_branch(branch):
                #logger.info(f'[SYMBOLIC EXPLORATION] Skipping non-symbolic branch {branch.id}, constraint has no symbolic vars')
                continue
            branch_found = True
            #logger.info(f'[SYMBOLIC EXPLORATION] Solving for branch {branch.id}')
            sat, sol = StrategyService.solve_branch(branch)

            if sat == SATResult.SAT:
                logger.info(f'[SYMBOLIC EXPLORATION] Found solution for branch {branch.id} {"skipped" if branch.skipped is None else "branched"}')
                symbolic_vars = branch.inputs
                # remember which branch we want to explore
                self.state.branch_to_explore = branch
                break
            if sat == SATResult.UNKNOWN:
                unknown_seen = True
            logger.debug(f'[SYMBOLIC EXPLORATION] No solution ({sat}) found for branch {branch.id}')

        if not branch_found or (sat == SATResult.UNSAT and not unknown_seen):
            self.state.verdict = Verdict.SAFE
            logger.info(f'[SYMBOLIC EXPLORATION] No symbolic branch found or UNSAT')
            return Action.REPORTVERDICT

        if sat == SATResult.UNKNOWN or unknown_seen:
            logger.info(f'[SYMBOLIC EXPLORATION] SAT result is UNKNOWN')
            self.state.verdict = Verdict.UNKNOWN
            return Action.REPORTVERDICT
        
        sol_viz = [f'{key}: {val["plain_value"]}' for key, val in sol.items()]
        logger.info(f'[SYMBOLIC EXPLORATION] SAT solution: {sol}')
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
        
    def run(self):
        Database.instance().add_endpoint(ENDPOINT_ID)
        logger.info(f'[SVCOMP] Beginning testcase analysis for property {self.verification_category.value}')

        # Start total timing
        TimingManager.instance().start_total_timer()

        verdict = self.run_testcase(java_path=self.args.java_path, agentpath=self.args.agent, configpath=self.args.config, z3path=self.args.z3dir, port=self.args.port, cp=self.args.classpath)
        
        if (verdict == Verdict.SAFE) and Database.instance().get_tree(ENDPOINT_ID).symbolic_context_loss:
            logger.warning(f'[SVCOMP] Found symbolic context loss')
            verdict = Verdict.UNKNOWN

        if (verdict == Verdict.SAFE) and Database.instance().get_tree(ENDPOINT_ID).symbolic_precision_loss:
            logger.warning(f'[SVCOMP] Found symbolic precision loss')
            verdict = Verdict.UNKNOWN

        if (verdict == Verdict.SAFE) and Database.instance().get_tree(ENDPOINT_ID).uncaught_exceptions > 0:
            logger.warning(f'[SVCOMP] Found uncaught exceptions during symbolic execution')
            verdict = Verdict.UNKNOWN

        if (verdict == Verdict.SAFE) and self.state.unverified_branches:
            logger.warning(f'[SVCOMP] Cannot conclude SAFE: {len(self.state.unverified_branches)} branch(es) diverged and were never verified (downgrading to UNKNOWN)')
            verdict = Verdict.UNKNOWN

        if verdict == Verdict.NO_SYMBOLIC_VARS:
            verdict = Verdict.SAFE
        verdict_logger.info(f'[VERDICT {self.verification_category.value}] {verdict.value}')

        # Stop total timing (don't print summary here - execution.py will print complete summary with witness timing)
        TimingManager.instance().stop_total_timer()

        # Save timing data to file in the log directory
        log_dir = self.args.logdir if hasattr(self.args, 'logdir') else 'logs'
        timing_file = os.path.join(log_dir, 'timing_data.json')
        TimingManager.instance().save_to_file(Path(timing_file))

        self.kill_current_process()
        
        
    def kill_current_process(self):
        pid = os.getpid()  
        os.kill(pid, signal.SIGTERM)  # Send termination signal
        # os.kill(pid, signal.SIGKILL)  # Use this for a more forceful kill if needed


