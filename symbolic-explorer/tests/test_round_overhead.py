"""
Unit tests for the per-round bookkeeping of the explorer: branch selection walks the Database's
tree without copying it, and the answers it reuses across rounds are the ones a fresh walk gives.

Run with the harness venv, from symbolic-explorer/:
    ../targets/sv-comp/scripts/.venv/bin/python3 -m unittest tests.test_round_overhead -v
"""

import copy
import os
import sys
import unittest
from types import SimpleNamespace
from unittest import mock

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from data.Database import Database
from data.trace.Branch import Branch
from data.trace.Input import Input
from driver import SVCompDriver as svcomp
from strategy.StrategyService import StrategyService

DECL = '(declare-fun I_0 () (_ BitVec 32))\n'


def branch(id, trace_id, taken, constraint='(assert (bvsgt I_0 #x00000000))'):
    return Branch(id, trace_id, taken, DECL + constraint)


class SelectBranchTest(unittest.TestCase):

    def setUp(self):
        Database.instance().reset()
        self.inputs = [Input('I_0', '0', 'int', DECL + '(assert true)', DECL + '(assert true)')]

    def add(self, trace_id, trace):
        Database.instance().add_trace(0, trace_id, trace, self.inputs, [], False, False)

    def test_candidates_are_the_open_sides_in_dfs_order(self):
        self.add('t1', [branch(1, 't1', True), branch(2, 't1', False), branch(3, 't1', True)])
        self.add('t2', [branch(1, 't2', True), branch(2, 't2', True)])
        nodes = StrategyService.select_branch(endpoint_id=0)
        self.assertEqual([n.id for n in nodes], [1, 3])  # 2 has both sides now

    def test_solved_and_unsat_branches_are_left_out(self):
        self.add('t1', [branch(1, 't1', True), branch(2, 't1', False)])
        first, second = StrategyService.select_branch(endpoint_id=0)
        Database.instance().add_unsat_branch(first.gid)
        Database.instance().add_solution(second.gid, {}, self.inputs, 0)
        self.assertEqual(StrategyService.select_branch(endpoint_id=0), [])

    def test_the_tree_is_not_copied(self):
        self.add('t1', [branch(1, 't1', True)])
        with mock.patch.object(copy, 'deepcopy', side_effect=AssertionError('copied')):
            nodes = StrategyService.select_branch(endpoint_id=0)
        self.assertEqual([n.id for n in nodes], [1])

    def test_symbolic_check_is_remembered_per_node(self):
        self.add('t1', [branch(1, 't1', True), branch(2, 't1', False, '(assert (= #x00000001 #x00000001))')])
        nodes = StrategyService.select_branch(endpoint_id=0)
        first = [StrategyService.is_symbolic_branch(n) for n in nodes]
        self.assertEqual(first, [True, False])
        with mock.patch('solver.ConstraintCache.get_constraint_cache', side_effect=AssertionError('asked again')):
            self.assertEqual([StrategyService.is_symbolic_branch(n) for n in nodes], first)


class BranchToExploreTest(unittest.TestCase):
    """The driver keeps the target as it was when it was solved: the tree's node gets its other
    side as soon as the next trace arrives, and check_divergence must not take that as taken."""

    def test_target_keeps_its_open_side(self):
        Database.instance().reset()
        inputs = [Input('I_0', '0', 'int', DECL + '(assert true)', DECL + '(assert true)')]
        Database.instance().add_trace(0, 't1', [branch(1, 't1', False)], inputs, [], False, False)
        node, = StrategyService.select_branch(endpoint_id=0)
        driver = svcomp.SVCompDriver.__new__(svcomp.SVCompDriver)
        driver.state = svcomp.State()
        driver.symbolicStorage = svcomp.SymbolicStorage()
        driver.pending_projected = None
        with mock.patch.object(StrategyService, 'select_branch', return_value=[node]), \
             mock.patch.object(svcomp.SVCompDriver, 'solve_candidates', return_value=(node, {}, False)):
            driver.sa_graph = SimpleNamespace(entry_node=None)
            driver.sa = SimpleNamespace(take_graph=lambda: None)
            driver.args = SimpleNamespace(sa_rank_distance=False)
            driver.retrieve_solution()
        # The next run takes the branched side, as solved for.
        Database.instance().add_trace(0, 't2', [branch(1, 't2', True)], inputs, [], False, False)
        self.assertIsNotNone(node.branched)
        with self.assertNoLogs(svcomp.logger, level='ERROR'):
            driver.check_divergence(Database.instance().get_trace(-1))


class ExtractVariablesTest(unittest.TestCase):
    """A formula is a DAG: with shared subterms it is exponentially larger as a tree, and it can
    be deeper than the recursion limit."""

    def test_shared_subterms_are_visited_once(self):
        from z3 import BitVec, Context
        from solver.ConstraintManager import ConstraintManager
        ctx = Context()
        x, y = BitVec('x', 64, ctx), BitVec('y', 64, ctx)
        e = x
        for _ in range(200):  # 2^200 paths through the DAG
            e = e + e
        mgr = ConstraintManager(shared_context=ctx)
        self.assertEqual(mgr._extract_variables(e == y), {'x', 'y'})

    def test_deep_terms(self):
        from z3 import BitVec, Context
        from solver.ConstraintManager import ConstraintManager
        ctx = Context()
        x = BitVec('x', 32, ctx)
        e = x
        for i in range(5000):
            e = e - (i % 7)
        mgr = ConstraintManager(shared_context=ctx)
        self.assertEqual(mgr._extract_variables(e == 0), {'x'})


if __name__ == '__main__':
    unittest.main()
