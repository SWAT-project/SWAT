"""
Unit tests for the handling of divergences in SVCompDriver: a run that does not take the side a
solution was solved for gets the branch re-solved with the failed solutions excluded, up to
divergence_retries times. Only then is the branch given up on, which is an internal error (and rules
out SAFE) unless the solution relied on an approximate model.

It imports the driver, which needs the explorer's dependencies (z3, fastapi, ...) that the system
Python lacks, so run it with the harness venv, from symbolic-explorer/:
    ../targets/sv-comp/scripts/.venv/bin/python3 -m unittest tests.test_divergence -v
"""

import os
import sys
import unittest
from types import SimpleNamespace
from unittest import mock

import z3

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from driver import SVCompDriver as svcomp
from solver.SolverHandler import SATResult, Z3Handler
from strategy.StrategyService import StrategyService


def trace_branch(id, has_branched):
    return SimpleNamespace(id=id, has_branched=has_branched)


def int_solution(ctx, **values):
    """A solution as Z3Handler.extract_and_encode_values builds it, for 32-bit int inputs."""
    return {name: {'encoded_value': str(v), 'plain_value': z3.BitVecVal(v, 32, ctx=ctx), 'index': name.split('_')[1]}
            for name, v in values.items()}


class DivergenceTestBase(unittest.TestCase):

    def setUp(self):
        self.tree = SimpleNamespace(expected_divergences=0, execution_errors=[])
        self.dropped, self.retired = [], set()
        self.approximate = False
        def record_expected_divergence(endpoint_id):
            self.tree.expected_divergences += 1
        def record_execution_error(endpoint_id, kind, message):
            self.tree.execution_errors.append({'kind': kind, 'message': message})
        db = SimpleNamespace(record_expected_divergence=record_expected_divergence,
                             record_execution_error=record_execution_error,
                             drop_solution=self.dropped.append,
                             retire_branch=self.retired.add,
                             is_approximate_trace=lambda endpoint_id, trace_id: self.approximate)
        patcher = mock.patch.object(svcomp.Database, 'instance', return_value=db)
        patcher.start()
        self.addCleanup(patcher.stop)
        self.driver = svcomp.SVCompDriver.__new__(svcomp.SVCompDriver)
        self.driver.args = SimpleNamespace(solver_rlimit=0)
        self.driver.state = svcomp.State()
        self.driver.divergence_retries = 2
        self.driver.failed_solutions = {}
        self.driver.given_up = []
        self.driver.divergence_retry_count = 0
        self.driver.nr_solver_calls = 0
        self.driver.solver_limit_hits = 0
        self.driver.rlimit_level = {}
        self.ctx = z3.Context()
        # Solved to take the branched side, which the tree has not explored yet.
        self.target = SimpleNamespace(id=7, gid=70, trace_id='t', branched=None, skipped=object(), parent=None)
        self.aim(self.target, {'I_0': 1})

    def aim(self, target, values):
        self.driver.state.branch_to_explore = target
        self.driver.state.solution = int_solution(self.ctx, **values)
        self.driver.state.solution_is_guess = self.approximate

    def check(self, trace):
        with self.assertLogs(svcomp.logger, level='INFO') as logs:
            self.driver.check_divergence(trace)
        return '\n'.join(logs.output)


