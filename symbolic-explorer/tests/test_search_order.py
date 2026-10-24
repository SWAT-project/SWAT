"""
Unit tests for the search orders of strategy/StrategyService (--search-order): node depth, the
instruction and side a flip takes, and the sides the tree's traces covered.

Run from symbolic-explorer/:
    ../targets/sv-comp/scripts/.venv/bin/python3 -m unittest tests.test_search_order -v
"""

import os
import sys
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from data.BinaryExecutionTree.Node import Node
from strategy.StrategyService import depth, open_side, covered_sides, first_coverers, trace_scores

LOOP = 1 << 58  # one loop iteration in an iid


def node(iid, parent=None, gid=0, trace='t1'):
    n = Node.__new__(Node)
    n.id = iid
    n.gid = gid
    n.trace_id = trace
    n.kind = "Branch"
    n.parent = parent
    n.branched = None
    n.skipped = None
    return n


class SearchOrderTest(unittest.TestCase):

    def setUp(self):
        # root (iid 1) took its branched side twice around a loop (iid 2, then 2 + LOOP);
        # the second iteration also went to the skipped side of iid 3, where the trace ended.
        # (t1 ran first; t2 diverged from it at b's skipped side and added c.)
        self.root = node(1, gid=1)
        self.a = node(2, self.root, gid=2)
        self.root.branched = self.a
        self.b = node(2 + LOOP, self.a, gid=3)
        self.a.branched = self.b
        self.c = node(3, self.b, gid=4, trace='t2')
        self.b.skipped = self.c
        self.c.skipped = object()  # a leaf

    def test_depth_counts_the_branches_above(self):
        self.assertEqual([depth(n) for n in (self.root, self.a, self.b, self.c)], [0, 1, 2, 3])

    def test_open_side_ignores_the_loop_count(self):
        # a and b are the same instruction in different iterations: a has its skipped side open,
        # b (which took the skipped side) its branched side
        self.assertEqual(open_side(self.a), (2, False))
        self.assertEqual(open_side(self.b), (2, True))
        # c took its skipped side, so flipping it takes the branched one
        self.assertEqual(open_side(self.c), (3, True))

    def test_covered_sides_are_those_some_trace_took(self):
        covered = covered_sides(self.root)
        self.assertEqual(covered, {(1, True), (2, True), (2, False), (3, False)})
        # The two iterations took both sides of instruction 2: flipping either reaches nothing new.
        self.assertIn(open_side(self.a), covered)
        self.assertIn(open_side(self.b), covered)
        self.assertNotIn(open_side(self.c), covered)
        self.assertNotIn(open_side(self.root), covered)


    def test_traces_score_the_sides_they_took_first(self):
        first = first_coverers(self.root)
        self.assertIs(first[(2, True)], self.a)  # a was there before b
        # t1 took 1-branched, 2-branched and 2-skipped (via b) first, t2 3-skipped
        self.assertEqual(trace_scores(first), {'t1': 3, 't2': 1})


if __name__ == '__main__':
    unittest.main()
