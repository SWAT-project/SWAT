import threading
import copy

from data.BinaryExecutionTree.Tree import Tree
from data.Solution.Solution import Solution
from driver.SymbolicStorage import SymbolicVar

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
        self.coverage = {}
        self.violations = {}

        lock.release()

    def add_violation(self, endpoint_id: int, sym_vars: [SymbolicVar]):
        lock.acquire()
        self._add_violation(endpoint_id, sym_vars)
        lock.release()

    def _add_violation(self, endpoint_id: int, sym_vars: [SymbolicVar]):
        endpoint_id = str(endpoint_id)
        if endpoint_id not in self.violations:
            self.violations[endpoint_id] = [sym_vars]
        else:
            self.violations[endpoint_id].append(sym_vars)

    def add_endpoint(self, endpoint_id):
        lock.acquire()
        self._add_endpoint(endpoint_id)
        lock.release()

    def _add_endpoint(self, endpoint_id):
        endpoint_id = str(endpoint_id)
        if endpoint_id not in self.tree:
            self.endpoints.append(endpoint_id)
            self.tree[endpoint_id] = Tree(endpoint_id=endpoint_id)

    def get_endpoint_ids(self):
        lock.acquire()
        endpoints = self._get_endpoint_ids().copy()
        lock.release()
        return endpoints

    def _get_endpoint_ids(self):
        return self.endpoints

    def add_trace(self, endpoint_id, trace_id, trace, inputs):
        endpoint_id = str(endpoint_id)

        lock.acquire()

        self._add_endpoint(endpoint_id)
        self.traces.append(trace_id)
        self.tree[endpoint_id].add(trace, inputs)

        lock.release()

    def get_tree(self, endpoint_id):
        endpoint_id = str(endpoint_id)

        lock.acquire()

        ret = copy.deepcopy(self.tree[endpoint_id])

        lock.release()

        return ret

    def add_solution(self, branch_id, sol, inputs):

        lock.acquire()

        self.solutions[branch_id] = Solution(sol, inputs)
        self.new_solutions.append(branch_id)

        lock.release()

    def get_solutions(self):

        lock.acquire()

        ret = copy.deepcopy(self.solutions)

        lock.release()

        return ret

    def add_unsat_branch(self, branch_id):

        lock.acquire()

        self.unsat_branches.append(branch_id)

        lock.release()

    def get_unsat_branches(self):

        lock.acquire()

        ret = copy.deepcopy(self.unsat_branches)

        lock.release()

        return ret
