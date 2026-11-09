"""
Unit tests for svcomp/StaticPreAnalysis.py budget handling: the extractor is launched with an
explicit -Xmx sized to the memory budget, and the pre-analysis is skipped (not run into an
out-of-memory error) when the budget is too small for it. Runs on the system Python (no z3).

    python3 -m unittest tests.test_static_pre_analysis -v
"""

import argparse
import os
import sys
import unittest
from unittest import mock

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from svcomp.StaticPreAnalysis import StaticPreAnalysis, SAStatus


def _args(**kw):
    base = dict(property="valid-assert.prp", sa_jar="/sa/cfg-extractor.jar", sa_path=None,
                sa_file=None, classpath=["/cp/common", "/cp/build"], logdir="/logs", target="Main",
                sa_entry=None, no_sa_slicing=False, sa_no_callback_targets=False)
    base.update(kw)
    return argparse.Namespace(**base)


class _FakeProc:
    def __init__(self, cmd):
        self.cmd = cmd

    def wait(self, timeout=None):
        return 0

    def poll(self):
        return 0


def _capture_extractor_cmd(sa):
    """Run _run_extractor with subprocess.Popen mocked; return (result, cmd)."""
    captured = {}

    def fake_popen(cmd, *a, **k):
        captured['cmd'] = cmd
        return _FakeProc(cmd)

    with mock.patch('subprocess.Popen', side_effect=fake_popen):
        result = sa._run_extractor()
    return result, captured.get('cmd')


class StaticPreAnalysisBudgetTest(unittest.TestCase):

    def test_skips_when_budget_too_small(self):
        sa = StaticPreAnalysis(_args())
        sa.set_budget(None)  # the reduced pre-run limit cannot fit the extractor
        self.assertFalse(sa.enabled)
        self.assertEqual(sa.status, SAStatus.DISABLED)
        sa.start()
        sa.wait()
        self.assertTrue(sa.is_done())
        self.assertEqual(sa.status, SAStatus.DISABLED)
        self.assertIsNone(sa.take_graph())
        stats = sa.stats(None)
        self.assertFalse(stats['enabled'])
        self.assertFalse(stats['failed'])  # skipped, not failed

    def test_extractor_gets_xmx_from_budget(self):
        sa = StaticPreAnalysis(_args())
        sa.set_budget(2048)
        self.assertTrue(sa.enabled)
        result, cmd = _capture_extractor_cmd(sa)
        self.assertTrue(result)
        self.assertEqual(cmd[0], 'java')
        self.assertIn('-Xmx2048m', cmd)
        # the heap flag must come before -jar so the JVM honours it
        self.assertLess(cmd.index('-Xmx2048m'), cmd.index('-jar'))

    def test_no_xmx_without_a_budget(self):
        # Backward compatible: with no budget set (e.g. a direct caller or a test) no -Xmx is
        # passed and the extractor runs with the JVM default heap, as before.
        sa = StaticPreAnalysis(_args())
        self.assertTrue(sa.enabled)
        _, cmd = _capture_extractor_cmd(sa)
        self.assertFalse(any(str(a).startswith('-Xmx') for a in cmd))


if __name__ == '__main__':
    unittest.main()
