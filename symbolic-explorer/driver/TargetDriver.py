import os
import signal
import subprocess
from contextlib import contextmanager
from enum import Enum

from data.Database import Database
from driver.SymbolicStorage import SymbolicStorage
from data.StaticAnalysisGraph.SAGraph import SAGraph

import log
logger = log.get_logger()
from solver.SolverHandler import SATResult
from strategy.StrategyService import StrategyService


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
        self.verdict = Verdict.SAFE


class TargetDriver:
    def __init__(self, args):
        self.state = State()
        self.sym_storage = SymbolicStorage()
        self.endpoint_id = None
        self.args = args
        self.branch_log = None  # Log file for branches
        self.sa_graph = SAGraph() # static analysis graph

    def build_command(self, mem: int = 32) -> list[str]:
        """Builds the Java command list with given parameters."""
        cmd = [
            'java',
            f'-Xmx{mem}g',
            f'-Dconfig.path={self.args.config}',
            f'-javaagent:{self.args.agent}',
            f"-Djava.library.path={self.args.z3dir}",
            '-Dsolver.mode=HTTP',
            '-Dagent.logging.level=DEBUG',
            '-ea',  # Enable assertions
        ]

        # Add target
        if self.args.target.endswith('.jar'):
            cmd.extend(['-jar', self.args.target])
        else:
            # Assume target is a class name with classpath provided
            if self.args.classpath:
                cmd.extend(['-cp', ':'.join(self.args.classpath), self.args.target])
            else:
                logger.error('[EXPLORE] No classpath provided for class target')
                cmd.extend([self.args.target])

        return cmd

    def add_values(self, cmd: list[str]) -> list[str]:
        cmd = cmd.copy()
        """Adds the symbolic values to the Java command."""
        for var in self.sym_storage.vars.values():
            if var.newValue is None:
                val = var.value
            else:
                val = var.newValue
                var.value = var.newValue
            if val is None:
                # Neither a solver value nor a concrete fallback is known; skipping the
                # assignment lets the executor keep the target's original value instead
                # of crashing on parsing a literal 'None'.
                logger.error(f'[EXPLORER] No value known for {var.dType.value}_{var.idx}, skipping assignment')
                continue
            if self.args.mode == "args":
                cmd.append(f'{val}')
            else:
                cmd.insert(1, f'-Dswat.assignment.{var.dType.value}_{var.idx}={val}')
        return cmd

    def run_command_with_timeout(self, cmd: list[str], timeout: int = 60) -> tuple[ExecutionStatus, dict]:
        """Executes the given command and returns the status and message."""

        logger.debug(f'[EXPLORER] Running command: {" ".join(cmd)}')
        logger.info(f'[EXPLORER] Java Output Begin')
        try:
            stdout = []
            with subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, bufsize=1,
                                  universal_newlines=True) as proc:
                for line in proc.stdout or []:
                    logger.info(f'[EXECUTOR] --> {line.strip()}')
                    stdout.append(line)
            logger.info(f'[EXPLORER] Java Output End')
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
            logger.critical(f'[EXPLORER] Exception: {e}')
            return ExecutionStatus.CRASH, str(e)

    def record_violation(self):
        """Records the violation in the database."""
        db = Database.instance()
        db.add_violation(endpoint_id=self.endpoint_id, sym_vars=list(self.sym_storage.vars.values()))

    def determine_next_step(self, status: ExecutionStatus, stdout: list[str]) -> Action:
        """Determines the next step based on the execution status."""
        match status:
            case ExecutionStatus.SUCCESS:
                return Action.SYMBOLICNEXT
            case ExecutionStatus.VIOLATION:
                self.record_violation()
                logger.info(f'[EXPLORER] Violation recorded!')
                self.state.verdict = Verdict.VIOLATION
                # return Action.SYMBOLICNEXT
                return Action.REPORTVERDICT
            case ExecutionStatus.TIMEOUT:
                logger.info(f'[EXPLORER] Timeout!')
                if self.state.verdict != Verdict.VIOLATION:
                    self.state.verdict = Verdict.UNKNOWN
                return Action.REPORTVERDICT
            case ExecutionStatus.CRASH:
                logger.info(f'[EXPLORER] Crash!')
                if self.state.verdict != Verdict.VIOLATION:
                    self.state.verdict = Verdict.UNKNOWN
                return Action.REPORTVERDICT
            case ExecutionStatus.ERROR:
                logger.info(f'[EXPLORER] Error!')
                if self.state.verdict != Verdict.VIOLATION:
                    self.state.verdict = Verdict.UNKNOWN
                return Action.REPORTVERDICT

        raise Exception(f'Unknown execution status: {status}')

    def retrieve_solution(self):
        possible_branches = StrategyService.select_branch(endpoint_id=self.endpoint_id, sa_node=self.sa_graph.entry_node)
        # possible_branches = possible_branches[::-1]  # Reverse the order to prioritize deeper branches
        logger.info(f'[EXPLORER] Found {len(possible_branches)} possible branches')
        sat = None
        branch_found = False
        for branch in possible_branches:
            if not StrategyService.is_symbolic_branch(branch):
                continue
            branch_found = True

            sat, sol = StrategyService.solve_branch(branch)

            if self.branch_log is None:
                self.branch_log = open("branches.log", "w")
            branch_id = branch.id & 0xFFFFFFFFFFFFFFFF
            print(f"Branch 0x{branch_id:016x} ({branch_id}) {sat}", flush=True, file=self.branch_log)

            if sat == SATResult.SAT:
                sol_viz = {key: val.get('plain_value', val.get('encoded_value'))
                          for key, val in sol.items()}
                logger.info(f'[EXPLORER] Solved branch {branch.id}')
                logger.info(f'[EXPLORER] Solution: {sol_viz}')

                # Register the solution in symbolic storage
                self.sym_storage.register_inputs(branch.inputs)
                self.sym_storage.store_solution(sol)
                return Action.SYMBOLICNEXT

        if not branch_found or sat == SATResult.UNSAT:
            # self.state.verdict = Verdict.SAFE
            logger.info(f'[EXPLORER] No symbolic branch found or UNSAT')
            return Action.REPORTVERDICT

        if sat == SATResult.UNKNOWN:
            logger.info(f'[EXPLORER] SAT result is UNKNOWN')
            if self.state.verdict != Verdict.VIOLATION:
                self.state.verdict = Verdict.UNKNOWN
            return Action.REPORTVERDICT

    def run(self):
        verdict = self.exec()
        logger.info(f'[EXPLORER] Verdict: {verdict}')
        self.kill_current_process()

    def exec(self):
        """Runs the symbolic execution on the given testcase."""
        
        logger.info(f'[EXPLORER] Beginning testcase analysis')
        # Register symbolic variables (in case of cmd line argument mode)
        if self.args.mode == "args":
            self.sym_storage.register_vars(self.args.symbolicvars)
            self.sym_storage.init_values()
        # Build the command to execute target
        base_cmd = self.build_command()
        
        # Load static pre-analysis information if provided
        if self.args.sa_file:
            self.sa_graph.load_json_graph(self.args.sa_file)
        
        # Main execution loop
        iteration = 1
        while iteration <= 150:
            logger.info('')
            logger.info('='*70)
            logger.info(f'ITERATION {iteration}')
            logger.info('='*70)

            # Add the symbolic values
            cmd = self.add_values(base_cmd)
            # Run the command
            status, output = self.run_command_with_timeout(cmd)
            # Determine the next step
            next_step = self.determine_next_step(status, output)
            # Select the (only!) endpoint
            assert len(Database.instance().get_endpoints()) == 1
            self.endpoint_id = Database.instance().get_endpoints()[0]

            # Visualize DB tree
            # print("Plotting DB Tree...", flush=True)
            # Database.instance().get_tree(self.endpoint_id).plot_tree(iteration)
            
            iteration += 1

            if next_step == Action.REPORTVERDICT:
                break

            if next_step == Action.SYMBOLICNEXT:
                logger.info(f'[EXPLORER] Next step: SYMBOLIC EXPLORATION')

                next_step = self.retrieve_solution()
                if next_step == Action.REPORTVERDICT:
                    break

        logger.info(f'[EXPLORER] Symbolic execution terminated after {iteration-1} iterations')
        violations = Database.instance().get_violations(self.endpoint_id)
        logger.info(f'[EXPLORER] Found {len(violations)} violations')
        if len(violations) > 0:
            for v in violations:
                logger.info(f'[EXPLORER] Violation: {[vv.__str__() for vv in v]}')
        
        return self.state.verdict

    def kill_current_process(self):
        pid = os.getpid()
        os.kill(pid, signal.SIGTERM)  # Send termination signal
        # os.kill(pid, signal.SIGKILL)  # Use this for a more forceful kill if needed
