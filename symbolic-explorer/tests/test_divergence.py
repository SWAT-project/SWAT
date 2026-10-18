"""
Unit tests for SVCompDriver.check_divergence: a run that does not take the side a solution was
solved for is an internal error, unless the solution relied on an approximate model.

It imports the driver, which needs the explorer's dependencies (z3, fastapi, ...) that the system
Python lacks, so run it with the harness venv, from symbolic-explorer/:
    ../targets/sv-comp/scripts/.venv/bin/python3 -m unittest tests.test_divergence -v
"""

import os
import sys
import unittest
from types import SimpleNamespace
from unittest import mock

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from driver import SVCompDriver as svcomp


def trace_branch(id, has_branched):
    return SimpleNamespace(id=id, has_branched=has_branched)


class CheckDivergenceTest(unittest.TestCase):

    def setUp(self):
        self.tree = SimpleNamespace(expected_divergences=0, execution_errors=[])
        def record_expected_divergence(endpoint_id):
            self.tree.expected_divergences += 1
        def record_execution_error(endpoint_id, kind, message):
            self.tree.execution_errors.append({'kind': kind, 'message': message})
        db = SimpleNamespace(record_expected_divergence=record_expected_divergence,
                             record_execution_error=record_execution_error)
        patcher = mock.patch.object(svcomp.Database, 'instance', return_value=db)
        patcher.start()
        self.addCleanup(patcher.stop)
        self.driver = svcomp.SVCompDriver.__new__(svcomp.SVCompDriver)
        self.driver.state = svcomp.State()
        # Solved to take the branched side, which the tree has not explored yet.
        self.driver.state.branch_to_explore = SimpleNamespace(id=7, branched=None)

    def check(self, trace):
        with self.assertLogs(svcomp.logger, level='INFO') as logs:
            self.driver.check_divergence(trace)
        return '\n'.join(logs.output)

    def test_taking_the_predicted_side_is_silent(self):
        with self.assertNoLogs(svcomp.logger, level='INFO'):
            self.driver.check_divergence([trace_branch(7, True)])
        self.assertEqual(self.tree.expected_divergences, 0)

    def test_repeated_id_is_matched_by_position(self):
        # The target is at depth 2; the same id also occurs at depth 0 (e.g. a switch whose case
        # numbers wrap), taken the other way. Only the element at the target's depth counts.
        root = SimpleNamespace(parent=None)
        middle = SimpleNamespace(parent=root)
        self.driver.state.branch_to_explore = SimpleNamespace(id=7, branched=None, parent=middle)
        with self.assertNoLogs(svcomp.logger, level='INFO'):
            self.driver.check_divergence([trace_branch(7, False), trace_branch(3, True), trace_branch(7, True)])
        self.assertEqual(self.tree.execution_errors, [])

    def test_divergence_of_an_exact_solution_is_an_error(self):
        out = self.check([trace_branch(7, False)])
        self.assertIn('ERROR', out)
        self.assertIn('SWAT Assertion failed', out)
        self.assertEqual(self.tree.expected_divergences, 0)
        # Recorded, so that the run cannot end SAFE
        self.assertEqual([e['kind'] for e in self.tree.execution_errors], ['unexpected_divergence'])

    def test_divergence_of_a_guess_is_expected(self):
        self.driver.state.solution_is_guess = True
        out = self.check([trace_branch(7, False)])
        self.assertNotIn('SWAT Assertion failed', out)
        self.assertNotIn('ERROR', out)
        self.assertEqual(self.tree.expected_divergences, 1)
        self.assertEqual(self.tree.execution_errors, [])

    def test_missing_branch_of_a_guess_is_expected(self):
        self.driver.state.solution_is_guess = True
        out = self.check([trace_branch(8, True)])
        self.assertNotIn('SWAT Assertion failed', out)
        self.assertEqual(self.tree.expected_divergences, 1)

    def test_unmodelled_exception_marker_does_not_count_as_a_level(self):
        # The tree has no level for the marker (Tree.structural), so neither does the trace.
        root = SimpleNamespace(parent=None)
        self.driver.state.branch_to_explore = SimpleNamespace(id=7, branched=None, parent=root)
        marker = SimpleNamespace(id=5, has_branched=True, inst='UNMODELLED_EXCEPTION')
        with self.assertNoLogs(svcomp.logger, level='INFO'):
            self.driver.check_divergence([trace_branch(3, True), marker, trace_branch(7, True)])
        self.assertEqual(self.tree.execution_errors, [])


if __name__ == '__main__':
    unittest.main()
