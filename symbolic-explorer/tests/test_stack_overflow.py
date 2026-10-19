"""
Unit tests for SVCompDriver.determine_next_step on executions whose trace reports a stack overflow
in the target: their outcome depends on the JVM's stack size, so a violation they show is not a
verdict, and an execution without symbolic inputs is not SAFE.

It imports the driver, which needs the explorer's dependencies (z3, fastapi, ...) that the system
Python lacks, so run it with the harness venv, from symbolic-explorer/:
    ../targets/sv-comp/scripts/.venv/bin/python3 -m unittest tests.test_stack_overflow -v
"""

import os
import sys
import unittest
from types import SimpleNamespace
from unittest import mock

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from driver import SVCompDriver as svcomp

ASSERTION = 'Exception in thread "main" java.lang.AssertionError'
RUNTIME_EXCEPTION = '[SWAT] Uncaught top-level RuntimeException in symbolic execution'


class StackOverflowRoundTest(unittest.TestCase):

    def setUp(self):
        self.tree = SimpleNamespace(execution_errors=[], symbolic_vars={'I_0'}, uncaught_exceptions=0)
        self.overflowed_traces = set()
        def record_execution_error(endpoint_id, kind, message):
            self.tree.execution_errors.append({'kind': kind, 'message': message})
        def record_uncaught_exception(endpoint_id):
            self.tree.uncaught_exceptions += 1
        db = SimpleNamespace(
            record_execution_error=record_execution_error,
            record_uncaught_exception=record_uncaught_exception,
            get_tree=lambda endpoint_id: self.tree,
            get_execution_errors=lambda endpoint_id, kind: [e for e in self.tree.execution_errors if e.get('kind') == kind],
            count_symbolic_vars=lambda endpoint_id: len(self.tree.symbolic_vars),
            get_witness_data=lambda endpoint_id: {'trace_id': 't1'},
            is_stack_overflow_trace=lambda endpoint_id, trace_id: trace_id in self.overflowed_traces)
        patcher = mock.patch.object(svcomp.Database, 'instance', return_value=db)
        patcher.start()
        self.addCleanup(patcher.stop)
        self.driver = svcomp.SVCompDriver.__new__(svcomp.SVCompDriver)
        self.driver.state = svcomp.State()

    def step(self, category, output):
        self.driver.verification_category = category
        return self.driver.determine_next_step(svcomp.ExecutionStatus.SUCCESS, output)

    def test_assertion_without_stack_overflow_is_a_violation(self):
        action = self.step(svcomp.VerificationCategory.VALID_ASSERT_PRP, [ASSERTION])
        self.assertEqual(action, svcomp.Action.REPORTVERDICT)
        self.assertEqual(self.driver.state.verdict, svcomp.Verdict.VIOLATION)

    def test_assertion_after_stack_overflow_is_not_reported(self):
        self.overflowed_traces.add('t1')
        action = self.step(svcomp.VerificationCategory.VALID_ASSERT_PRP, [ASSERTION])
        self.assertEqual(action, svcomp.Action.SYMBOLICNEXT)
        self.assertNotEqual(self.driver.state.verdict, svcomp.Verdict.VIOLATION)
        self.assertEqual([e['kind'] for e in self.tree.execution_errors], ['stack_overflow_violation'])

    def test_runtime_exception_after_stack_overflow_is_not_reported(self):
        self.overflowed_traces.add('t1')
        action = self.step(svcomp.VerificationCategory.NO_RUNTIME_EXCEPTION_PRP, [RUNTIME_EXCEPTION])
        self.assertEqual(action, svcomp.Action.SYMBOLICNEXT)
        self.assertNotEqual(self.driver.state.verdict, svcomp.Verdict.VIOLATION)

    def test_swat_exception_still_ends_the_run(self):
        self.overflowed_traces.add('t1')
        action = self.step(svcomp.VerificationCategory.VALID_ASSERT_PRP, [ASSERTION, '[SWAT Exception]: boom'])
        self.assertEqual(action, svcomp.Action.REPORTVERDICT)
        self.assertEqual(self.driver.state.verdict, svcomp.Verdict.UNKNOWN)

    def test_overflow_of_another_round_does_not_count(self):
        self.overflowed_traces.add('t0')
        self.step(svcomp.VerificationCategory.VALID_ASSERT_PRP, [ASSERTION])
        self.assertEqual(self.driver.state.verdict, svcomp.Verdict.VIOLATION)

    def test_no_symbolic_vars_after_stack_overflow_is_unknown(self):
        self.overflowed_traces.add('t1')
        self.tree.symbolic_vars = set()
        action = self.step(svcomp.VerificationCategory.NO_RUNTIME_EXCEPTION_PRP, [])
        self.assertEqual(action, svcomp.Action.REPORTVERDICT)
        self.assertEqual(self.driver.state.verdict, svcomp.Verdict.UNKNOWN)

    def test_no_symbolic_vars_without_stack_overflow_is_safe(self):
        self.tree.symbolic_vars = set()
        self.step(svcomp.VerificationCategory.NO_RUNTIME_EXCEPTION_PRP, [])
        self.assertEqual(self.driver.state.verdict, svcomp.Verdict.NO_SYMBOLIC_VARS)


if __name__ == '__main__':
    unittest.main()
