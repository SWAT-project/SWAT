"""
Unit tests for the Database lock and for traces that cannot be added: a failure must neither leave
the lock held (the driver would wait for it forever) nor go unnoticed.

Run from symbolic-explorer/ with the harness venv:
    ../targets/sv-comp/scripts/.venv/bin/python3 -m unittest tests.test_database -v
"""

import os
import sys
import unittest
from unittest import mock

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from data import Database as database
from data.BinaryExecutionTree.Tree import Tree
from data.trace.Branch import Branch


class DatabaseLockTest(unittest.TestCase):

    def test_failing_insert_releases_the_lock(self):
        db = database.Database.instance()
        with mock.patch.object(Tree, 'add', side_effect=RecursionError('boom')):
            with self.assertRaises(RecursionError):
                db.add_trace('lock-test', 't', [Branch(1, 't', True, '(c1)')], [], [], False, False, reference_semantic_change=True)
        self.assertTrue(database.lock.acquire(timeout=1))
        database.lock.release()
        # The flags were recorded before the insert failed.
        self.assertTrue(db.get_tree('lock-test').reference_semantic_change)

    def test_ingest_failure_is_recorded(self):
        from constraint.ConstraintService import ConstraintService
        db = database.Database.instance()
        with mock.patch.object(Tree, 'add', side_effect=RecursionError('boom')):
            ConstraintService.add_constraints('ingest-test', 't', [], [], [], False, False)
        kinds = [e['kind'] for e in db.get_tree('ingest-test').execution_errors]
        self.assertEqual(kinds, ['trace_ingest_failed'])

    def test_ingest_failure_keeps_a_stack_overflow(self):
        # The driver must still drop that execution's violation (SVCompDriver.round_has_stack_overflow).
        from constraint.ConstraintService import ConstraintService
        from parse.TraceParser import Parser
        db = database.Database.instance()
        with mock.patch.object(Parser, 'parse_trace', side_effect=RecursionError('boom')):
            ConstraintService.add_constraints('so-ingest-test', 't-so', [], [], [], True, False, stack_overflow=True)
        self.assertEqual(db.get_witness_data('so-ingest-test')['trace_id'], 't-so')
        self.assertTrue(db.is_stack_overflow_trace('so-ingest-test', 't-so'))


if __name__ == '__main__':
    unittest.main()
