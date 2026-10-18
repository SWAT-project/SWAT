"""
Unit tests for how SVCompDriver ends a run whose search is incomplete or that failed: a crash ends
the run at once with UNKNOWN, never SAFE; a tree that does not cover every run, a trace that could
not be added and a run without a trace rule out SAFE.

Needs the explorer's dependencies, so run it with the harness venv, from symbolic-explorer/:
    ../targets/sv-comp/scripts/.venv/bin/python3 -m unittest tests.test_driver_robustness -v
"""

import os
import sys
import tempfile
import unittest
from types import SimpleNamespace
from unittest import mock

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from data.BinaryExecutionTree.Tree import Tree
from driver import SVCompDriver as svcomp


def not_proven():
    return SimpleNamespace(proven=False, reason=None, run=lambda: None, stats=lambda: {})


class FakeDatabase:
    def __init__(self):
        self.tree = Tree(0)
        self.traces = []

    def get_tree(self, endpoint_id):
        return self.tree

    def add_endpoint(self, endpoint_id):
        pass

    def record_execution_error(self, endpoint_id, kind, message):
        self.tree.record_execution_error(kind, message)

    def trace_count(self):
        return len(self.traces)

    def get_trace(self, idx):
        return self.traces[idx]

    def clear_witness_data(self, endpoint_id):
        pass


class DriverTest(unittest.TestCase):

    def setUp(self):
        self.db = FakeDatabase()
        patcher = mock.patch.object(svcomp.Database, 'instance', return_value=self.db)
        patcher.start()
        self.addCleanup(patcher.stop)
        self.logdir = tempfile.mkdtemp()
        self.driver = svcomp.SVCompDriver.__new__(svcomp.SVCompDriver)
        self.driver.args = SimpleNamespace(java_path='java', agent='a', config='c', z3dir='z', port=0,
                                           classpath=[], parallel_sa=False, logdir=self.logdir)
        self.driver.state = svcomp.State()
        self.driver.verification_category = svcomp.VerificationCategory.VALID_ASSERT_PRP
        self.driver.sa = SimpleNamespace(cancel=lambda: None, duration=None, stats=lambda r: {})
        self.driver.sa_adopted_round = None
        self.driver.round_idx = 0
        self.driver.nr_solver_calls = 0
        self.driver.solver_limit_hits = 0
        self.driver.verdict_reported = False
        self.driver.kill_current_process = lambda: None
        self.driver.precheck = not_proven()
        self.driver.given_up = []
        self.driver.divergence_retry_count = 0

    def run_with(self, run_testcase):
        self.driver.run_testcase = run_testcase
        with self.assertLogs(svcomp.verdict_logger, level='INFO') as logs:
            self.driver.run()
        verdicts = [l for l in logs.output if '[VERDICT' in l]
        self.assertEqual(len(verdicts), 1)
        self.assertTrue(self.driver.verdict_reported)
        return verdicts[0]

    def test_crash_reports_unknown(self):
        def crash(**kwargs):
            raise RecursionError('maximum recursion depth exceeded')
        verdict = self.run_with(crash)
        self.assertIn('== DONT-KNOW', verdict)
        self.assertEqual([e['kind'] for e in self.db.tree.execution_errors], ['explorer_crash'])

    def test_safe_stays_safe_on_a_complete_search(self):
        self.assertIn('== OK', self.run_with(lambda **kwargs: svcomp.Verdict.SAFE))

    def test_incomplete_search_rules_out_safe(self):
        for kind in svcomp.INCOMPLETE_SEARCH_ERRORS:
            with self.subTest(kind=kind):
                self.db.tree = Tree(0)
                self.db.tree.record_execution_error(kind, 'x')
                self.assertIn('== DONT-KNOW', self.run_with(lambda **kwargs: svcomp.Verdict.SAFE))

    def test_incomplete_search_rules_out_a_proven_safe(self):
        self.driver.precheck = SimpleNamespace(proven=True, reason='no throw', run=lambda: None, stats=lambda: {})
        self.db.tree.record_execution_error('trace_ingest_failed', 'x')
        self.assertIn('== DONT-KNOW', self.run_with(lambda **kwargs: svcomp.Verdict.SAFE))

    def test_incomplete_search_keeps_a_violation(self):
        self.db.tree.record_execution_error('tree_mismatch', 'x')
        self.driver.write_witness = lambda: None
        self.assertIn('== ERROR', self.run_with(lambda **kwargs: svcomp.Verdict.VIOLATION))

    def test_ingest_failure_ends_the_search(self):
        self.db.tree.symbolic_vars.add('x')
        self.db.tree.record_execution_error('trace_ingest_failed', 'trace t: RecursionError')
        with self.assertLogs(svcomp.logger, level='INFO'):
            action = self.driver.determine_next_step(svcomp.ExecutionStatus.SUCCESS, [])
        self.assertEqual(action, svcomp.Action.REPORTVERDICT)
        self.assertEqual(self.driver.state.verdict, svcomp.Verdict.UNKNOWN)

    def test_violation_in_the_output_still_counts_after_an_ingest_failure(self):
        self.db.tree.record_execution_error('trace_ingest_failed', 'trace t: RecursionError')
        with self.assertLogs(svcomp.logger, level='INFO'):
            action = self.driver.determine_next_step(svcomp.ExecutionStatus.SUCCESS,
                                                     ['Exception in thread "main" java.lang.AssertionError'])
        self.assertEqual(action, svcomp.Action.REPORTVERDICT)
        self.assertEqual(self.driver.state.verdict, svcomp.Verdict.VIOLATION)


class NoTraceTest(unittest.TestCase):
    """A run that sends no trace is not checked against the previous run's trace."""

    def test_run_without_trace_is_recorded_not_checked(self):
        db = FakeDatabase()
        db.traces.append([SimpleNamespace(id=7, has_branched=False)])
        driver = svcomp.SVCompDriver.__new__(svcomp.SVCompDriver)
        driver.args = SimpleNamespace(parallel_sa=True)
        driver.state = svcomp.State()
        driver.state.branch_to_explore = SimpleNamespace(id=7, branched=None, parent=None)
        driver.sa = SimpleNamespace(start=lambda: None)
        driver.precheck = not_proven()
        driver.symbolicStorage = SimpleNamespace(input_type=None)
        driver.build_command = lambda **kwargs: []
        driver.run_command_with_timeout = lambda cmd: (svcomp.ExecutionStatus.ERROR, [])
        driver.check_divergence = mock.Mock()
        with mock.patch.object(svcomp.Database, 'instance', return_value=db), \
                mock.patch.object(svcomp.TimingManager, 'instance'), \
                self.assertLogs(svcomp.logger, level='INFO'):
            verdict = driver.run_testcase(java_path='java', agentpath='a', configpath='c', z3path='z', port=0, cp=[])
        self.assertEqual(verdict, svcomp.Verdict.UNKNOWN)
        driver.check_divergence.assert_not_called()
        self.assertEqual([e['kind'] for e in db.tree.execution_errors], ['no_trace'])


if __name__ == '__main__':
    unittest.main()