class CheckDivergenceTest(DivergenceTestBase):

    def test_taking_the_predicted_side_is_silent(self):
        with self.assertNoLogs(svcomp.logger, level='INFO'):
            self.driver.check_divergence([trace_branch(7, True)])
        self.assertEqual(self.tree.expected_divergences, 0)
        self.assertEqual(self.driver.failed_solutions, {})

    def test_repeated_id_is_matched_by_position(self):
        # The target is at depth 2; the same id also occurs at depth 0 (e.g. a switch whose case
        # numbers wrap), taken the other way. Only the element at the target's depth counts.
        root = SimpleNamespace(parent=None)
        middle = SimpleNamespace(parent=root)
        self.target.parent = middle
        with self.assertNoLogs(svcomp.logger, level='INFO'):
            self.driver.check_divergence([trace_branch(7, False), trace_branch(3, True), trace_branch(7, True)])
        self.assertEqual(self.tree.execution_errors, [])

    def test_divergence_is_retried_first(self):
        out = self.check([trace_branch(7, False)])
        self.assertNotIn('SWAT Assertion failed', out)
        self.assertIn('Re-solving', out)
        # The branch becomes a candidate again, with the failed solution remembered.
        self.assertEqual(self.dropped, [70])
        self.assertEqual(len(self.driver.failed_solutions[70]), 1)
        self.assertEqual(self.tree.execution_errors, [])
        self.assertEqual(self.retired, set())
        self.assertEqual(self.driver.divergence_retry_count, 1)

    def test_exact_solutions_that_keep_diverging_are_an_error(self):
        for i in range(2):
            self.aim(self.target, {'I_0': i})
            self.check([trace_branch(8, True)])
        self.assertEqual(self.tree.execution_errors, [])
        self.aim(self.target, {'I_0': 2})
        out = self.check([trace_branch(7, False)])
        self.assertIn('ERROR', out)
        self.assertIn('SWAT Assertion failed', out)
        self.assertEqual(self.tree.expected_divergences, 0)
        # Recorded, so that the run cannot end SAFE, and out of the search for good
        self.assertEqual([e['kind'] for e in self.tree.execution_errors], ['unexpected_divergence'])
        self.assertEqual(self.retired, {70})
        self.assertEqual(self.driver.unreached_given_up_branches(), [self.target])
        self.assertEqual(len(self.driver.failed_solutions[70]), 3)

    def test_no_retries(self):
        self.driver.divergence_retries = 0
        out = self.check([trace_branch(7, False)])
        self.assertIn('SWAT Assertion failed', out)
        self.assertEqual(self.dropped, [])
        self.assertEqual(self.retired, {70})

    def test_divergence_of_a_guess_is_expected(self):
        self.approximate = True
        self.driver.divergence_retries = 0
        self.aim(self.target, {'I_0': 1})
        out = self.check([trace_branch(7, False)])
        self.assertNotIn('SWAT Assertion failed', out)
        self.assertNotIn('ERROR', out)
        self.assertEqual(self.tree.expected_divergences, 1)
        self.assertEqual(self.tree.execution_errors, [])
        # Given up on, but no SAFE downgrade of its own: the approximate model already rules SAFE out.
        self.assertEqual(self.retired, {70})
        self.assertEqual(self.driver.given_up, [])

    def test_guess_is_retried_too(self):
        self.approximate = True
        self.aim(self.target, {'I_0': 1})
        out = self.check([trace_branch(8, True)])
        self.assertNotIn('SWAT Assertion failed', out)
        self.assertEqual(self.tree.expected_divergences, 1)
        self.assertEqual(self.dropped, [70])

    def test_side_reached_later_lifts_the_downgrade(self):
        self.driver.divergence_retries = 0
        self.check([trace_branch(7, False)])
        self.assertEqual(self.driver.unreached_given_up_branches(), [self.target])
        # Another run reaches the branched side after all: what lies behind it is in the tree now.
        self.target.branched = object()
        self.assertEqual(self.driver.unreached_given_up_branches(), [])

    def test_skipped_side(self):
        target = SimpleNamespace(id=9, gid=90, trace_id='t', branched=object(), skipped=None, parent=None)
        self.driver.divergence_retries = 0
        self.aim(target, {'I_0': 1})
        self.check([trace_branch(9, True)])
        self.assertEqual(self.driver.unreached_given_up_branches(), [target])
        target.skipped = object()
        self.assertEqual(self.driver.unreached_given_up_branches(), [])


