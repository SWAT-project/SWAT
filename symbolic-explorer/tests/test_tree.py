"""
Unit tests for the execution tree: adding traces (also very long ones, and ones that do not fit),
the UNMODELLED_EXCEPTION marker, and copying.

Run from symbolic-explorer/ with the harness venv (dfs imports the explorer's dependencies):
    ../targets/sv-comp/scripts/.venv/bin/python3 -m unittest tests.test_tree -v
"""

import copy
import os
import sys
import threading
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from data.BinaryExecutionTree.Tree import Tree
from data.BinaryExecutionTree.Node import Node
from data.BinaryExecutionTree.Leaf import Leaf
from data.trace.Branch import Branch
from data.trace.Special import Special, UNMODELLED_EXCEPTION
from strategy.DFS import dfs

# Longer than any recursion limit the interpreter would allow on a thread's C stack.
DEEP = 50_000


def b(id, taken, trace_id='t'):
    return Branch(id=id, trace_id=trace_id, has_branched=taken, constraint=f'(c{id})')


def s(id, inst='INVOKEVIRTUAL', trace_id='t'):
    return Special(id=id, trace_id=trace_id, has_branched=True, inst=inst)


def path(tree):
    """The nodes from the root along the side each node has a child on (branched first)."""
    out, node = [], tree.root
    while isinstance(node, Node):
        out.append(node)
        node = node.branched if node.branched is not None else node.skipped
    return out, node


def in_thread(fn):
    """Runs fn on a fresh thread, as the explorer does, and returns its result or raises its error."""
    result = {}
    def run():
        try:
            result['value'] = fn()
        except BaseException as e:
            result['error'] = e
    t = threading.Thread(target=run)
    t.start()
    t.join()
    if 'error' in result:
        raise result['error']
    return result.get('value')


class TreeAddTest(unittest.TestCase):

    def test_single_trace_is_a_path_ending_in_a_leaf(self):
        tree = Tree(0)
        self.assertTrue(tree.add([b(1, True), s(2), b(3, False)], [], []))
        nodes, end = path(tree)
        self.assertEqual([n.id for n in nodes], [1, 2, 3])
        self.assertEqual([n.kind for n in nodes], ['Branch', 'Special', 'Branch'])
        self.assertIsInstance(end, Leaf)
        self.assertIs(nodes[0].branched, nodes[1])
        self.assertIs(nodes[2].skipped, end)
        self.assertIs(nodes[1].parent, nodes[0])
        self.assertEqual(nodes[0].constraint, {'t': '(c1)'})

    def test_empty_trace_is_a_leaf(self):
        tree = Tree(0)
        self.assertTrue(tree.add([], [], []))
        self.assertIsInstance(tree.root, Leaf)

    def test_second_trace_shares_the_prefix_and_adds_the_other_side(self):
        tree = Tree(0)
        tree.add([b(1, True), b(2, True)], [], [])
        tree.add([b(1, True, 'u'), b(2, False, 'u'), b(3, True, 'u')], [], [])
        n1 = tree.root
        n2 = n1.branched
        self.assertIsNone(n1.skipped)
        self.assertIsInstance(n2.branched, Leaf)
        self.assertEqual(n2.skipped.id, 3)
        self.assertEqual(n2.constraint, {'t': '(c2)', 'u': '(c2)'})

    def test_trace_replaces_a_leaf(self):
        tree = Tree(0)
        tree.add([b(1, True)], [], [])
        tree.add([b(1, True), b(2, False)], [], [])
        self.assertEqual(tree.root.branched.id, 2)

    def test_shorter_trace_keeps_the_longer_path(self):
        tree = Tree(0)
        tree.add([b(1, True), b(2, False)], [], [])
        self.assertTrue(tree.add([b(1, True)], [], []))
        self.assertEqual(tree.root.branched.id, 2)

    def test_mismatch_keeps_the_tree_and_records_an_incomplete_search(self):
        tree = Tree(0)
        tree.add([b(1, True), b(2, True)], [], [])
        self.assertFalse(tree.add([b(1, True), b(7, True), b(8, True)], [], []))
        nodes, _ = path(tree)
        self.assertEqual([n.id for n in nodes], [1, 2])
        self.assertEqual([e['kind'] for e in tree.execution_errors], ['tree_mismatch'])

    def test_kind_mismatch_at_the_same_id_does_not_fit(self):
        tree = Tree(0)
        tree.add([b(1, True), b(2, True)], [], [])
        self.assertFalse(tree.add([b(1, True), s(2)], [], []))
        self.assertEqual(tree.root.branched.kind, 'Branch')


