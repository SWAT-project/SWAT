"""
Unit tests for the static property pre-check: how its result is read per property, and the verdict
the driver gives after round 0 when the property is proven.

It imports the driver, which needs the explorer's dependencies (z3, fastapi, ...) that the system
Python lacks, so run it with the harness venv, from symbolic-explorer/:
    ../targets/sv-comp/scripts/.venv/bin/python3 -m unittest tests.test_property_precheck -v
"""

import json
import os
import subprocess
import sys
import unittest
from types import SimpleNamespace
from unittest import mock

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from driver import SVCompDriver as svcomp
from svcomp import PropertyPreCheck as precheck

RESULT = {"exception_free": True, "reason": "no throw", "assertion_free": False, "assertion_reason": "Main.main: assert"}


def args(**kw):
    a = dict(classpath=['/task/common', '/task/Main', '/libs/com.microsoft.z3.jar'], agent='/swat/executor.jar',
             java_path='java', no_property_precheck=False)
    a.update(kw)
    return SimpleNamespace(**a)


def completed(stdout):
    return subprocess.CompletedProcess(args=[], returncode=0, stdout=stdout, stderr='')


class PropertyPreCheckTest(unittest.TestCase):

    def run_check(self, prop, stdout=json.dumps(RESULT), **kw):
        check = precheck.PropertyPreCheck(args(**kw), prop)
        with mock.patch.object(precheck.subprocess, 'run', return_value=completed(stdout)) as run:
            proven = check.run()
        return check, proven, run

    def test_no_runtime_exception_uses_exception_freedom(self):
        check, proven, _ = self.run_check('no-runtime-exception.prp')
        self.assertTrue(proven)
        self.assertEqual(check.status, 'proven')
        self.assertEqual(check.reason, 'no throw')

    def test_valid_assert_uses_assertion_freedom(self):
        check, proven, _ = self.run_check('valid-assert.prp')
        self.assertFalse(proven)
        self.assertEqual(check.status, 'not_proven')
        self.assertEqual(check.reason, 'Main.main: assert')

    def test_command_leaves_out_z3(self):
        _, _, run = self.run_check('no-runtime-exception.prp')
        cmd = run.call_args[0][0]
        self.assertEqual(cmd[:4], ['java', '-Xmx2g', '-cp', '/swat/executor.jar'])
        self.assertEqual(cmd[-2:], ['/task/common', '/task/Main'])

    def test_other_property_and_flag_disable_it(self):
        for check, proven, run in (self.run_check('no-deadlock.prp'),
                                   self.run_check('no-runtime-exception.prp', no_property_precheck=True)):
            self.assertFalse(proven)
            self.assertEqual(check.status, 'disabled')
            run.assert_not_called()

    def test_garbage_output_proves_nothing(self):
        check, proven, _ = self.run_check('no-runtime-exception.prp', stdout='Exception in thread "main"')
        self.assertFalse(proven)
        self.assertEqual(check.status, 'failed')

    def test_timeout_proves_nothing(self):
        check = precheck.PropertyPreCheck(args(), 'no-runtime-exception.prp')
        with mock.patch.object(precheck.subprocess, 'run', side_effect=subprocess.TimeoutExpired('java', 60)):
            self.assertFalse(check.run())
        self.assertEqual(check.status, 'timeout')


class VerdictWithProofTest(unittest.TestCase):

    def setUp(self):
        self.errors = []
        db = SimpleNamespace(record_execution_error=lambda endpoint, kind, message: self.errors.append(kind))
        patcher = mock.patch.object(svcomp.Database, 'instance', return_value=db)
        patcher.start()
        self.addCleanup(patcher.stop)
        self.driver = svcomp.SVCompDriver.__new__(svcomp.SVCompDriver)
        self.driver.state = svcomp.State()
        self.driver.precheck = SimpleNamespace(proven=True, reason='no throw')

    def test_safe_when_round_0_would_explore(self):
        self.assertEqual(self.driver.verdict_with_proof(svcomp.Action.SYMBOLICNEXT), svcomp.Verdict.SAFE)
        self.assertEqual(self.errors, [])

    def test_safe_when_round_0_gave_up(self):
        self.driver.state.verdict = svcomp.Verdict.UNKNOWN
        self.assertEqual(self.driver.verdict_with_proof(svcomp.Action.REPORTVERDICT), svcomp.Verdict.SAFE)

    def test_violation_contradicts_the_proof(self):
        self.driver.state.verdict = svcomp.Verdict.VIOLATION
        self.assertEqual(self.driver.verdict_with_proof(svcomp.Action.REPORTVERDICT), svcomp.Verdict.UNKNOWN)
        self.assertEqual(self.errors, ['precheck_contradiction'])


if __name__ == '__main__':
    unittest.main()
