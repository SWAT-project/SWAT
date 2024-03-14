import os
import signal
import subprocess
from contextlib import contextmanager
from enum import Enum

from data.Database import Database
from driver.SymbolicStorage import SymbolicStorage
# import logging
from log import logger
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
        self.verdict = Verdict.UNKNOWN


class TargetDriver:
    def __init__(self):
        self.state = State()
        self.sym_storage = SymbolicStorage()
        self.endpoint_id = None

    def build_command(self, args, mem: int = 32) -> [str]:
        """Builds the Java command list with given parameters."""
        cmd = [
            'java',
            f'-Xmx{mem}g',
            f'-Dconfig.path={args.config}',
            f'-javaagent:{args.agent}',
            f"-Djava.library.path={args.z3dir}",
            '-Dlogging.level=DEBUG',
            '-ea',
            '-jar',
            args.target
        ]
        return cmd

    def add_values(self, cmd: [str]) -> [str]:
        cmd = cmd.copy()
        """Adds the symbolic values to the Java command."""
        for var in self.sym_storage.vars.values():
            if var.newValue is None:
                val = var.value
            else:
                val = var.newValue
                var.value = var.newValue
            cmd.append(f'{val}')
        return cmd

    def run_command_with_timeout(self, cmd: [str], timeout: int = 60) -> (ExecutionStatus, dict):
        """Executes the given command and returns the status and message."""

        logger.info(f'[EXPLORER] Java Output Begin')
        try:
            stdout = []
            with subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, bufsize=1,
                                  universal_newlines=True) as proc:
                for line in proc.stdout:
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

    def determine_next_step(self, status: ExecutionStatus, stdout: [str]) -> Action:
        """Determines the next step based on the execution status."""
        match status:
            case ExecutionStatus.SUCCESS:
                return Action.SYMBOLICNEXT
            case ExecutionStatus.VIOLATION:
                self.record_violation()
                logger.info(f'[EXPLORER] Violation recorded!')
                return Action.SYMBOLICNEXT
            case ExecutionStatus.TIMEOUT:
                logger.info(f'[EXPLORER] Timeout!')
                self.state.verdict = Verdict.UNKNOWN
                return Action.REPORTVERDICT
            case ExecutionStatus.CRASH:
                logger.info(f'[EXPLORER] Crash!')
                self.state.verdict = Verdict.UNKNOWN
                return Action.REPORTVERDICT
            case ExecutionStatus.ERROR:
                logger.info(f'[EXPLORER] Error!')
                self.state.verdict = Verdict.UNKNOWN
                return Action.REPORTVERDICT

        raise Exception(f'Unknown execution status: {status}')

    def retrieve_solution(self):
        possible_branches = StrategyService.select_branch(endpoint_id=self.endpoint_id)
        logger.info(f'[EXPLORER] Found {len(possible_branches)} possible branches')
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
            logger.info(f'[EXPLORER] No symbolic branch found or UNSAT')
            return Action.REPORTVERDICT

        if sat == SATResult.UNKNOWN:
            logger.info(f'[EXPLORER] SAT result is UNKNOWN')
            self.state.verdict = Verdict.UNKNOWN
            return Action.REPORTVERDICT

        sol_viz = [f'{key}: {val["plain_value"]}' for key, val in sol.items()]
        logger.info(f'[EXPLORER] Found new solution: {sol_viz}')
        # self.sym_storage.register_vars(symbolic_vars)
        self.sym_storage.store_solution(sol)
        return Action.SYMBOLICNEXT

    def run(self, args):
        verdict = self.exec(args)
        logger.info(f'[EXPLORER] Verdict: {verdict}')
        self.kill_current_process()

    def exec(self, args):

        """Runs the symbolic execution on the given testcase."""
        logger.info(f'[EXPLORER] Beginning testcase analysis')
        # Register symbolic variables
        self.sym_storage.register_vars(args.symbolicvars)
        self.sym_storage.init_values()
        # Build the command to execute target
        base_cmd = self.build_command(args)
        # Main execution loop
        while True:
            # Add the symbolic values
            cmd = self.add_values(base_cmd)
            # Run the command
            status, output = self.run_command_with_timeout(cmd)
            # Determine the next step
            next_step = self.determine_next_step(status, output)
            # Select the (only!) endpoint
            assert len(Database.instance().get_endpoint_ids()) == 1
            self.endpoint_id = Database.instance().get_endpoint_ids()[0]
            if next_step == Action.REPORTVERDICT:
                break

            if next_step == Action.SYMBOLICNEXT:
                logger.info(f'[EXPLORER] Next step: SYMBOLIC EXPLORATION')

                next_step = self.retrieve_solution()
                if next_step == Action.REPORTVERDICT:
                    break

        logger.info(f'[EXPLORER] Symbolic execution terminated')
        violations = Database.instance().get_violations(self.endpoint_id)
        logger.info(f'[EXPLORER] Found {len(violations)} violations')
        if len(violations) > 0:
            for v in violations:
                logger.info(f'[EXPLORER] Violation: {[vv.__str__() for vv in v]}')

    def kill_current_process(self):
        pid = os.getpid()
        os.kill(pid, signal.SIGTERM)  # Send termination signal
        # os.kill(pid, signal.SIGKILL)  # Use this for a more forceful kill if needed
