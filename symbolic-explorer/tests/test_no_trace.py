"""
Unit tests for SVCompDriver.determine_next_step on runs that sent no trace: the target never reached
its entry method (e.g. a VerifyError or a missing main class), so the run must not count as a
non-symbolic program that ran to completion.

It imports the driver, which needs the explorer's dependencies (z3, fastapi, ...) that the system
Python lacks, so run it with the harness venv, from symbolic-explorer/:
    ../targets/sv-comp/scripts/.venv/bin/python3 -m unittest tests.test_no_trace -v
"""

import os
import sys
import unittest
from types import SimpleNamespace
from unittest import mock

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from driver import SVCompDriver as svcomp
from svcomp.ResourceBudget import ResourceBudget

VERIFY_ERROR_OUTPUT = [
    'Error: Unable to initialize main class Main',
    'Caused by: java.lang.VerifyError: Bad type on operand stack',
]


class NoTraceTest(unittest.TestCase):

    def setUp(self):
        self.tree = SimpleNamespace(symbolic_vars=set(), execution_errors=[], uncaught_exceptions=0)
        def record_execution_error(endpoint_id, kind, message):
            self.tree.execution_errors.append({'kind': kind, 'message': message})
        def record_uncaught_exception(endpoint_id):
            self.tree.uncaught_exceptions += 1
        db = SimpleNamespace(record_execution_error=record_execution_error,
                             record_uncaught_exception=record_uncaught_exception,
                             get_tree=lambda endpoint_id: self.tree,
                             get_witness_data=lambda endpoint_id: None,
                             is_stack_overflow_trace=lambda endpoint_id, trace_id: False,
                             get_execution_errors=lambda endpoint_id, kind: [e for e in self.tree.execution_errors if e.get('kind') == kind],
                             count_symbolic_vars=lambda endpoint_id: len(self.tree.symbolic_vars))
        patcher = mock.patch.object(svcomp.Database, 'instance', return_value=db)
        patcher.start()
        self.addCleanup(patcher.stop)
        self.driver = svcomp.SVCompDriver.__new__(svcomp.SVCompDriver)
        self.driver.state = svcomp.State()
        self.driver.verification_category = svcomp.VerificationCategory.VALID_ASSERT_PRP

    def step(self, output, received_trace):
        return self.driver.determine_next_step(svcomp.ExecutionStatus.SUCCESS, output, received_trace)

    def test_failed_start_is_unknown(self):
        self.assertEqual(self.step(VERIFY_ERROR_OUTPUT, received_trace=False), svcomp.Action.REPORTVERDICT)
        self.assertEqual(self.driver.state.verdict, svcomp.Verdict.UNKNOWN)
        self.assertEqual([e['kind'] for e in self.tree.execution_errors], ['no_trace'])

    def test_missing_trace_with_symbolic_vars_from_earlier_rounds_is_unknown(self):
        self.tree.symbolic_vars = {'int_0'}
        self.assertEqual(self.step([], received_trace=False), svcomp.Action.REPORTVERDICT)
        self.assertEqual(self.driver.state.verdict, svcomp.Verdict.UNKNOWN)

    def test_non_symbolic_program_that_ran_is_non_symbolic(self):
        self.assertEqual(self.step([], received_trace=True), svcomp.Action.REPORTVERDICT)
        self.assertEqual(self.driver.state.verdict, svcomp.Verdict.NO_SYMBOLIC_VARS)
        self.assertEqual(self.tree.execution_errors, [])

    def test_symbolic_program_that_ran_continues(self):
        self.tree.symbolic_vars = {'int_0'}
        self.assertEqual(self.step([], received_trace=True), svcomp.Action.SYMBOLICNEXT)

    def test_assertion_error_in_output_is_still_a_violation(self):
        # E.g. an assert in the static initializer of the main class: program code that failed
        # before main, a genuine violation.
        output = ['Exception in thread "main" java.lang.ExceptionInInitializerError',
                  'Caused by: java.lang.AssertionError']
        self.assertEqual(self.step(output, received_trace=False), svcomp.Action.REPORTVERDICT)
        self.assertEqual(self.driver.state.verdict, svcomp.Verdict.VIOLATION)


class ReceivedTraceTest(unittest.TestCase):
    """run_testcase decides received_trace from the trace count around the target run."""

    def run_round(self, traces_sent, branch_to_explore=None, proven=False):
        traces = {'n': 0}
        tree = SimpleNamespace(symbolic_vars=set(), execution_errors=[])
        def record_execution_error(endpoint_id, kind, message):
            tree.execution_errors.append({'kind': kind, 'message': message})
        db = SimpleNamespace(trace_count=lambda: traces['n'], clear_witness_data=lambda endpoint_id: None,
                             record_execution_error=record_execution_error, get_tree=lambda endpoint_id: tree,
                             get_trace=lambda idx: [], get_witness_data=lambda endpoint_id: None,
                             is_stack_overflow_trace=lambda endpoint_id, trace_id: False,
                             get_execution_errors=lambda endpoint_id, kind: [e for e in tree.execution_errors if e.get('kind') == kind],
                             count_symbolic_vars=lambda endpoint_id: len(tree.symbolic_vars))
        driver = svcomp.SVCompDriver.__new__(svcomp.SVCompDriver)
        driver.state = svcomp.State()
        driver.state.branch_to_explore = branch_to_explore
        driver.verification_category = svcomp.VerificationCategory.VALID_ASSERT_PRP
        driver.args = SimpleNamespace(parallel_sa=True)
        driver.sa = SimpleNamespace(start=lambda: None)
        driver.precheck = SimpleNamespace(proven=proven, reason=None, run=lambda: None, stats=lambda: {})
        driver.budget = ResourceBudget(cgroup_limit=0)
        driver.build_command = lambda **kwargs: []
        def run_command(cmd):
            traces['n'] += traces_sent
            return svcomp.ExecutionStatus.SUCCESS, []
        driver.run_command_with_timeout = run_command
        driver.check_divergence = mock.Mock()
        with mock.patch.object(svcomp.Database, 'instance', return_value=db), \
                mock.patch.object(svcomp.TimingManager, 'instance'):
            verdict = driver.run_testcase(None, None, None, None, None, None)
        return verdict, tree, driver

    def test_round_with_trace(self):
        verdict, tree, _ = self.run_round(traces_sent=1)
        self.assertEqual(verdict, svcomp.Verdict.NO_SYMBOLIC_VARS)

    def test_round_without_trace(self):
        verdict, tree, driver = self.run_round(traces_sent=0, branch_to_explore=SimpleNamespace(id=1))
        self.assertEqual(verdict, svcomp.Verdict.UNKNOWN)
        self.assertEqual([e['kind'] for e in tree.execution_errors], ['no_trace'])
        # The latest trace is one of an earlier round: no divergence check against it
        driver.check_divergence.assert_not_called()

    def test_failed_start_is_unknown_despite_a_proof(self):
        # The property pre-check (#73) proves nothing about a run that never reached main
        verdict, tree, _ = self.run_round(traces_sent=0, proven=True)
        self.assertEqual(verdict, svcomp.Verdict.UNKNOWN)
        self.assertEqual([e['kind'] for e in tree.execution_errors], ['no_trace'])


if __name__ == '__main__':
    unittest.main()