class SolveRetryTest(DivergenceTestBase):
    """solve_retry tries the bounds in RETRY_BOUNDS, then no bound; only UNSAT without a bound
    gives the branch up."""

    def setUp(self):
        super().setUp()
        self.driver.failed_solutions[70] = [int_solution(self.ctx, I_0=2**31 - 1)]
        self.calls = []

    def solver(self, results):
        def solve(b, solver_timeout_ms=None, solver_rlimit=None, extra_exprs=None):
            self.calls.append((b.gid, solver_rlimit, len(extra_exprs or [])))
            result = results[len(self.calls) - 1]
            return result, ({'I_0': 3} if result == SATResult.SAT else {})
        return mock.patch.object(svcomp.StrategyService, 'solve_branch', side_effect=solve)

    def test_smallest_bound_first(self):
        with self.solver([SATResult.SAT]):
            sat, sol = self.driver.solve_retry(self.target, None)
        self.assertEqual(sat, SATResult.SAT)
        # The exclusion plus the bound on I_0
        self.assertEqual(self.calls, [(70, None, 2)])
        self.assertEqual(self.retired, set())

    def test_bounded_failures_move_on(self):
        results = [SATResult.UNSAT, SATResult.LIMIT, SATResult.UNKNOWN, SATResult.SAT]
        with self.solver(results):
            sat, _ = self.driver.solve_retry(self.target, 1000)
        self.assertEqual(sat, SATResult.SAT)
        self.assertEqual(self.calls, [(70, 1000, 2)] * 3 + [(70, 1000, 1)])
        self.assertEqual(self.driver.nr_solver_calls, 4)
        self.assertEqual(self.retired, set())

    def test_no_other_solution_gives_up(self):
        with self.solver([SATResult.UNSAT] * 4):
            with self.assertLogs(svcomp.logger, level='INFO') as logs:
                sat, _ = self.driver.solve_retry(self.target, None)
        self.assertEqual(sat, SATResult.UNSAT)
        self.assertIn('SWAT Assertion failed', '\n'.join(logs.output))
        self.assertEqual(self.retired, {70})
        self.assertEqual([e['kind'] for e in self.tree.execution_errors], ['unexpected_divergence'])

    def test_unbounded_limit_is_passed_on(self):
        # The caller escalates the resource limit; the branch is not given up on.
        with self.solver([SATResult.UNSAT] * 3 + [SATResult.LIMIT]):
            sat, _ = self.driver.solve_retry(self.target, 1000)
        self.assertEqual(sat, SATResult.LIMIT)
        self.assertEqual(self.retired, set())

    def test_solve_candidates_routes_retries(self):
        other = SimpleNamespace(id=8, gid=80, trace_id='t')
        with self.solver([SATResult.UNSAT] * 4 + [SATResult.SAT]):
            found, _, gave_up = self.driver.solve_candidates([self.target, other])
        self.assertIs(found, other)
        self.assertFalse(gave_up)
        self.assertEqual(self.retired, {70})


