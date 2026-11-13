"""
Unit tests for the class preparation of the SV-COMP driver (svcomp/ClassPreparation.py).

Run from symbolic-explorer/:
    python3 -m unittest tests.test_class_preparation -v
The test of a real preparation needs the agent jar (./gradlew :symbolic-executor:copyJar).
"""
import json
import os
import shutil
import sys
import tempfile
import unittest
from types import SimpleNamespace

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from svcomp.ClassPreparation import ClassPreparation

SWAT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
AGENT = os.path.join(SWAT, 'symbolic-executor', 'lib', 'symbolic-executor.jar')
CONFIG = os.path.join(SWAT, 'targets', 'sv-comp', 'sv-comp.cfg')
BENCH = os.path.join(SWAT, 'targets', 'sv-comp', 'sv-benchmarks', 'java')
Z3_JAR = os.path.join(SWAT, 'libs', 'java-library-path', 'com.microsoft.z3.jar')


class ClassPreparationTest(unittest.TestCase):

    def setUp(self):
        self.logdir = tempfile.mkdtemp(prefix='test-class-preparation-')

    def tearDown(self):
        shutil.rmtree(self.logdir, ignore_errors=True)

    def args(self, **kw):
        base = dict(agent=AGENT, config=CONFIG, java_path='java', logdir=self.logdir, classpath=[])
        base.update(kw)
        return SimpleNamespace(**base)

    def test_disabled(self):
        prep = ClassPreparation(self.args(no_prepare_classes=True, classpath=['x']))
        self.assertIsNone(prep.run())
        self.assertEqual(prep.stats()['status'], 'disabled')

    def test_a_failure_keeps_the_original_classpath(self):
        prep = ClassPreparation(self.args(agent=os.path.join(self.logdir, 'missing.jar'), classpath=[self.logdir]))
        self.assertIsNone(prep.run())
        self.assertEqual(prep.status, 'failed')
        self.assertIsNotNone(prep.reason)
        self.assertIsNotNone(prep.stats()['duration_s'])

    @unittest.skipUnless(os.path.isfile(AGENT) and os.path.isdir(BENCH), 'needs the agent jar and sv-benchmarks')
    def test_directories_are_replaced_by_their_prepared_copies(self):
        task = os.path.join(BENCH, 'jbmc-regression', 'aastore_aaload1')
        common = os.path.join(BENCH, 'common')
        cp = [common, task, Z3_JAR]
        prep = ClassPreparation(self.args(classpath=cp))
        prepared = prep.run()
        self.assertEqual(prep.status, 'prepared', prep.reason)
        self.assertEqual(len(prepared), len(cp))
        out = os.path.join(self.logdir, 'prepared')
        self.assertEqual(prepared, [os.path.join(out, '0'), os.path.join(out, '1'), Z3_JAR])
        self.assertTrue(os.path.isfile(os.path.join(out, '1', 'Main.class')))
        # The class is rewritten and marked; the benchmark's Verifier (excluded) is copied unchanged
        with open(os.path.join(out, '1', 'Main.class'), 'rb') as f:
            self.assertIn(b'Lde/uzl/its/swat/Prepared;', f.read())
        with open(os.path.join(out, '0', 'org', 'sosy_lab', 'sv_benchmarks', 'Verifier.class'), 'rb') as f, \
                open(os.path.join(common, 'org', 'sosy_lab', 'sv_benchmarks', 'Verifier.class'), 'rb') as g:
            self.assertEqual(f.read(), g.read())
        self.assertGreater(prep.prepared, 0)
        with open(os.path.join(out, 'prepared.json')) as f:
            self.assertEqual(json.load(f)['classpath'], prepared)


if __name__ == '__main__':
    unittest.main()