class UnmodelledExceptionTest(unittest.TestCase):
    """The marker is only in the trace of the run that throws (#65, StringBuilderChars03)."""

    def test_marker_is_not_a_level_of_the_tree(self):
        tree = Tree(0)
        # A run that does not throw at the call, then one that does and ends there.
        self.assertTrue(tree.add([b(1, True), s(5), b(2, True)], [], []))
        self.assertTrue(tree.add([b(1, True), s(5), s(5, UNMODELLED_EXCEPTION)], [], []))
        self.assertTrue(tree.add([b(1, True), s(5), b(2, False)], [], []))
        nodes, _ = path(tree)
        self.assertEqual([n.id for n in nodes], [1, 5, 2])
        self.assertEqual(tree.execution_errors, [])

    def test_throwing_run_first(self):
        tree = Tree(0)
        self.assertTrue(tree.add([b(1, True), s(5), s(5, UNMODELLED_EXCEPTION)], [], []))
        self.assertTrue(tree.add([b(1, True), s(5), b(2, True)], [], []))
        nodes, _ = path(tree)
        self.assertEqual([n.id for n in nodes], [1, 5, 2])
        self.assertFalse(any(n.unmodelled_exception for n in nodes))

    def test_marker_is_kept_on_the_next_node(self):
        tree = Tree(0)
        tree.add([b(1, True), s(5), s(5, UNMODELLED_EXCEPTION), b(9, True)], [], [])
        nodes, _ = path(tree)
        self.assertEqual([n.id for n in nodes], [1, 5, 9])
        self.assertEqual([n.unmodelled_exception for n in nodes], [False, False, True])
        # A run that reaches the node without the exception does not clear the mark.
        tree.add([b(1, True), s(5), b(9, False)], [], [])
        self.assertTrue(nodes[2].unmodelled_exception)

    def test_mark_set_by_a_later_trace_on_an_existing_node(self):
        tree = Tree(0)
        tree.add([b(1, True), s(5), b(9, True)], [], [])
        tree.add([b(1, True), s(5), s(5, UNMODELLED_EXCEPTION), b(9, True)], [], [])
        self.assertTrue(path(tree)[0][2].unmodelled_exception)

    def test_handler_path_after_the_exception_does_not_fit(self):
        # The throwing run continues in a handler: same branches, different behaviour, with no
        # branch to tell them apart. The tree keeps the first path and the search is incomplete.
        tree = Tree(0)
        tree.add([b(1, True), s(5), b(2, True)], [], [])
        self.assertFalse(tree.add([b(1, True), s(5), s(5, UNMODELLED_EXCEPTION), b(30, True)], [], []))
        self.assertEqual([e['kind'] for e in tree.execution_errors], ['tree_mismatch'])

    def test_structural_drops_markers_for_the_divergence_check(self):
        trace = [b(1, True), s(5), s(5, UNMODELLED_EXCEPTION), b(9, True)]
        self.assertEqual([(e.id, m) for e, m in Tree.structural(trace)], [(1, False), (5, False), (9, True)])


class DeepTreeTest(unittest.TestCase):
    """Traces far longer than the recursion limit (#59): a StackOverflowError on a symbolic path."""

    def deep_trace(self, trace_id='t'):
        return [b(i % 7, i % 3 == 0, trace_id) for i in range(DEEP)]

    def test_add_copy_and_walk_a_deep_tree(self):
        def work():
            tree = Tree(0)
            tree.add(self.deep_trace(), [], [])
            # A second trace that takes the other side halfway down.
            k = DEEP // 2
            flipped = self.deep_trace('u')[:k + 1]
            flipped[k] = b(flipped[k].id, not flipped[k].has_branched, 'u')
            assert tree.add(flipped, [], [])
            copied = copy.deepcopy(tree)
            candidates = dfs(set(), copied, copied.root, set(), set())
            return tree, copied, candidates
        tree, copied, candidates = in_thread(work)
        nodes, leaves, work = [], [], [copied.root]
        while work:
            n = work.pop()
            if isinstance(n, Node):
                nodes.append(n)
                work += [c for c in (n.skipped, n.branched) if c is not None]
            else:
                leaves.append(n)
        self.assertEqual((len(nodes), len(leaves)), (DEEP, 2))
        self.assertIsNot(copied.root, tree.root)
        nodes, _ = path(copied)
        self.assertIs(nodes[1].parent, nodes[0])
        # Every node has one side unexplored, but the one where the second trace left the first.
        self.assertEqual(len(candidates), DEEP - 1)
        self.assertEqual(tree.execution_errors, [])
        self.assertEqual(candidates[0].gid, copied.root.gid)

    def test_copy_keeps_shared_inputs_shared(self):
        inputs = [object()]
        tree = Tree(0)
        tree.add([b(1, True), b(2, True)], inputs, [])
        copied = copy.deepcopy(tree)
        self.assertIs(copied.root.inputs, copied.root.branched.inputs)
        self.assertIsNot(copied.root.inputs, inputs)


class DfsOrderTest(unittest.TestCase):

    def test_pre_order_skipped_side_first(self):
        tree = Tree(0)
        tree.add([b(1, False), b(2, False), b(3, True)], [], [])
        tree.add([b(1, True), b(4, True)], [], [])
        tree.add([b(1, False), b(2, True), b(5, True)], [], [])
        self.assertEqual([n.id for n in dfs(set(), tree, tree.root, set(), set())], [3, 5, 4])

    def test_specials_are_passed_through(self):
        tree = Tree(0)
        tree.add([s(10), b(1, True), s(11), b(2, False)], [], [])
        self.assertEqual([n.id for n in dfs(set(), tree, tree.root, set(), set())], [1, 2])


if __name__ == '__main__':
    unittest.main()
