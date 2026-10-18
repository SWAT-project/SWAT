import glob
import sys
import signal
import subprocess
import os
import time
from contextlib import contextmanager
from typing import List, Tuple
from pathlib import Path


from solver.SolverHandler import SATResult, Z3Handler
from z3 import Z3Exception
from solver.ConstraintCache import get_constraint_cache

from data.Database import Database
from data.BinaryExecutionTree.Node import Node

from strategy.StrategyService import StrategyService
from data.StaticAnalysisGraph.SAGraph import SAGraph
from svcomp.StaticPreAnalysis import StaticPreAnalysis

from enum import Enum
from svcomp.SymbolicStorage import SymbolicStorage, DataTypes
from svcomp.TestcaseStats import write_testcase_stats
from svcomp.WitnessWriter import write_violation_witness
from svcomp.ResourceBudget import ResourceBudget, DEFAULT_MEMORY_LIMIT_MB, DEFAULT_CPU_LIMIT_S, descendants
from timing.TimingManager import TimingManager


import log
logger = log.get_logger()
verdict_logger = log.get_verdict_logger()
import platform
import threading


# With --solver-rlimit, each time a branch's query runs out of its resource limit, the limit for
# that branch grows by this factor, up to MAX_RLIMIT (the largest value Z3 accepts).
RLIMIT_GROWTH = 5
MAX_RLIMIT = 2**32 - 1

# Execution errors after which an exhausted search proves nothing: a run was cut short, so the
# rest of its path was never explored.
CUT_SHORT_ERRORS = ('resource_limit', 'out_of_memory')

# Executor JVM options besides the heap size. Every round starts a new, short-lived JVM, in which
# the C2 compiler spends more CPU time than it ever saves (more than a third of an executor's CPU
# time on MinePump/TCAS); C1 alone compiles fast enough.
EXECUTOR_JVM_OPTIONS = ['-XX:TieredStopAtLevel=1']

# The (unique) endpoint ID for the SV-COMP target. As each target is handled separately, this ID is always 0.
ENDPOINT_ID = 0 
    

class ExecutionStatus(Enum):
    SUCCESS = 1
    ERROR = 2
    TIMEOUT = 3
    # Stopped by the resource budget before it ended (see svcomp/ResourceBudget.py)
    RESOURCE_LIMIT = 4

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
        # Whether the solution for branch_to_explore came from a trace with an approximate model,
        # so that the next run may legitimately diverge from it.
        self.solution_is_guess: bool = False
    
    
def escape_java_string(s: str) -> str:
    """Escapes a String input for the command line, undone by the executor's Verifier.nondetString:
    a backslash as \\\\, and every character outside printable ASCII as \\uXXXX, so the value does
    not depend on how the JVM decodes its arguments."""
    return ''.join('\\\\' if c == '\\' else c if ' ' <= c <= '~' else f'\\u{ord(c):04x}' for c in s)