class RetryConstraintsTest(unittest.TestCase):
    """Z3Handler.retry_constraints on a real solver."""

    def setUp(self):
        self.ctx = z3.Context()
        self.x = z3.BitVec('I_0', 32, ctx=self.ctx)
        self.y = z3.BitVec('I_1', 32, ctx=self.ctx)

    def model(self, *exprs):
        s = z3.Solver(ctx=self.ctx)
        s.add(*exprs)
        if s.check() != z3.sat:
            return None
        m = s.model()
        return {str(d.name()): m[d].as_signed_long() for d in m.decls()}

    def test_failed_solutions_are_excluded(self):
        failed = [int_solution(self.ctx, I_0=5), int_solution(self.ctx, I_0=6)]
        extra = Z3Handler.retry_constraints(failed, ctx=self.ctx)
        self.assertEqual(self.model(self.x >= 5, self.x <= 7, *extra), {'I_0': 7})
        self.assertIsNone(self.model(self.x >= 5, self.x <= 6, *extra))

    def test_exclusion_is_by_the_whole_solution(self):
        # Excluding (I_0=1, I_1=2) still allows I_0=1 with another I_1.
        extra = Z3Handler.retry_constraints([int_solution(self.ctx, I_0=1, I_1=2)], ctx=self.ctx)
        m = self.model(self.x == 1, self.y >= 2, self.y <= 3, *extra)
        self.assertEqual(m, {'I_0': 1, 'I_1': 3})

    def test_bound_gives_small_values(self):
        failed = [int_solution(self.ctx, I_0=2**31 - 1)]
        extra = Z3Handler.retry_constraints(failed, 16, ctx=self.ctx)
        m = self.model(self.x > 1, *extra)
        self.assertLessEqual(m['I_0'], 16)
        self.assertGreater(m['I_0'], 1)
        # Bounds are signed: negative values are bounded below as well.
        m = self.model(self.x < 0, *extra)
        self.assertGreaterEqual(m['I_0'], -16)
        self.assertIsNone(self.model(self.x > 16, *extra))

    def test_bound_beyond_the_type_is_left_out(self):
        b = z3.BitVec('B_0', 8, ctx=self.ctx)
        failed = [{'B_0': {'encoded_value': '5', 'plain_value': z3.BitVecVal(5, 8, ctx=self.ctx), 'index': '0'}}]
        self.assertEqual(len(Z3Handler.retry_constraints(failed, 256, ctx=self.ctx)), 1)
        self.assertEqual(len(Z3Handler.retry_constraints(failed, 16, ctx=self.ctx)), 2)

    def test_arrays_and_empty_solutions(self):
        # Array entries carry no Z3 value; with nothing to tell models apart, all are excluded.
        failed = [{'[I_0': {'encoded_value': '[1]', 'plain_value': 'K(Int, 1)', 'index': '0'}}]
        extra = Z3Handler.retry_constraints(failed, ctx=self.ctx)
        self.assertIsNone(self.model(*extra))

    def test_strings_and_doubles_are_excluded_exactly(self):
        s = z3.String('java/lang/String_0', ctx=self.ctx)
        d = z3.FP('D_1', z3.Float64(self.ctx))
        failed = [{'java/lang/String_0': {'encoded_value': 'a', 'plain_value': z3.StringVal('a', ctx=self.ctx), 'index': '0'},
                   'D_1': {'encoded_value': '0', 'plain_value': z3.FPVal(-0.0, z3.Float64(self.ctx)), 'index': '1'}}]
        extra = Z3Handler.retry_constraints(failed, 16, ctx=self.ctx)
        sol = z3.Solver(ctx=self.ctx)
        sol.add(s == z3.StringVal('a', ctx=self.ctx), z3.Or(d == z3.FPVal(0.0, z3.Float64(self.ctx)), d == z3.FPVal(-0.0, z3.Float64(self.ctx))), *extra)
        self.assertEqual(sol.check(), z3.sat)
        # Only the other zero is left: the exclusion is by bit pattern, not by FP equality.
        self.assertEqual(sol.model().eval(d).as_string(), z3.FPVal(0.0, z3.Float64(self.ctx)).as_string())


class SolveBranchTest(unittest.TestCase):
    """UNSAT under retry constraints does not make the branch infeasible."""

    def test_unsat_with_extra_constraints_is_not_recorded(self):
        db = mock.Mock()
        branch = SimpleNamespace(gid=1, id=1, trace_id='t', parent=None, inputs=[], ufs=[])
        with mock.patch('strategy.StrategyService.Database.instance', return_value=db), \
             mock.patch('strategy.StrategyService.Z3Handler.solve', return_value=(SATResult.UNSAT, {})):
            StrategyService.solve_branch(branch, extra_exprs=['x'])
            db.add_unsat_branch.assert_not_called()
            StrategyService.solve_branch(branch)
            db.add_unsat_branch.assert_called_once_with(1)


if __name__ == '__main__':
    unittest.main()
