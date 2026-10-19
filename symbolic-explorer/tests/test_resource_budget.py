"""
Unit tests for the SV-COMP resource budget (svcomp/ResourceBudget.py) and how SVCompDriver acts on
it: the derived limits, the monitor stopping the executor or a Z3 query, and the verdicts after a run
the budget stopped.

It imports the driver, which needs the explorer's dependencies (z3, fastapi, ...) that the system
Python lacks, so run it with the harness venv, from symbolic-explorer/:
    ../targets/sv-comp/scripts/.venv/bin/python3 -m unittest tests.test_resource_budget -v
"""

import os
import subprocess
import sys
import tempfile
import time
import unittest
from types import SimpleNamespace
from unittest import mock

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from driver import SVCompDriver as svcomp
from svcomp import ResourceBudget as rb
from svcomp.ResourceBudget import ResourceBudget, MB, MIB


class CgroupLimitTest(unittest.TestCase):

    def fs(self, cgroup_line, limits):
        root = tempfile.mkdtemp()
        os.makedirs(f'{root}/proc/self')
        with open(f'{root}/proc/self/cgroup', 'w') as f:
            f.write(cgroup_line + '\n')
        for path, value in limits.items():
            os.makedirs(f'{root}/cg{path}', exist_ok=True)
            with open(f'{root}/cg{path}/{"memory.limit_in_bytes" if path.startswith("/memory") else "memory.max"}', 'w') as f:
                f.write(value + '\n')
        return dict(proc_root=f'{root}/proc', cgroup_root=f'{root}/cg')

    def test_v2_takes_the_tightest_ancestor(self):
        roots = self.fs('0::/bench/run/tool', {'/bench': '20000000000', '/bench/run': '15000000000', '/bench/run/tool': 'max'})
        self.assertEqual(rb.cgroup_memory_limit(**roots), 15_000_000_000)

    def test_v2_without_limit(self):
        roots = self.fs('0::/user.slice/session.scope', {'/user.slice': 'max', '/user.slice/session.scope': 'max'})
        self.assertIsNone(rb.cgroup_memory_limit(**roots))

    def test_v1_ignores_the_no_limit_value(self):
        roots = self.fs('5:memory:/run', {'/memory/run': str(2**63 - 4096), '/memory': '8000000000'})
        self.assertEqual(rb.cgroup_memory_limit(**roots), 8_000_000_000)


    def test_cpu_time_of_the_limiting_cgroup(self):
        roots = self.fs('0::/bench/run', {'/bench': 'max', '/bench/run': '15000000000'})
        limit, directory = rb.limiting_cgroup(**roots)
        self.assertEqual(limit, 15_000_000_000)
        with open(f'{directory}/cpu.stat', 'w') as f:
            f.write('usage_usec 12500000\nuser_usec 10000000\n')
        self.assertEqual(rb.cgroup_cpu_time(directory), 12.5)
        self.assertIsNone(rb.cgroup_cpu_time(None))


