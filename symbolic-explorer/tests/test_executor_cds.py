"""Unit tests for the executor AppCDS archive (svcomp/ExecutorCDS.py).

Focus: the -Xshare:dump command must declare the same modules the executor JVMs add implicitly via
-javaagent (java.instrument), so the archive's jdk.module.addmods matches at map time. Without it
the JVM rejects the archive from round DUMP_AT_ROUND on ("Mismatched values for property
jdk.module.addmods ... java.instrument") and the optimization is silently defeated.
"""
import os
import sys
import tempfile
import unittest
from types import SimpleNamespace
from unittest import mock

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from svcomp import ExecutorCDS as ecds
from svcomp.ExecutorCDS import ExecutorCDS, DUMP_ADD_MODULES


class DumpCommandTest(unittest.TestCase):
    def _prepared(self, tmp):
        """An ExecutorCDS ready to dump: a real dir, a non-empty class list, a jar-only prefix."""
        cds = ExecutorCDS(enabled=True, dump_at_round=3)
        cds.dir = tmp
        cds.classlist = os.path.join(tmp, 'classes.lst')
        cds.archive = os.path.join(tmp, 'executor.jsa')
        with open(cds.classlist, 'w') as f:
            f.write('java/lang/Object\n')
        cds.layout = ['/libs/agent.jar', '/libs/z3.jar', '/task/build']
        return cds

    def test_dump_cmd_declares_javaagent_modules(self):
        with tempfile.TemporaryDirectory() as tmp:
            cds = self._prepared(tmp)
            with mock.patch.object(ecds.subprocess, 'run',
                                   return_value=SimpleNamespace(returncode=0)) as run:
                # archive file is not actually written (run is mocked), so the dump is recorded as
                # failed; we only care about the command that was built.
                cds.prepare_round(3, 'java', lambda: ['-Xmx244m'])
            run.assert_called_once()
            cmd = run.call_args[0][0]
            self.assertIn('--add-modules', cmd)
            self.assertEqual(cmd[cmd.index('--add-modules') + 1], ','.join(DUMP_ADD_MODULES))
            self.assertIn('java.instrument', cmd[cmd.index('--add-modules') + 1])
            # must take effect for this dump: before -Xshare:dump, and the heap options are kept
            self.assertLess(cmd.index('--add-modules'), cmd.index('-Xshare:dump'))
            self.assertIn('-Xmx244m', cmd)

    def test_no_dump_before_dump_at_round(self):
        with tempfile.TemporaryDirectory() as tmp:
            cds = self._prepared(tmp)
            with mock.patch.object(ecds.subprocess, 'run') as run:
                cds.prepare_round(2, 'java', lambda: [])
            run.assert_not_called()


if __name__ == '__main__':
    unittest.main()
