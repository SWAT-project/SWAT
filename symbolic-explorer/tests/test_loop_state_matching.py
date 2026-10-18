"""
Unit tests for loop-head state matching: Tree positions the loop-head states the executor reports
with a trace, and dfs() leaves out the branches below a state that was reached at a smaller
position already.

Run from symbolic-explorer/:
    python3 -m unittest tests.test_loop_state_matching -v
"""

import os
import sys
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from data.BinaryExecutionTree.Tree import Tree
from data.trace.Branch import Branch
from strategy.DFS import dfs

SYMBOLIC = '(declare-fun Z_0 () Bool)(assert Z_0)'


def trace(trace_id, *steps):
    """Steps are (iid, taken) or (iid, taken, states)."""
    branches = []
    for step in steps:
        iid, taken = step[0], step[1]
        states = step[2] if len(step) > 2 else ()
        branches.append(Branch(id=iid, trace_id=trace_id, has_branched=taken, constraint=SYMBOLIC,
                               loop_states=tuple(states)))
    return branches


def open_branches(tree):
    tree.loop_state_pruned = 0
    return sorted(n.id for n in dfs(set(), tree, tree.root, set(), set()))


class LoopStateMatchingTest(unittest.TestCase):

    def setUp(self):
        Tree.LOOP_STATE_MATCHING = True

    def tearDown(self):
        Tree.LOOP_STATE_MATCHING = True

    def test_positions(self):
        tree = Tree(0)
        t = trace('t1', (1, True, ['A']), (2, False), (3, True, ['B', 'A']))
        tree.add(t, [], [])
        # (branches before, their outcomes as a binary number, states before)
        self.assertEqual(tree.loop_state_first, {'A': (0, 0, 0), 'B': (2, 0b10, 1)})
        self.assertEqual(tree.root.branched.skipped.loop_states, (('B', (2, 0b10, 1)), ('A', (2, 0b10, 2))))

    def test_repeated_state_on_one_path(self):
        tree = Tree(0)
        # A loop whose state repeats: the second visit of A comes before branch 3
        tree.add(trace('t1', (1, True, ['A']), (2, True), (3, True, ['A']), (4, True)), [], [])
        self.assertEqual(open_branches(tree), [1, 2])
        self.assertEqual(tree.loop_state_pruned, 1)

    def test_state_reached_earlier_on_another_path(self):
        tree = Tree(0)
        tree.add(trace('t1', (1, True), (2, True), (3, True, ['S']), (4, True)), [], [])
        self.assertEqual(open_branches(tree), [1, 2, 3, 4])
        # Another path reaches S after one branch: below the first occurrence nothing is new
        tree.add(trace('t2', (1, False, []), (5, True, ['S']), (6, True)), [], [])
        self.assertEqual(open_branches(tree), [2, 5, 6])

    def test_same_depth_ordered_by_outcomes(self):
        tree = Tree(0)
        tree.add(trace('t1', (1, True), (2, True, ['S']), (3, True)), [], [])
        tree.add(trace('t2', (1, False), (4, True, ['S']), (5, True)), [], [])
        # Both after one branch; the path with outcome 0 (not taken) comes first
        self.assertEqual(open_branches(tree), [4, 5])

    def test_equal_positions_do_not_prune(self):
        tree = Tree(0)
        tree.add(trace('t1', (1, True, ['S']), (2, True, ['S'])), [], [])
        # the second visit is after a branch, the first not: only the second is dominated
        self.assertEqual(open_branches(tree), [1])
        tree2 = Tree(0)
        tree2.add(trace('t1', (1, True, ['S']), (2, True)), [], [])
        tree2.add(trace('t2', (1, True, ['S']), (2, False)), [], [])
        self.assertEqual(open_branches(tree2), [1])

    def test_disabled(self):
        Tree.LOOP_STATE_MATCHING = False
        tree = Tree(0)
        tree.add(trace('t1', (1, True, ['A']), (2, True), (3, True, ['A']), (4, True)), [], [])
        self.assertEqual(open_branches(tree), [1, 2, 3, 4])


if __name__ == '__main__':
    unittest.main()
