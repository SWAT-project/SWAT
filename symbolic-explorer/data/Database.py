import threading
import copy
from typing import Any, List, Union

from data.BinaryExecutionTree.Tree import Tree
from data.Solution.Solution import Solution
from driver.SymbolicStorage import SymbolicVar
from data.trace.Input import Input
from data.trace.UF import UF
from data.trace.Special import Special
from data.trace.Branch import Branch

lock = threading.Lock()


class Database:
    __instance = None

    @staticmethod
    def instance():

        if Database.__instance is None:
            Database.__instance = Database()

        return Database.__instance

    def __init__(self):

        with lock:
            if Database.__instance:
                raise Exception("Attempting to create a new singleton instance.")

            self.tree = {}
            self.constraints = {}
            self.solutions = {}
            self.new_solutions = []
            self.unsat_branches = []
            # Branches whose solutions repeatedly did not lead the run to them, given up on (by gid)
            self.retired_branches = set()
            self.endpoints = list()
            self.traces: list[list[Branch | Special]] = list()
            self.instr_coverage = {'total_executed': {}, 'total_instr_count': 0, 'endpoints': {}}
            self.branch_coverage = {'total_executed': {}, 'total_branch_instr_count': 0}
            self.violations = {}
            # Witness data (nondet returns, uncaught throwable) of the latest trace per endpoint
            self.witness_data = {}
            self.optimize_solutions = False  # Whether to use Z3 Optimize() to minimize values
            self.log_smt_formulas = False  # Whether to log SMT formulas and models to disk for debugging
            # Whether queries with floating-point terms go to Z3's qffp tactic first (see Z3Handler.check_query)
            self.fp_tactic = True
            self.prefer_short_strings = False  # Whether to shrink the string inputs of a model (Z3Handler.shorten_strings)

    def reset(self):

        with lock:
            self.tree = {}
            self.constraints = {}
            self.solutions = {}
            self.new_solutions = []
            self.unsat_branches = []
            # Branches whose solutions repeatedly did not lead the run to them, given up on (by gid)
            self.retired_branches = set()
            self.endpoints = list()
            self.traces: list[list[Branch | Special]] = list()
            self.instr_coverage = {'total_executed': {}, 'total_instr_count': 0, 'endpoints': {}}
            self.branch_coverage = {'total_executed': {}, 'total_branch_instr_count': 0}
            self.violations = {}
            self.witness_data = {}

    def add_constraints(self, trace_id: str, constraints: dict):
        with lock:
            self.constraints[trace_id] = constraints

    def get_constraints(self, trace_id: str):
        with lock:
            ret = copy.deepcopy(self.constraints[trace_id])
        return ret

    def add_violation(self, endpoint_id: int, sym_vars: List[SymbolicVar]):
        with lock:
            self._add_violation(endpoint_id, sym_vars)

    def _add_violation(self, endpoint_id: str | int, sym_vars: List[SymbolicVar]):
        endpoint_id = str(endpoint_id)
        if endpoint_id not in self.violations:
            self.violations[endpoint_id] = [sym_vars]
        else:
            self.violations[endpoint_id].append(sym_vars)

    def get_violations(self, endpoint_id: int):
        with lock:
            if endpoint_id not in self.violations:
                return []
            violations = copy.deepcopy(self.violations[endpoint_id])
        return violations

    def add_endpoint(self, endpoint_id: Union[str, int]):
        with lock:
            self._add_endpoint(endpoint_id)

    def _add_endpoint(self, endpoint_id: Union[str, int]):
        endpoint_id = str(endpoint_id)
        if endpoint_id not in self.tree:
            self.endpoints.append(endpoint_id)
            self.tree[endpoint_id] = Tree(endpoint_id=endpoint_id)

    def get_endpoints(self):
        with lock:
            endpoints = copy.deepcopy(self.endpoints)
        return endpoints

    def add_trace(self, endpoint_id: Union[str, int], trace_id: str, trace: list[Branch | Special], inputs: List[Input], ufs: List[UF],
                  symbolic_context_loss: bool, symbolic_precision_loss: bool,
                  reference_semantic_change: bool = False, missing_invocations: list = None,
                  witness_data: dict = None,
                  approximate_model: bool = False,
                  stack_overflow: bool = False):
        endpoint_id = str(endpoint_id)

        with lock:
            self._add_endpoint(endpoint_id)
            tree = self.tree[endpoint_id]
            # The soundness flags go first: they must hold even if adding the trace to the tree fails.
            tree.record_context_loss() if symbolic_context_loss else None
            tree.record_precision_loss() if symbolic_precision_loss else None
            tree.record_reference_semantic_change() if reference_semantic_change else None
            tree.approximate_traces.add(trace_id) if approximate_model else None
            tree.stack_overflow_traces.add(trace_id) if stack_overflow else None
            tree.record_missing_invocations(missing_invocations)
            tree.record_inputs(inputs)
            tree.record_ufs(ufs)
            self.traces.append(trace)
            if witness_data is not None:
                self.witness_data[endpoint_id] = witness_data
            tree.add(trace, inputs, ufs)

    def record_trace_ingest_failure(self, endpoint_id: Union[str, int], message: str,
                                    reference_semantic_change: bool = False, trace_id: str = None,
                                    stack_overflow: bool = False):
        """Records that a trace the executor sent could not be added: the tree lacks its path, so
        the search on it is incomplete. A reference semantic change it reported still counts, and so
        does a stack overflow: the trace becomes the latest one, so that the driver does not report
        a violation of that execution (see SVCompDriver.round_has_stack_overflow)."""
        endpoint_id = str(endpoint_id)
        with lock:
            self._add_endpoint(endpoint_id)
            self.tree[endpoint_id].record_execution_error('trace_ingest_failed', message)
            if reference_semantic_change:
                self.tree[endpoint_id].record_reference_semantic_change()
            if stack_overflow and trace_id is not None:
                self.tree[endpoint_id].stack_overflow_traces.add(trace_id)
                self.witness_data[endpoint_id] = {'trace_id': trace_id, 'nondet_returns': [], 'violation': None}

    def trace_count(self) -> int:
        """How many traces arrived so far."""
        with lock:
            return len(self.traces)

    def get_traces(self):
        with lock:
            ret = copy.deepcopy(self.traces)
        return ret

    def trace_count(self) -> int:
        """The number of traces received so far, over all endpoints."""
        lock.acquire()
        ret = len(self.traces)
        lock.release()
        return ret

    def get_trace(self, idx: int):
        with lock:
            ret = copy.deepcopy(self.traces[idx])
        return ret

    def get_tree(self, endpoint_id: Union[str, int]) -> Tree:
        endpoint_id = str(endpoint_id)
        with lock:
            ret = copy.deepcopy(self.tree[endpoint_id])
        return ret
    
    def visit_tree(self, endpoint_id: Union[str, int], visit):
        """Returns visit(tree, solved, unsat) for the endpoint's tree, with the gids of the solved
        (or retired) and of the unsat branches, all under the lock and without copying: a copy of
        the whole tree in every round costs more than the walk itself. visit must not keep the
        sets, and must not call back into the Database (the lock is held)."""
        endpoint_id = str(endpoint_id)
        with lock:
            return visit(self.tree[endpoint_id], self.solutions.keys() | self.retired_branches,
                         set(self.unsat_branches))

    def count_symbolic_vars(self, endpoint_id: Union[str, int]) -> int:
        endpoint_id = str(endpoint_id)
        with lock:
            ret = len(self.tree[endpoint_id].symbolic_vars)
        return ret

    def get_execution_errors(self, endpoint_id: Union[str, int], kind: str) -> list[dict]:
        """The execution errors of the endpoint's tree of the given kind, without copying the tree."""
        endpoint_id = str(endpoint_id)
        with lock:
            ret = [dict(e) for e in self.tree[endpoint_id].execution_errors if e.get('kind') == kind]
        return ret

    def get_witness_data(self, endpoint_id: Union[str, int]) -> dict | None:
        """The witness data of the latest trace of the endpoint (None if no trace arrived since the last clear)."""
        endpoint_id = str(endpoint_id)
        with lock:
            ret = copy.deepcopy(self.witness_data.get(endpoint_id))
        return ret

    def clear_witness_data(self, endpoint_id: Union[str, int]):
        """Forgets the witness data of the endpoint, so that only a trace of the next execution can provide it."""
        endpoint_id = str(endpoint_id)
        with lock:
            self.witness_data.pop(endpoint_id, None)

    def record_uncaught_exception(self, endpoint_id: Union[str, int]):
        endpoint_id = str(endpoint_id)
        with lock:
            self.tree[endpoint_id].uncaught_exceptions += 1

    def is_approximate_trace(self, endpoint_id: Union[str, int], trace_id: str) -> bool:
        """Whether the trace used an approximate model, so solutions built from it are guesses."""
        endpoint_id = str(endpoint_id)
        with lock:
            ret = trace_id in self.tree[endpoint_id].approximate_traces
        return ret

    def is_stack_overflow_trace(self, endpoint_id: Union[str, int], trace_id: str) -> bool:
        """Whether a StackOverflowError reached the target's code in the execution of the trace."""
        endpoint_id = str(endpoint_id)
        with lock:
            ret = trace_id in self.tree[endpoint_id].stack_overflow_traces
        return ret

    def record_expected_divergence(self, endpoint_id: Union[str, int]):
        endpoint_id = str(endpoint_id)
        with lock:
            self.tree[endpoint_id].expected_divergences += 1

    def record_execution_error(self, endpoint_id: Union[str, int], kind: str, message: str):
        endpoint_id = str(endpoint_id)
        with lock:
            self._add_endpoint(endpoint_id)
            self.tree[endpoint_id].record_execution_error(kind, message)

    def add_solution(self, branch_id: int, sol: Any, inputs: List[Input], endpoint_id: Union[str, int]):
        with lock:
            self.solutions[branch_id] = Solution(sol=sol, inputs=inputs, endpoint_id=endpoint_id)
            self.new_solutions.append(branch_id)

    def get_solutions(self):
        with lock:
            ret = copy.deepcopy(self.solutions)
        return ret

    def consume_new_solution(self, branch_id: int):
        # raise Exception("CAREFUL! Switched from node.id to node.gid, untested here")
        with lock:
            solution = copy.deepcopy(self.solutions[branch_id])
            self.new_solutions.remove(branch_id)


        return solution

    def get_new_solutions(self):
        with lock:
            ret = copy.deepcopy(self.new_solutions)
        return ret

    def clear_new_solutions(self):
        with lock:
            self.new_solutions.clear()

    def drop_solution(self, branch_id: int):
        """Forgets the solution of the branch, so that it becomes a candidate again."""
        with lock:
            self.solutions.pop(branch_id, None)
            if branch_id in self.new_solutions:
                self.new_solutions.remove(branch_id)

    def retire_branch(self, branch_id: int):
        """Takes the branch out of the candidates for good, without claiming it is infeasible."""
        with lock:
            self.retired_branches.add(branch_id)

    def get_retired_branches(self):
        with lock:
            ret = set(self.retired_branches)
        return ret

    def add_unsat_branch(self, branch_id: int):
        with lock:
            self.unsat_branches.append(branch_id)

    def get_unsat_branches(self):
        with lock:
            ret = copy.deepcopy(self.unsat_branches)
        return ret

    def add_branch_coverage(self, ids: List[int], total_branch_instr_count: int):
        with lock:
            for _id in ids:
                if _id in self.branch_coverage['total_executed']:
                    self.branch_coverage['total_executed'][_id] += 1
                else:
                    self.branch_coverage['total_executed'][_id] = 1

            self.branch_coverage['total_branch_instr_count'] = total_branch_instr_count

    def get_branch_coverage(self):
        with lock:
            ret = {
                'executed_branches': len(self.branch_coverage['total_executed']),
                'total_branch_count': self.branch_coverage['total_branch_instr_count']
            }


        return ret

    def add_instr_coverage(self, endpoint_id: Union[str, int], ids: List[int], total_instr_count: int):

        with lock:
            if endpoint_id not in self.instr_coverage['endpoints'].keys():
                self.instr_coverage['endpoints'][endpoint_id] = {}

            for _id in ids:
                if _id in self.instr_coverage['total_executed']:
                    self.instr_coverage['total_executed'][_id] += 1
                else:
                    self.instr_coverage['total_executed'][_id] = 1

                if _id in self.instr_coverage:
                    self.instr_coverage['endpoints'][endpoint_id][_id] += 1
                else:
                    self.instr_coverage['endpoints'][endpoint_id][_id] = 1

            self.instr_coverage['total_instr_count'] = total_instr_count

    def get_total_instr_coverage(self):

        with lock:
            ret = {
                'executed_instr': len(self.instr_coverage['total_executed']),
                'total_instr_count': self.instr_coverage['total_instr_count']
            }


        return ret

    def get_endpoint_instr_coverage(self, endpoint_id: Union[str, int]):
        with lock:
            if endpoint_id in self.instr_coverage['endpoints'].keys():
                ret = len(self.instr_coverage['endpoints'][endpoint_id])
            else:
                ret = 0


        return ret

