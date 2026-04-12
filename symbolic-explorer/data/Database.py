import threading
import copy
from typing import List

from data.BinaryExecutionTree.Tree import Tree
from data.Solution.Solution import Solution
from driver.SymbolicStorage import SymbolicVar
from data.trace.Input import Input
from data.trace.UF import UF

lock = threading.Lock()


class Database:
    __instance = None

    @staticmethod
    def instance():

        if Database.__instance is None:
            Database.__instance = Database()

        return Database.__instance

    def __init__(self):

        lock.acquire()

        if Database.__instance:
            lock.release()
            raise Exception("Attempting to create a new singleton instance.")

        self.tree = {}
        self.constraints = {}
        self.solutions = {}
        self.new_solutions = []
        self.unsat_branches = []
        self.endpoints = list()
        self.traces = list()
        self.instr_coverage = {'total_executed': {}, 'total_instr_count': 0, 'endpoints': {}}
        self.branch_coverage = {'total_executed': {}, 'total_branch_instr_count': 0}
        self.violations = {}

        lock.release()

    def reset(self):

        lock.acquire()

        self.tree = {}
        self.constraints = {}
        self.solutions = {}
        self.new_solutions = []
        self.unsat_branches = []
        self.endpoints = list()
        self.traces = list()
        self.instr_coverage = {'total_executed': {}, 'total_instr_count': 0, 'endpoints': {}}
        self.branch_coverage = {'total_executed': {}, 'total_branch_instr_count': 0}
        self.violations = {}

        lock.release()

    def add_constraints(self, trace_id, constraints):
        lock.acquire()
        self.constraints[trace_id] = constraints
        lock.release()

    def get_constraints(self, trace_id):
        lock.acquire()
        ret = copy.deepcopy(self.constraints[trace_id])
        lock.release()
        return ret

    def add_violation(self, endpoint_id: int, sym_vars: List[SymbolicVar]):
        lock.acquire()
        self._add_violation(endpoint_id, sym_vars)
        lock.release()

    def _add_violation(self, endpoint_id: int, sym_vars: List[SymbolicVar]):
        endpoint_id = str(endpoint_id)
        if endpoint_id not in self.violations:
            self.violations[endpoint_id] = [sym_vars]
        else:
            self.violations[endpoint_id].append(sym_vars)

    def get_violations(self, endpoint_id: int):
        lock.acquire()
        if endpoint_id not in self.violations:
            return []
        violations = copy.deepcopy(self.violations[endpoint_id])
        lock.release()
        return violations

    def add_endpoint(self, endpoint_id):
        lock.acquire()
        self._add_endpoint(endpoint_id)
        lock.release()

    def _add_endpoint(self, endpoint_id):
        endpoint_id = str(endpoint_id)
        if endpoint_id not in self.tree:
            self.endpoints.append(endpoint_id)
            self.tree[endpoint_id] = Tree(endpoint_id=endpoint_id)

    def get_endpoints(self):
        lock.acquire()
        endpoints = copy.deepcopy(self.endpoints)
        lock.release()
        return endpoints

    def add_trace(self, endpoint_id, trace_id, trace, inputs: List[Input], ufs: List[UF], symbolic_context_loss, symbolic_precision_loss, reference_semantic_change=False):
        endpoint_id = str(endpoint_id)

        lock.acquire()

        self._add_endpoint(endpoint_id)
        self.traces.append(trace_id)
        self.tree[endpoint_id].add(trace, inputs, ufs)
        self.tree[endpoint_id].record_inputs(inputs)
        self.tree[endpoint_id].record_ufs(ufs)
        self.tree[endpoint_id].record_context_loss() if symbolic_context_loss else None
        self.tree[endpoint_id].record_precision_loss() if symbolic_precision_loss else None
        self.tree[endpoint_id].record_reference_semantic_change() if reference_semantic_change else None

        lock.release()

    def get_traces(self):
        lock.acquire()
        ret = copy.deepcopy(self.traces)
        lock.release()
        return ret

    def get_tree(self, endpoint_id):
        endpoint_id = str(endpoint_id)
        lock.acquire()
        ret = copy.deepcopy(self.tree[endpoint_id])
        lock.release()
        return ret
    
    def record_uncaught_exception(self, endpoint_id):
        endpoint_id = str(endpoint_id)
        lock.acquire()
        self.tree[endpoint_id].uncaught_exceptions += 1
        lock.release()

    def add_solution(self, branch_id, sol, inputs, endpoint_id):
        lock.acquire()
        self.solutions[branch_id] = Solution(sol=sol, inputs=inputs, endpoint_id=endpoint_id)
        self.new_solutions.append(branch_id)
        lock.release()

    def get_solutions(self):
        lock.acquire()
        ret = copy.deepcopy(self.solutions)
        lock.release()
        return ret

    def consume_new_solution(self, branch_id):
        # raise Exception("CARFUL! Switched from node.id to node.gid, untested here")
        lock.acquire()

        solution = copy.deepcopy(self.solutions[branch_id])
        self.new_solutions.remove(branch_id)

        lock.release()

        return solution

    def get_new_solutions(self):
        lock.acquire()
        ret = copy.deepcopy(self.new_solutions)
        lock.release()
        return ret

    def clear_new_solutions(self):
        lock.acquire()
        self.new_solutions.clear()
        lock.release()

    def add_unsat_branch(self, branch_id):
        lock.acquire()
        self.unsat_branches.append(branch_id)
        lock.release()

    def get_unsat_branches(self):
        lock.acquire()
        ret = copy.deepcopy(self.unsat_branches)
        lock.release()
        return ret

    def add_branch_coverage(self, ids, total_branch_instr_count):
        lock.acquire()

        for _id in ids:
            if _id in self.branch_coverage['total_executed']:
                self.branch_coverage['total_executed'][_id] += 1
            else:
                self.branch_coverage['total_executed'][_id] = 1

        self.branch_coverage['total_branch_instr_count'] = total_branch_instr_count

        lock.release()

    def get_branch_coverage(self):
        lock.acquire()

        ret = {
            'executed_branches': len(self.branch_coverage['total_executed']),
            'total_branch_count': self.branch_coverage['total_branch_instr_count']
        }

        lock.release()

        return ret

    def add_instr_coverage(self, endpoint_id, ids, total_instr_count):

        lock.acquire()

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

        lock.release()

    def get_total_instr_coverage(self):

        lock.acquire()

        ret = {
            'executed_instr': len(self.instr_coverage['total_executed']),
            'total_instr_count': self.instr_coverage['total_instr_count']
        }

        lock.release()

        return ret

    def get_endpoint_instr_coverage(self, endpoint_id):
        lock.acquire()

        if endpoint_id in self.instr_coverage['endpoints'].keys():
            ret = len(self.instr_coverage['endpoints'][endpoint_id])
        else:
            ret = 0

        lock.release()

        return ret