class LimitsTest(unittest.TestCase):

    def test_competition_defaults(self):
        b = ResourceBudget(cgroup_limit=0)
        self.assertEqual(b.memory_limit, 15_000 * MB)
        self.assertEqual(b.memory_threshold, 13_000 * MB)
        # half of what is left after the explorer's share
        self.assertEqual(b.heap_mib, (13_000 - 1_500) * MB // 2 // MIB)
        self.assertEqual(b.cpu_deadline, 880)

    def test_lower_cgroup_limit_wins(self):
        b = ResourceBudget(cgroup_limit=8_000 * MB)
        self.assertEqual(b.memory_limit, 8_000 * MB)
        self.assertLess(b.heap_mib * MIB + 1_500 * MB, b.memory_threshold)

    def test_cpu_used_before_counts(self):
        b = ResourceBudget(cgroup_limit=0, cpu_used_s=12.5)
        self.assertGreaterEqual(b.usage()[1], 12.5)

    def test_usage_includes_children(self):
        b = ResourceBudget(cgroup_limit=0)
        own = b.usage()[0]
        with subprocess.Popen(['sleep', '5']) as p:
            time.sleep(0.2)  # until it is running
            b._last_scan = 0.0  # due for a full scan
            self.assertGreater(b.usage()[0], own)
            p.kill()

    def test_usage_includes_the_executor_between_scans(self):
        b = ResourceBudget(cgroup_limit=0)
        own = b.usage()[0]
        with subprocess.Popen(['sleep', '5']) as p, b.executor(p):
            time.sleep(0.2)
            self.assertGreater(b.usage()[0], own)
            p.kill()


class MonitorTest(unittest.TestCase):

    def budget(self, rss, cpu):
        b = ResourceBudget(cgroup_limit=0)
        b.usage = lambda: (rss, cpu)
        b.on_emergency = mock.Mock()
        return b

    def test_cpu_deadline_kills_the_executor(self):
        b = self.budget(100 * MB, 885)
        with subprocess.Popen(['sleep', '30']) as p, b.executor(p) as stopped_by:
            b.check()
            self.assertEqual(p.wait(timeout=5), -9)  # SIGKILL, so no shutdown hook sends a trace
        self.assertEqual(stopped_by(), 'cpu')
        self.assertTrue(b.cpu_exceeded.is_set())
        b.on_emergency.assert_not_called()

    def test_below_the_limits_nothing_happens(self):
        b = self.budget(12_000 * MB, 800)
        with subprocess.Popen(['sleep', '30']) as p, b.executor(p) as stopped_by:
            b.check()
            self.assertIsNone(p.poll())
            p.kill()
        self.assertIsNone(stopped_by())
        self.assertFalse(b.cpu_exceeded.is_set())

    def test_memory_interrupts_a_query(self):
        b = self.budget(13_100 * MB, 10)
        ctx = mock.Mock()
        with b.solver(ctx) as interrupted_by:
            b.check()
            b.check()  # still over: interrupted again, but counted once
        self.assertEqual(ctx.interrupt.call_count, 2)
        self.assertEqual(b.memory_hits, 1)
        self.assertEqual(interrupted_by(), 'memory')
        self.assertFalse(b.cpu_exceeded.is_set())

    def test_query_that_does_not_stop_ends_the_run(self):
        b = self.budget(13_100 * MB, 10)
        with b.solver(mock.Mock()):
            b.check()
            b.check()
            b.on_emergency.assert_not_called()
            b._solver_interrupted_at -= rb.INTERRUPT_GRACE_S + 0.1
            b.check()
        b.on_emergency.assert_called_once()

    def test_explorer_alone_over_the_limit_ends_the_run(self):
        b = self.budget(14_100 * MB, 10)
        b.check()
        b.on_emergency.assert_called_once()

    def test_explorer_slightly_over_the_threshold_is_tolerated(self):
        b = self.budget(13_100 * MB, 10)
        b.check()
        b.on_emergency.assert_not_called()

    def test_main_thread_stuck_after_the_deadline_ends_the_run(self):
        b = self.budget(100 * MB, 890)
        b.check()
        b.on_emergency.assert_not_called()
        b.cpu_exceeded_at -= rb.GRACE_S + 1
        b.check()
        b.on_emergency.assert_called_once()


class DriverTest(unittest.TestCase):

    def setUp(self):
        self.tree = SimpleNamespace(execution_errors=[], symbolic_vars={'I_0'})
        def record_execution_error(endpoint_id, kind, message):
            self.tree.execution_errors.append({'kind': kind, 'message': message})
        db = SimpleNamespace(record_execution_error=record_execution_error, get_tree=lambda e: self.tree,
                             record_uncaught_exception=lambda e: None)
        patcher = mock.patch.object(svcomp.Database, 'instance', return_value=db)
        patcher.start()
        self.addCleanup(patcher.stop)
        d = svcomp.SVCompDriver.__new__(svcomp.SVCompDriver)
        d.state = svcomp.State()
        d.verification_category = svcomp.VerificationCategory.VALID_ASSERT_PRP
        d.round_idx = 3
        d.stopped_by = None
        d.budget = ResourceBudget(cgroup_limit=0)
        self.driver = d

    def kinds(self):
        return [e['kind'] for e in self.tree.execution_errors]

    def test_memory_stop_goes_on_exploring(self):
        self.driver.stopped_by = 'memory'
        step = self.driver.determine_next_step(svcomp.ExecutionStatus.RESOURCE_LIMIT, ['some output'])
        self.assertEqual(step, svcomp.Action.SYMBOLICNEXT)
        self.assertEqual(self.kinds(), ['resource_limit'])  # rules out SAFE, see CUT_SHORT_ERRORS

    def test_memory_stop_of_the_first_run_is_unknown(self):
        self.tree.symbolic_vars = set()  # no trace yet: not "no symbolic inputs"
        self.driver.stopped_by = 'memory'
        step = self.driver.determine_next_step(svcomp.ExecutionStatus.RESOURCE_LIMIT, [])
        self.assertEqual(step, svcomp.Action.REPORTVERDICT)
        self.assertEqual(self.driver.state.verdict, svcomp.Verdict.UNKNOWN)

    def test_cpu_stop_is_unknown(self):
        self.driver.stopped_by = 'cpu'
        step = self.driver.determine_next_step(svcomp.ExecutionStatus.RESOURCE_LIMIT, [])
        self.assertEqual(step, svcomp.Action.REPORTVERDICT)
        self.assertEqual(self.driver.state.verdict, svcomp.Verdict.UNKNOWN)

    def test_violation_printed_before_the_stop_is_reported(self):
        self.driver.stopped_by = 'cpu'
        step = self.driver.determine_next_step(svcomp.ExecutionStatus.RESOURCE_LIMIT,
                                               ['Exception in thread "main" java.lang.AssertionError'])
        self.assertEqual(step, svcomp.Action.REPORTVERDICT)
        self.assertEqual(self.driver.state.verdict, svcomp.Verdict.VIOLATION)

    def test_out_of_memory_error_is_recorded(self):
        self.driver.verification_category = svcomp.VerificationCategory.NO_RUNTIME_EXCEPTION_PRP
        out = ['java.lang.OutOfMemoryError: Requested array size exceeds VM limit',
               'Exception in thread "main" java.lang.OutOfMemoryError: Requested array size exceeds VM limit']
        step = self.driver.determine_next_step(svcomp.ExecutionStatus.SUCCESS, out)
        self.assertEqual(step, svcomp.Action.SYMBOLICNEXT)
        self.assertEqual(self.kinds(), ['out_of_memory'])

    def test_heap_and_jvm_options_in_the_command(self):
        self.driver.symbolicStorage = SimpleNamespace(input_type=None)
        cmd = self.driver.build_command('agent.jar', 'sv-comp.cfg', 'z3', 8000, ['cp'], java_path='java')
        self.assertIn(f'-Xmx{self.driver.budget.heap_mib}m', cmd)
        self.assertIn('-XX:TieredStopAtLevel=1', cmd)

    def test_no_query_after_the_cpu_deadline(self):
        self.driver.args = SimpleNamespace(solver_rlimit=0)
        self.driver.nr_solver_calls = self.driver.solver_limit_hits = 0
        self.driver.rlimit_level = {}
        self.driver.budget.cpu_exceeded.set()
        with mock.patch.object(svcomp.StrategyService, 'solve_branch') as solve:
            found, _, gave_up = self.driver.solve_candidates([SimpleNamespace(gid=1, id=1)])
        solve.assert_not_called()
        self.assertIsNone(found)
        self.assertTrue(gave_up)  # so the verdict is not SAFE


if __name__ == '__main__':
    unittest.main()