class SVCompDriver:
    def __init__(self, args):
        self.args = args
        self.state = State()
        self.symbolicStorage = SymbolicStorage()
        self.shutdown_flag = False
        self.verification_category = VerificationCategory(self.args.property)
        self.sa_graph = SAGraph() # static analysis graph, empty until the pre-analysis is adopted
        self.sa = StaticPreAnalysis(args)
        self.sa_adopted_round: int | None = None
        self.round_idx = 0
        self.nr_solver_calls = 0
        self.solver_limit_hits = 0 # queries that ran out of their resource limit
        # How often each branch (by gid) ran out of its resource limit; each time multiplies it.
        self.rlimit_level: dict[int, int] = {}
        self.budget = ResourceBudget(memory_limit_mb=getattr(args, 'memory_limit', None) or DEFAULT_MEMORY_LIMIT_MB,
                                     cpu_limit_s=getattr(args, 'cpu_limit', None) or DEFAULT_CPU_LIMIT_S,
                                     cpu_used_s=getattr(args, 'cpu_used', None) or 0.0)
        self.budget.on_emergency = self.end_run_now
        # Only one verdict is reported, by the main thread or, in an emergency, by the monitor
        self.verdict_lock = threading.Lock()
        self.verdict_reported = False
        self.stopped_by: str | None = None  # why the budget stopped the last executor run, if it did
    
    @contextmanager
    def pushd(self, dirname):
        """Context manager to temporarily change the working directory."""
        original_dir = os.getcwd()
        os.chdir(dirname)
        try:
            yield
        finally:
            os.chdir(original_dir)

    def build_command(self,javaagent:str, config:str, Z3_DIR: str, port, cp,  java_path:str = 'java', heap_mib: int | None = None) -> List[str]:
        """Builds the Java command list with given parameters. The heap size defaults to the
        executor's share of the memory budget."""

        classpath_separator = ";" if platform.system() == "Windows" else ":"
        cmd = [
            java_path,
            f'-Xmx{heap_mib or self.budget.heap_mib}m',
            *EXECUTOR_JVM_OPTIONS,
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
                if var.dType == DataTypes.STRING and val is not None:
                    val = escape_java_string(val)
                sym_vars.append(f'-Dswat.input.{var.dType.value}_{var.idx}={val}')
            at = cmd.index(f'-Dconfig.path={config}') + 1
            cmd[at:at] = sym_vars
        return cmd


    def run_command_with_timeout(self, cmd: List[str]) -> Tuple[ExecutionStatus, List[str]]:
        """ Executes the given command and returns output from both STDOUT and STDERR.
            There is no timeout of its own: the resource budget stops the run when the memory or
            CPU time of the whole verification run gets close to its limit (RESOURCE_LIMIT).
        """
        logger.info(f'[SVCOMP] Executing target')
        output = []
        with subprocess.Popen(cmd, stdout=subprocess.PIPE,stderr=subprocess.STDOUT, bufsize=1, universal_newlines=True) as proc:        
            try:
                with self.budget.executor(proc) as stopped_by:
                    stdout, _ = proc.communicate()
                output = stdout.splitlines()
                self.stopped_by = stopped_by()
                if self.stopped_by:
                    logger.warning(f'[SVCOMP] Executor stopped by the resource budget ({self.stopped_by})')
                    return ExecutionStatus.RESOURCE_LIMIT, output
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
        """Determines the next step based on the execution status.

        A run the resource budget stopped (RESOURCE_LIMIT) still reports a violation it printed
        before. Otherwise it ended without a trace, so its path stays unexplored: on a CPU stop the
        verdict is UNKNOWN, on a memory stop exploration goes on with other branches.
        """
        if status == ExecutionStatus.SUCCESS or status == ExecutionStatus.RESOURCE_LIMIT:

            out_of_memory_recorded = False
            for l in output:
                if "java.lang.OutOfMemoryError" in l and not out_of_memory_recorded:
                    # The run was cut short by the JVM's memory limit, which the task does not have
                    logger.warning(f'[SVCOMP] Executor ran out of memory: {l.strip()}')
                    Database.instance().record_execution_error(ENDPOINT_ID, 'out_of_memory', l.strip())
                    out_of_memory_recorded = True
                if "java.lang.AssertionError: [SWAT]" in l:
                    # Internal assertion error in the DSE
                    logger.critical(f'[SVCOMP] SWAT Assertion failed: {l}')
                    Database.instance().record_execution_error(ENDPOINT_ID, 'swat_assertion', l.strip())
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
                    Database.instance().record_execution_error(ENDPOINT_ID, 'swat_exception', l.strip())
                    self.state.verdict = Verdict.UNKNOWN
                    return Action.REPORTVERDICT

            if status == ExecutionStatus.RESOURCE_LIMIT:
                Database.instance().record_execution_error(ENDPOINT_ID, 'resource_limit',
                                                           f'round {self.round_idx}: executor stopped ({self.stopped_by})')
                if self.stopped_by == 'cpu' or len(Database.instance().get_tree(ENDPOINT_ID).symbolic_vars) == 0:
                    self.state.verdict = Verdict.UNKNOWN
                    return Action.REPORTVERDICT
                return Action.SYMBOLICNEXT

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
        
        # Static pre-analysis: an external Java subprocess plus loading its graph. By default
        # exploration waits for it, and the wait is its own timing stage (otherwise it would be
        # folded into the symbolic explorer residual). With --parallel-sa it runs in the background
        # while exploration already starts without pruning, and the graph is adopted in
        # retrieve_solution() once it is ready.
        self.sa.start()
        if not self.args.parallel_sa:
            self.sa.wait()
            self.adopt_sa_graph()

        next_step = Action.RANDOMNEXT
        self.round_idx = 0

        while True:
            logger.info(f'[SVCOMP] {"="*73}')
            logger.info(f'[SVCOMP] {"="*30} ROUND ({self.round_idx:03}) {"="*30}')
            logger.info(f'[SVCOMP] {"="*73}')

            # Only a trace of this round may provide the witness data of the violating execution
            Database.instance().clear_witness_data(ENDPOINT_ID)

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

            # check if the target explored the branch as expected (a run stopped by the resource
            # budget sent no trace to check)
            if self.state.branch_to_explore and status != ExecutionStatus.RESOURCE_LIMIT:
                self.check_divergence(Database.instance().get_trace(-1)) # most recent trace
            self.state.branch_to_explore = None
            self.state.solution_is_guess = False

            self.log_output(output)
            logger.info(f'[STATUS] {status}')
            next_step: Action = self.determine_next_step(status, output)

            # Visualize DB tree
            # print("Plotting DB Tree...", flush=True)
            # Database.instance().get_tree(0).plot_tree(self.round_idx)
            self.round_idx += 1

            if next_step == Action.REPORTVERDICT:
                logger.info(f'[SVCOMP] Finised testcase analysis, reporting verdict')
                return self.state.verdict


            elif next_step == Action.SYMBOLICNEXT:
                logger.info(f'[SVCOMP] Next step: SYMBOLIC EXPLORATION')

                # Explorer time is computed as residual in TimingManager.get_aggregates()
                # (total_time - executor_time - solver_time - witness_times)
                next_step = self.retrieve_solution()
                if self.budget.cpu_exceeded.is_set() and next_step != Action.REPORTVERDICT:
                    logger.warning('[SVCOMP] CPU time deadline reached, stopping exploration')
                    self.state.verdict = Verdict.UNKNOWN
                    return self.state.verdict
                
                # If we are using SA and come to a SAFE verdict, then continue without SA
                # this should prevent false SAFE classifications from incomplete SA
                if self.args.sa_retry_without and self.sa_graph.entry_node is not None \
                        and next_step == Action.REPORTVERDICT and self.state.verdict == Verdict.SAFE:
                    logger.info(f'[SYMBOLIC EXPLORATION] No violation found. Retrying without SA.')
                    self.sa_graph = SAGraph() # disable SA
                    next_step = self.retrieve_solution() # and try again

                if next_step == Action.REPORTVERDICT:
                    return self.state.verdict
                elif next_step == Action.SYMBOLICNEXT:
                    self.symbolicStorage.input_type = INPUTTYPE.SYMBOLIC
                
         
            

    def check_divergence(self, trace) -> None:
        """Checks that the last run took the side of branch_to_explore its solution was solved for.

        A divergence is a bug, unless the solution relied on an approximate model: then it was only a
        guess, and the divergence is expected. That is logged without the "SWAT Assertion failed"
        marker the harness counts as an internal error, and counted in the tree's statistics.
        """
        target = self.state.branch_to_explore
        # The tree has one level per trace element, so the target is at its depth in the trace.
        # Matching by id alone would be wrong: ids repeat within a trace (all keys of a
        # LOOKUPSWITCH share one, TABLESWITCH case numbers wrap at 16, loops, recursion).
        depth = 0
        node = getattr(target, 'parent', None)
        while node is not None:
            depth += 1
            node = node.parent
        if depth < len(trace) and trace[depth].id == target.id:
            if trace[depth].has_branched != (target.branched is not None):
                return
            reason = 'Branch unexpectedly taken/skipped.'
        else:
            reason = 'Branch does not appear in trace.'
        if self.state.solution_is_guess:
            Database.instance().record_expected_divergence(ENDPOINT_ID)
            logger.info(f'[SVCOMP] Diverged from branch {target.id}: its solution relied on an approximate model (expected). {reason}')
        else:
            logger.error(f'[SVCOMP] SWAT Assertion failed: Target did not explore branch {target.id} as expected! {reason}')
            # The branch counts as solved now, so its unexplored side is never tried again: an
            # exhausted search no longer proves anything (see run()).
            Database.instance().record_execution_error(ENDPOINT_ID, 'unexpected_divergence', f'branch {target.id}: {reason}')

    def adopt_sa_graph(self):
        """Switches branch selection to the pre-analysis graph once it is ready (at most once).

        Switching mid-exploration is safe: every selection walks the tree from its root, and the
        rounds before only explored more than pruning would have.
        """
        graph = self.sa.take_graph()
        if graph is not None:
            logger.info(f'[EXPLORER] Using static pre-analysis graph from round {self.round_idx} on.')
            self.sa_graph = graph
            self.sa_adopted_round = self.round_idx

    def solver_rlimit_for(self, branch: Node) -> int | None:
        """The resource limit for the next query of this branch: the base limit, grown by
        RLIMIT_GROWTH for each time the branch already ran out of it (None: unlimited)."""
        if not self.args.solver_rlimit:
            return None
        return min(self.args.solver_rlimit * RLIMIT_GROWTH ** self.rlimit_level.get(branch.gid, 0), MAX_RLIMIT)

    def solve_candidates(self, candidates: list[Node]) -> tuple[Node | None, dict, bool]:
        """Solves the candidate branches until one is SAT.

        Returns the branch and its solution, or (None, {}, gave_up) if none is SAT, where gave_up
        says whether the solver failed to decide some branch, so that no SAFE verdict may follow.

        With a solver rlimit, a branch that runs out of it is skipped for now and retried with a
        larger limit once every cheaper query has been tried: branches are taken in order of how
        often they ran out already (a stable sort, so DFS order otherwise), and after a full pass
        the ones that ran out are tried again, until one is SAT or all are decided. A branch that
        runs out even at MAX_RLIMIT is given up on.

        The limit only serves to let other branches go first. So once a retry pass is left with a
        single branch, that one is solved without a limit: escalating it step by step would only
        redo the same work at each step.
        """
        gave_up = False
        pending = sorted(candidates, key=lambda b: self.rlimit_level.get(b.gid, 0))
        retry = False
        while pending:
            limited = []
            alone = retry and len(pending) == 1
            for branch in pending:
                if self.budget.cpu_exceeded.is_set():
                    return None, {}, True
                rlimit = None if alone else self.solver_rlimit_for(branch)
                Z3Handler.last_unknown_reason = None
                with self.budget.solver(get_constraint_cache().shared_context) as interrupted_by:
                    sat, sol = StrategyService.solve_branch(branch, solver_timeout_ms=None, solver_rlimit=rlimit)
                self.nr_solver_calls += 1
                if sat == SATResult.UNKNOWN and 'memory' in (Z3Handler.last_unknown_reason or ''):
                    # Z3 reached its memory limit and refuses further work until memory is freed
                    raise Z3Exception(Z3Handler.last_unknown_reason)
                if interrupted_by():
                    logger.info(f'[SYMBOLIC EXPLORATION] Query for branch {branch.id} stopped by the resource budget ({interrupted_by()})')
                    gave_up = True
                    continue
                if sat == SATResult.SAT:
                    return branch, sol, gave_up
                if sat == SATResult.LIMIT:
                    self.solver_limit_hits += 1
                    if rlimit is None or rlimit >= MAX_RLIMIT:
                        logger.info(f'[SYMBOLIC EXPLORATION] Branch {branch.id} exceeds even the largest resource limit, giving up on it')
                        gave_up = True
                        continue
                    self.rlimit_level[branch.gid] = self.rlimit_level.get(branch.gid, 0) + 1
                    logger.info(f'[SYMBOLIC EXPLORATION] Branch {branch.id} ran out of resource limit {rlimit}, retrying later with {self.solver_rlimit_for(branch)}')
                    limited.append(branch)
                elif sat == SATResult.UNKNOWN:
                    logger.info(f'[SYMBOLIC EXPLORATION] Solver could not decide branch {branch.id}')
                    gave_up = True
                else:
                    logger.debug(f'[SYMBOLIC EXPLORATION] No solution ({sat}) found for branch {branch.id}')
            pending = limited
            retry = True
        return None, {}, gave_up

    def retrieve_solution(self):
        try:
            return self._retrieve_solution()
        except Z3Exception as e:
            if 'memory' not in str(e):
                raise
            logger.warning(f'[SYMBOLIC EXPLORATION] Z3 ran out of its memory limit ({e}), stopping exploration')
            Database.instance().record_execution_error(ENDPOINT_ID, 'resource_limit', f'round {self.round_idx}: Z3 out of memory')
            self.state.verdict = Verdict.UNKNOWN
            return Action.REPORTVERDICT

    def _retrieve_solution(self):
        self.adopt_sa_graph()
        possible_branches = StrategyService.select_branch(endpoint_id=ENDPOINT_ID, sa_node=self.sa_graph.entry_node, rank_by_distance=self.args.sa_rank_distance)
        logger.info(f'[SYMBOLIC EXPLORATION] Found {len(possible_branches)} possible branches')
        logger.info(f'[SYMBOLIC EXPLORATION] Possible branch IDs: {[b.id for b in possible_branches]}')
        candidates = [b for b in possible_branches if StrategyService.is_symbolic_branch(b)]
        branch, sol, solver_gave_up = self.solve_candidates(candidates)

        if branch is None:
            if solver_gave_up:
                # Some branch might still be feasible, the solver just could not tell.
                logger.info(f'[SYMBOLIC EXPLORATION] No solution found, but the solver gave up on some branch')
                self.state.verdict = Verdict.UNKNOWN
            else:
                logger.info(f'[SYMBOLIC EXPLORATION] No symbolic branch found or UNSAT')
                self.state.verdict = Verdict.SAFE
            return Action.REPORTVERDICT

        logger.info(f'[SYMBOLIC EXPLORATION] Found solution for branch {branch.id} {"skipped" if branch.skipped is None else "branched"}')
        # remember which branch we want to explore
        self.state.branch_to_explore = branch
        self.state.solution_is_guess = Database.instance().is_approximate_trace(ENDPOINT_ID, branch.trace_id)

        sol_viz = [f'{key}: {val["plain_value"]}' for key, val in sol.items()]
        logger.info(f'[SYMBOLIC EXPLORATION] SAT solution: {sol}')
        logger.info(f'[SYMBOLIC EXPLORATION] Found new solution: {sol_viz}')
        self.symbolicStorage.register_vars(branch.inputs)
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
        self.budget.start()

        try:
            verdict = self.run_testcase(java_path=self.args.java_path, agentpath=self.args.agent, configpath=self.args.config, z3path=self.args.z3dir, port=self.args.port, cp=self.args.classpath)
        finally:
            # Exploration can finish before the pre-analysis: stop it, so no extractor outlives us.
            self.sa.cancel()
            if self.sa.duration is not None:
                TimingManager.instance().record_static_analysis_time(self.sa.duration, blocking=not self.args.parallel_sa)
        
        if (verdict == Verdict.SAFE) and Database.instance().get_tree(ENDPOINT_ID).symbolic_context_loss:
            logger.warning(f'[SVCOMP] Found symbolic context loss')
            verdict = Verdict.UNKNOWN

        if (verdict == Verdict.SAFE) and Database.instance().get_tree(ENDPOINT_ID).symbolic_precision_loss:
            logger.warning(f'[SVCOMP] Found symbolic precision loss')
            verdict = Verdict.UNKNOWN

        if (verdict == Verdict.SAFE) and any(e.get('kind') == 'unexpected_divergence'
                                              for e in Database.instance().get_tree(ENDPOINT_ID).execution_errors):
            logger.warning(f'[SVCOMP] A solution did not reach the side of the branch it was solved for')
            verdict = Verdict.UNKNOWN

        if (verdict == Verdict.SAFE) and any(e.get('kind') == 'sa_desync'
                                              for e in Database.instance().get_tree(ENDPOINT_ID).execution_errors):
            logger.warning(f'[SVCOMP] The SA walk fell out of step with the execution (see [DFS] SA desync)')
            verdict = Verdict.UNKNOWN

        if (verdict == Verdict.SAFE) and any(e.get('kind') in CUT_SHORT_ERRORS
                                              for e in Database.instance().get_tree(ENDPOINT_ID).execution_errors):
            logger.warning(f'[SVCOMP] An execution was cut short by a memory limit, its path is unexplored')
            verdict = Verdict.UNKNOWN

        if (verdict == Verdict.SAFE) and Database.instance().get_tree(ENDPOINT_ID).uncaught_exceptions > 0:
            logger.warning(f'[SVCOMP] Found uncaught exceptions during symbolic execution')
            verdict = Verdict.UNKNOWN

        if (verdict == Verdict.VIOLATION) and Database.instance().get_tree(ENDPOINT_ID).reference_semantic_change:
            logger.warning(f'[SVCOMP] Found reference semantic change (user-de-interned strings compared via Objects.equals) - downgrading VIOLATION to UNKNOWN')
            verdict = Verdict.UNKNOWN

        if verdict == Verdict.NO_SYMBOLIC_VARS:
            verdict = Verdict.SAFE
        with self.verdict_lock:
            if self.verdict_reported:
                return
            self.verdict_reported = True
            verdict_logger.info(f'[VERDICT {self.verification_category.value}] {verdict.value}')
        self.budget.stop()
        logger.info(f'[EXPLORER] Symbolic execution terminated after {self.round_idx} iterations')

        # The witness is written after the verdict and never affects it
        if verdict == Verdict.VIOLATION:
            self.write_witness()

        # Stop total timing (don't print summary here - execution.py will print complete summary with witness timing)
        TimingManager.instance().stop_total_timer()

        # Save timing data to file in the log directory
        log_dir = self.args.logdir if hasattr(self.args, 'logdir') else 'logs'
        timing_file = os.path.join(log_dir, 'timing_data.json')
        TimingManager.instance().save_to_file(Path(timing_file))

        # Write the consolidated per-testcase statistics (verdict, soundness flags, missing
        # invocations and the context-loss subset) so the analysis can rely on structured data.
        self.write_stats(verdict)

        self.kill_current_process()

    def write_stats(self, verdict):
        stats_file = os.path.join(self.args.logdir if hasattr(self.args, 'logdir') else 'logs', 'stats.json')
        write_testcase_stats(Path(stats_file), verdict, self.verification_category, Database.instance().get_tree(ENDPOINT_ID),
                             self.round_idx, self.nr_solver_calls, self.sa.stats(self.sa_adopted_round),
                             self.solver_limit_hits, resources=self.budget.stats())

    def end_run_now(self, why: str):
        """Called by the resource budget's monitor when the run must end at once: reports UNKNOWN
        (unless the main thread reported a verdict already) and exits. The main thread is stuck in
        work the budget cannot stop, or the explorer alone is too large."""
        with self.verdict_lock:
            if self.verdict_reported:
                return
            self.verdict_reported = True
            logger.warning(f'[SVCOMP] Ending the run with UNKNOWN: {why}')
            verdict_logger.info(f'[VERDICT {self.verification_category.value}] {Verdict.UNKNOWN.value}')
        try:
            self.write_stats(Verdict.UNKNOWN)
        except Exception as e:  # the tree may be changing under us
            logger.error(f'[SVCOMP] Could not write the statistics: {e}')
        for handler in logger.handlers + verdict_logger.handlers:
            handler.flush()
        sys.stdout.flush()
        for pid in descendants(os.getpid()):  # the executor or the pre-analysis, if one is running
            try:
                os.kill(pid, signal.SIGKILL)
            except OSError:
                pass
        os._exit(0)
        
        
    def write_witness(self):
        """Writes witness.yml/witness.graphml for the VIOLATION verdict from the trace of the violating round."""
        input_files = getattr(self.args, 'witness_input', None) or []
        property_file = getattr(self.args, 'witness_property', None)
        if not input_files or not property_file:
            logger.warning('[WITNESS] No task input files/property file given (--witness-input/--witness-property), no witness is written')
            return
        write_violation_witness(
            Database.instance().get_witness_data(ENDPOINT_ID),
            input_files=input_files,
            property_file=property_file,
            out_dir=getattr(self.args, 'witness_dir', None) or 'witness',
            producer_version=getattr(self.args, 'producer_version', None) or 'unknown',
            base_dir=getattr(self.args, 'witness_base_dir', None) or None)

    def kill_current_process(self):
        pid = os.getpid()  
        os.kill(pid, signal.SIGTERM)  # Send termination signal
        # os.kill(pid, signal.SIGKILL)  # Use this for a more forceful kill if needed


