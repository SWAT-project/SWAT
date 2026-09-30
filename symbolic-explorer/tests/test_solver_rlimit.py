"""
Unit tests for SVCompDriver.solve_candidates: branches that run out of their solver resource limit
are retried with a growing limit after all cheaper queries, and an undecided branch rules out SAFE.

Needs the explorer's dependencies, so run it with the harness venv, from symbolic-explorer/:
    ../targets/sv-comp/scripts/.venv/bin/python3 -m unittest tests.test_solver_rlimit -v
"""

import os
import sys
import unittest
from types import SimpleNamespace
from unittest import mock

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from driver import SVCompDriver as svcomp
from solver.SolverHandler import SATResult

BASE = 1000


def branch(gid):
    return SimpleNamespace(gid=gid, id=gid)


class FakeSolver:
    """Decides branch gid when given at least needed[gid] resources, with verdict results[gid]."""

    def __init__(self, needed, results):
        self.needed, self.results, self.calls = needed, results, []

    def __call__(self, b, solver_timeout_ms=None, solver_rlimit=None):
        self.calls.append((b.gid, solver_rlimit))
        if solver_rlimit is not None and solver_rlimit < self.needed.get(b.gid, 0):
            return SATResult.LIMIT, {}
        result = self.results[b.gid]
        return result, ({'x': 1} if result == SATResult.SAT else {})


class SolveCandidatesTest(unittest.TestCase):

    def driver(self, rlimit=BASE):
        d = svcomp.SVCompDriver.__new__(svcomp.SVCompDriver)
        d.args = SimpleNamespace(solver_rlimit=rlimit)
        d.nr_solver_calls = 0
        d.solver_limit_hits = 0
        d.rlimit_level = {}
        return d

    def solve(self, d, candidates, solver):
        with mock.patch.object(svcomp.StrategyService, 'solve_branch', side_effect=solver):
            return d.solve_candidates(candidates)

    def test_cheap_queries_come_before_retries(self):
        # 1 needs 20x the base limit, 2 is UNSAT, 3 is SAT: 3 is found before 1 is retried.
        d = self.driver()
        s = FakeSolver({1: 20 * BASE}, {1: SATResult.SAT, 2: SATResult.UNSAT, 3: SATResult.SAT})
        found, sol, gave_up = self.solve(d, [branch(1), branch(2), branch(3)], s)
        self.assertEqual(found.gid, 3)
        self.assertEqual(s.calls, [(1, BASE), (2, BASE), (3, BASE)])
        self.assertEqual(d.rlimit_level, {1: 1})

    def test_limit_grows_while_several_branches_compete(self):
        # 1 and 2 both need 20x: they alternate at growing limits until 1 is decided.
        d = self.driver()
        s = FakeSolver({1: 20 * BASE, 2: 20 * BASE}, {1: SATResult.SAT, 2: SATResult.SAT})
        found, _, gave_up = self.solve(d, [branch(1), branch(2)], s)
        self.assertEqual(found.gid, 1)
        self.assertEqual(s.calls, [(1, BASE), (2, BASE), (1, 5 * BASE), (2, 5 * BASE), (1, 25 * BASE)])
        self.assertEqual(d.solver_limit_hits, 4)
        self.assertFalse(gave_up)

    def test_lone_retry_is_unlimited(self):
        # Nothing else is left to go first, so the retry is not escalated step by step.
        d = self.driver()
        s = FakeSolver({1: 10**9}, {1: SATResult.SAT, 2: SATResult.UNSAT})
        found, _, _ = self.solve(d, [branch(1), branch(2)], s)
        self.assertEqual(found.gid, 1)
        self.assertEqual(s.calls, [(1, BASE), (2, BASE), (1, None)])

    def test_first_pass_is_limited_even_for_a_single_candidate(self):
        # A branch escalated in earlier rounds keeps its level; only retries go unlimited.
        d = self.driver()
        s = FakeSolver({}, {1: SATResult.SAT})
        self.solve(d, [branch(1)], s)
        self.assertEqual(s.calls, [(1, BASE)])

    def test_escalated_branches_go_last_in_later_rounds(self):
        d = self.driver()
        d.rlimit_level = {1: 2}
        s = FakeSolver({}, {1: SATResult.SAT, 2: SATResult.SAT})
        found, _, _ = self.solve(d, [branch(1), branch(2)], s)
        self.assertEqual(found.gid, 2)

    def test_all_unsat_allows_safe(self):
        d = self.driver()
        s = FakeSolver({1: 5 * BASE}, {1: SATResult.UNSAT, 2: SATResult.UNSAT})
        self.assertEqual(self.solve(d, [branch(1), branch(2)], s), (None, {}, False))

    def test_unknown_anywhere_rules_out_safe(self):
        # An UNKNOWN before the last branch used to be forgotten when the last one was UNSAT.
        d = self.driver()
        s = FakeSolver({}, {1: SATResult.UNKNOWN, 2: SATResult.UNSAT})
        self.assertEqual(self.solve(d, [branch(1), branch(2)], s), (None, {}, True))

    def test_limit_at_the_maximum_gives_up(self):
        d = self.driver()
        d.rlimit_level = {1: 40, 2: 40}  # far past MAX_RLIMIT
        s = FakeSolver({1: 2**40, 2: 2**40}, {1: SATResult.SAT, 2: SATResult.SAT})
        self.assertEqual(self.solve(d, [branch(1), branch(2)], s), (None, {}, True))
        self.assertEqual(s.calls, [(1, svcomp.MAX_RLIMIT), (2, svcomp.MAX_RLIMIT)])

    def test_without_flag_queries_are_unlimited(self):
        d = self.driver(rlimit=0)
        s = FakeSolver({1: 10**9}, {1: SATResult.SAT})
        found, _, _ = self.solve(d, [branch(1)], s)
        self.assertEqual(s.calls, [(1, None)])


if __name__ == '__main__':
    unittest.main()
