"""
Unit tests for the static-analysis graph walk that DFS runs in lockstep with the execution tree.

Graphs are built as the extractor's JSON and loaded through SAGraph.load_json_graph(), so the
loader's invariant checks run too. Execution trees are built by hand: `dfs` only descends into real
`Node` instances, so they are created with Node.__new__ and their fields set directly.

Run from symbolic-explorer/:
    python3 -m unittest tests.test_sa_walk -v
"""

import json
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from data.BinaryExecutionTree.Node import Node
from data.BinaryExecutionTree.Leaf import Leaf
from data.StaticAnalysisGraph import SAGraph as sagraph
from data.StaticAnalysisGraph.SAGraph import SAGraph, mark_assertion_path, SANode
from strategy.DFS import dfs

AIOOBE_CHAIN = ['java.lang.ArrayIndexOutOfBoundsException', 'java.lang.IndexOutOfBoundsException',
                'java.lang.RuntimeException', 'java.lang.Exception', 'java.lang.Throwable']
ARITH_CHAIN = ['java.lang.ArithmeticException', 'java.lang.RuntimeException', 'java.lang.Exception',
               'java.lang.Throwable']


class GraphBuilder:
    """Builds a graph in the extractor's JSON format."""

    def __init__(self):
        self.nodes = []
        self.edges = []
        self.exits = []
        self.asserts = []
        self.entry = None

    def node(self, id, exit=False, incomplete=False, chain=None, assertion=False):
        n = {'id': id, 'statement': id, 'stmtType': 'X', 'lineNumber': -1}
        if incomplete:
            n['isIncomplete'] = True
            self.asserts.append(id)
        if chain is not None:
            n['exceptionType'] = chain[0]
            n['exceptionTypeChain'] = chain
        if exit:
            self.exits.append(id)
        if assertion:
            self.asserts.append(id)
        if self.entry is None:
            self.entry = id
        self.nodes.append(n)
        return self

    def nodes_(self, *ids):
        for id in ids:
            self.node(id)
        return self

    def edge(self, source, target, type='NORMAL', **extra):
        e = {'source': source, 'target': target, 'type': type}
        e.update(extra)
        self.edges.append(e)
        return self

    def branch(self, source, false_target, true_target, phantom=False):
        prefix = 'PHANTOM_' if phantom else ''
        self.edge(source, false_target, prefix + 'FALSE_BRANCH')
        self.edge(source, true_target, prefix + 'TRUE_BRANCH')
        return self

    def call(self, site, callee_entry, return_site, callee_exits=(), handlers=None):
        self.edge(site, callee_entry, 'CALL', returnSite=return_site, handlers=handlers or {})
        for exit in callee_exits:
            self.edge(exit, return_site, 'RETURN')
        return self

    def load(self) -> SAGraph:
        graph = {'nodes': self.nodes, 'edges': self.edges, 'entryNodeId': self.entry,
                 'exitNodeIds': self.exits,
                 'metadata': {'assertionPointIds': self.asserts,
                              'isIncomplete': any(n.get('isIncomplete') for n in self.nodes)}}
        with tempfile.NamedTemporaryFile('w', suffix='.json', delete=False) as f:
            json.dump(graph, f)
            path = f.name
        try:
            g = SAGraph()
            g.load_json_graph(path)
            return g
        finally:
            os.unlink(path)


def branch_node(name, skipped=None, branched=None):
    """A tree node for one branch event; `None` children are unexplored sides."""
    n = Node.__new__(Node)
    n.parent = None
    n.id = name
    n.trace_id = name
    n.gid = name
    n.kind = 'Branch'
    n.inst = None
    n.constraint = {}
    n.skipped = skipped
    n.branched = branched
    return n


def leaf():
    lf = Leaf.__new__(Leaf)
    lf.parent = None
    lf.inputs = []
    return lf


def candidates(tree_root, sa_graph):
    """The unexplored branches dfs considers worth solving, by tree node name."""
    return {n.id for n in dfs(set(), None, tree_root, set(), set(), sa_graph.entry_node)}


def shared_callee_graph():
    """
    main: c1 = check(); c2 = check(); if (..) assert  -- check() has one branch and two exits.

        e0 -> c1 -CALL-> k0 ... RETURN -> r1 -> c2 -CALL-> k0 ... RETURN -> r2 -> bm
        bm: FALSE -> a (assert), TRUE -> x (exit)
        check: k0 -> kb: FALSE -> kx1 (exit), TRUE -> kx2 (exit)
    """
    return (GraphBuilder()
            .nodes_('e0', 'c1', 'r1', 'c2', 'r2', 'bm').node('a', assertion=True).node('x', exit=True)
            .node('k0').node('kb').node('kx1', exit=True).node('kx2', exit=True)
            .edge('e0', 'c1')
            .call('c1', 'k0', 'r1', ['kx1', 'kx2'])
            .edge('r1', 'c2')
            .call('c2', 'k0', 'r2', ['kx1', 'kx2'])
            .edge('r2', 'bm')
            .branch('bm', 'a', 'x')
            .edge('a', 'x')
            .edge('k0', 'kb')
            .branch('kb', 'kx1', 'kx2'))


class LoaderTest(unittest.TestCase):

    def test_return_fan_out_loads(self):
        # One exit returning to two call sites: RETURN used to share the one fall-through slot.
        g = shared_callee_graph().load()
        self.assertEqual({n.id for n in g.nodes['kx1'].next_returns}, {'r1', 'r2'})
        self.assertIsNone(g.nodes['kx1'].next_fallthrough)
        self.assertIs(g.nodes['c1'].return_site, g.nodes['r1'])
        self.assertTrue(g.nodes['kx1'].isExit)

    def test_unknown_edge_type_is_rejected(self):
        with self.assertRaises(ValueError):
            GraphBuilder().nodes_('a', 'b').edge('a', 'b', 'TELEPORT').load()

    def test_marking_long_chain_does_not_recurse(self):
        # The old recursive marking hit the recursion limit on long predecessor chains.
        nodes = [SANode(str(i)) for i in range(50_000)]
        for a, b in zip(nodes, nodes[1:]):
            b.prev.append(a)
        limit = sys.getrecursionlimit()
        sys.setrecursionlimit(1000)
        try:
            mark_assertion_path(nodes[-1])
        finally:
            sys.setrecursionlimit(limit)
        self.assertTrue(nodes[0].reachesAssertSameLevel)


class WalkTest(unittest.TestCase):

    def test_branch_free_call_is_crossed_in_one_walk(self):
        g = (GraphBuilder().nodes_('e0', 'c1', 'r1', 'bm', 'f0').node('x', exit=True).node('fx', exit=True)
             .edge('e0', 'c1').call('c1', 'f0', 'r1', ['fx']).edge('f0', 'fx')
             .edge('r1', 'bm').branch('bm', 'x', 'x').load())
        node, stack = g.entry_node.walk_till_branch(None)
        self.assertEqual(node.id, 'bm')
        self.assertIsNone(stack)

    def test_return_goes_to_the_calling_site(self):
        g = shared_callee_graph().load()
        kb, stack = g.nodes['c2'].walk_till_branch(None)
        self.assertEqual(kb.id, 'kb')
        node, stack = kb.get_branched_child().walk_till_branch(stack)
        self.assertEqual(node.id, 'bm')
        self.assertIsNone(stack)

    def test_exit_with_empty_stack_gives_up(self):
        g = shared_callee_graph().load()
        self.assertIsNone(g.nodes['kx1'].walk_till_branch(None))

    def test_incomplete_node_gives_up(self):
        g = (GraphBuilder().nodes_('e0').node('i', incomplete=True).node('b').node('x', exit=True)
             .edge('e0', 'i').edge('i', 'b').branch('b', 'x', 'x').load())
        self.assertIsNone(g.entry_node.walk_till_branch(None))

    def test_branch_free_unbounded_recursion_terminates(self):
        g = (GraphBuilder().nodes_('e0', 'c', 'r').node('x', exit=True)
             .edge('e0', 'c').call('c', 'e0', 'r', ['x']).edge('r', 'x').load())
        self.assertIsNone(g.entry_node.walk_till_branch(None))

    def test_branch_free_infinite_loop_terminates(self):
        g = GraphBuilder().nodes_('a', 'b').edge('a', 'b').edge('b', 'a').load()
        self.assertIsNone(g.entry_node.walk_till_branch(None))


def inter_method_catch_graph():
    """
    InterMethodCatch: main calls test() inside try/catch(Exception); test()'s array store guard
    fails into uncaught[AIOOBE], which main's handler catches.

        main: e0 -> c -CALL-> t0 ; r (return site) -> mx (exit)
              handler h -> hb: FALSE -> a (assert), TRUE -> mx
        test: t0 -> g: PHANTOM_TRUE -> tx (exit), PHANTOM_FALSE -> u (uncaught[AIOOBE])
    """
    return (GraphBuilder()
            .nodes_('e0', 'c', 'r', 'h', 'hb').node('a', assertion=True).node('mx', exit=True)
            .node('t0').node('g').node('tx', exit=True).node('u', chain=AIOOBE_CHAIN)
            .edge('e0', 'c')
            .call('c', 't0', 'r', ['tx'], handlers={'java.lang.Exception': 'h'})
            .edge('r', 'mx')
            .edge('h', 'hb').branch('hb', 'a', 'mx').edge('a', 'mx')
            .edge('t0', 'g').branch('g', 'u', 'tx', phantom=True)
            .edge('u', 'h', 'EXCEPTIONAL_RETURN'))


class UnwindTest(unittest.TestCase):

    def test_escaping_exception_resumes_at_callers_handler(self):
        g = inter_method_catch_graph().load()
        g_node, stack = g.entry_node.walk_till_branch(None)
        self.assertEqual(g_node.id, 'g')
        node, stack = g_node.get_fallthrough_child().walk_till_branch(stack)
        self.assertEqual(node.id, 'hb')
        self.assertIsNone(stack)

    def test_unwinding_skips_frames_that_do_not_catch(self):
        # main -> middle (catches only UnsupportedOperationException) -> inner: ArithmeticException
        g = (GraphBuilder()
             .nodes_('e0', 'c1', 'r1', 'h', 'hb').node('a', assertion=True).node('mx', exit=True)
             .nodes_('m0', 'c2', 'hm').node('mret', exit=True)
             .nodes_('i0', 'ig').node('ix', exit=True).node('u', chain=ARITH_CHAIN)
             .edge('e0', 'c1')
             .call('c1', 'm0', 'r1', ['mret'], handlers={'java.lang.ArithmeticException': 'h'})
             .edge('r1', 'mx').edge('h', 'hb').branch('hb', 'a', 'mx').edge('a', 'mx')
             .edge('m0', 'c2')
             .call('c2', 'i0', 'mret', ['ix'], handlers={'java.lang.UnsupportedOperationException': 'hm'})
             .edge('hm', 'mret')
             .edge('i0', 'ig').branch('ig', 'u', 'ix', phantom=True)
             .edge('u', 'h', 'EXCEPTIONAL_RETURN')
             .load())
        ig, stack = g.entry_node.walk_till_branch(None)
        self.assertEqual((ig.id, sagraph.depth(stack)), ('ig', 2))
        node, stack = ig.get_fallthrough_child().walk_till_branch(stack)
        self.assertEqual(node.id, 'hb')
        self.assertIsNone(stack)

    def test_most_specific_handler_of_a_frame_wins(self):
        g = (GraphBuilder().nodes_('c', 'hr', 'he').node('u', chain=ARITH_CHAIN)
             .edge('c', 'u', 'CALL', returnSite='hr',
                   handlers={'java.lang.Exception': 'he', 'java.lang.RuntimeException': 'hr'})
             .load())
        node, stack = sagraph.unwind(g.nodes['u'].exceptionChain, sagraph.push(None, g.nodes["c"]))
        self.assertEqual(node.id, 'hr')

    def test_exception_leaving_main_gives_up(self):
        g = (GraphBuilder().nodes_('e0', 'g').node('x', exit=True).node('u', chain=AIOOBE_CHAIN)
             .edge('e0', 'g').branch('g', 'u', 'x', phantom=True).load())
        self.assertIsNone(g.nodes['u'].walk_till_branch(None))


class DfsTest(unittest.TestCase):

    def test_second_return_pairs_with_callers_next_branch(self):
        # Events: kb (1st call), kb (2nd call), bm. bm's TRUE side leads straight to main's exit,
        # so its unexplored branched side must be pruned. That only happens if the second return
        # resumed at r2: resuming at r1 would pair bm's event with kb again, whose sides are
        # interesting, and losing track altogether would make everything interesting.
        g = shared_callee_graph().load()
        n3 = branch_node('n3', skipped=leaf(), branched=None)
        n2 = branch_node('n2', skipped=n3, branched=leaf())
        n1 = branch_node('n1', skipped=n2, branched=leaf())
        self.assertEqual(candidates(n1, g), set())

    def test_unexplored_side_leading_to_assert_is_kept(self):
        g = shared_callee_graph().load()
        n3 = branch_node('n3', skipped=None, branched=leaf())
        n2 = branch_node('n2', skipped=n3, branched=leaf())
        n1 = branch_node('n1', skipped=n2, branched=leaf())
        self.assertEqual(candidates(n1, g), {'n3'})

    def test_guard_failure_is_followed_into_callers_handler(self):
        # Events: the guard in test() fails (skipped), then the branch in main's handler. Its TRUE
        # side skips the assert, so the unexplored branched side is uninteresting -- which the walk
        # can only tell if it unwound from uncaught[AIOOBE] into main's handler. Stopping at the
        # uncaught node means "no information" and keeps it.
        g = inter_method_catch_graph().load()
        n2 = branch_node('n2', skipped=leaf(), branched=None)
        n1 = branch_node('n1', skipped=n2, branched=leaf())
        self.assertEqual(candidates(n1, g), set())

    def test_guard_failure_keeps_handler_assert_reachable(self):
        g = inter_method_catch_graph().load()
        n2 = branch_node('n2', skipped=None, branched=leaf())
        n1 = branch_node('n1', skipped=n2, branched=leaf())
        self.assertEqual(candidates(n1, g), {'n2'})

    def test_unwinding_picks_the_handler_of_the_frame_on_the_stack(self):
        # test() is called twice, each time under a different handler: h1 leads to an assert, h2
        # does not. Events: g (1st call, safe), g (2nd call, fails), then the branch in h2. Its
        # unexplored side is uninteresting only if the walk unwound into h2 -- the handler of the
        # frame on top -- rather than into any handler uncaught[AIOOBE] has an edge to.
        g = (GraphBuilder()
             .nodes_('e0', 'c1', 'r1', 'c2', 'r2', 'h1', 'h1b', 'h2', 'h2b')
             .node('a', assertion=True).node('mx', exit=True)
             .node('t0').node('g').node('tx', exit=True).node('u', chain=AIOOBE_CHAIN)
             .edge('e0', 'c1')
             .call('c1', 't0', 'r1', ['tx'], handlers={'java.lang.Exception': 'h1'})
             .edge('r1', 'c2')
             .call('c2', 't0', 'r2', ['tx'], handlers={'java.lang.RuntimeException': 'h2'})
             .edge('r2', 'mx')
             .edge('h1', 'h1b').branch('h1b', 'a', 'mx').edge('a', 'mx')
             .edge('h2', 'h2b').branch('h2b', 'mx', 'mx')
             .edge('t0', 'g').branch('g', 'u', 'tx', phantom=True)
             .edge('u', 'h1', 'EXCEPTIONAL_RETURN').edge('u', 'h2', 'EXCEPTIONAL_RETURN')
             .load())
        n3 = branch_node('n3', skipped=None, branched=None)
        n2 = branch_node('n2', skipped=n3, branched=leaf())
        n1 = branch_node('n1', skipped=leaf(), branched=n2)
        self.assertEqual(candidates(n1, g), set())

    def test_recursion_levels_return_in_order(self):
        # f(): fb: FALSE -> call f() -> fr -> fx ; TRUE -> fx.  main: call f() -> r -> bm.
        # Events: fb, fb, fb(taken), bm. bm's TRUE side is the exit, so its unexplored branched
        # side must be pruned; that needs all three frames popped back to main's return site.
        g = (GraphBuilder().nodes_('e0', 'c', 'r', 'bm').node('a', assertion=True).node('x', exit=True)
             .nodes_('f0', 'fb', 'fc', 'fr').node('fx', exit=True)
             .edge('e0', 'c').call('c', 'f0', 'r', ['fx'])
             .edge('r', 'bm').branch('bm', 'a', 'x').edge('a', 'x')
             .edge('f0', 'fb').branch('fb', 'fc', 'fx')
             .call('fc', 'f0', 'fr', ['fx']).edge('fr', 'fx')
             .load())
        n4 = branch_node('n4', skipped=leaf(), branched=None)
        n3 = branch_node('n3', skipped=leaf(), branched=n4)
        n2 = branch_node('n2', skipped=n3, branched=leaf())
        n1 = branch_node('n1', skipped=n2, branched=leaf())
        self.assertEqual(candidates(n1, g), set())


def precision_graph():
    """
    SharedCalleePrecision: classify() is called from a context that returns without reaching an
    assert, and from one that asserts on its result.

        main: e0 -> b0: FALSE -> c1 -CALL-> k0, returns to r1 -> x (exit, no assert)
                        TRUE  -> c2 -CALL-> k0, returns to r2 -> bm: FALSE -> a (assert), TRUE -> x
        classify: k0 -> kb: FALSE -> kx1 (exit), TRUE -> kx2 (exit)
    """
    return (GraphBuilder()
            .nodes_('e0', 'b0', 'c1', 'r1', 'c2', 'r2', 'bm').node('a', assertion=True).node('x', exit=True)
            .node('k0').node('kb').node('kx1', exit=True).node('kx2', exit=True)
            .edge('e0', 'b0').branch('b0', 'c1', 'c2')
            .call('c1', 'k0', 'r1', ['kx1', 'kx2']).edge('r1', 'x')
            .call('c2', 'k0', 'r2', ['kx1', 'kx2']).edge('r2', 'bm')
            .branch('bm', 'a', 'x').edge('a', 'x')
            .edge('k0', 'kb').branch('kb', 'kx1', 'kx2'))


class ContextSensitiveMarkingTest(unittest.TestCase):

    def test_callee_branch_is_interesting_only_in_the_asserting_context(self):
        # Both calls to classify() have their branch half explored. Context-insensitively both
        # unexplored sides are interesting, because the second caller reaches an assert; with the
        # call stack only the one under the second call is.
        g = precision_graph().load()
        k_first = branch_node('k_first', skipped=leaf(), branched=None)
        k_second = branch_node('k_second', skipped=leaf(), branched=None)
        n0 = branch_node('n0', skipped=k_first, branched=k_second)
        self.assertEqual(candidates(n0, g), {'k_second'})

    def test_summaries(self):
        n = precision_graph().load().nodes
        self.assertFalse(n['kb'].reachesAssertSameLevel)
        self.assertTrue(n['kb'].canReturn)
        self.assertTrue(n['c2'].reachesAssertSameLevel)
        self.assertFalse(n['c1'].reachesAssertSameLevel)
        self.assertTrue(n['e0'].reachesAssertSameLevel)

    def test_assert_behind_the_return_site_is_found(self):
        # Soundness: the assert is not in classify() but after the second call returns. The walk
        # queries kb's sides with the second call on its stack, and has to look through the return.
        n = precision_graph().load().nodes
        second = sagraph.push(None, n['c2'])
        first = sagraph.push(None, n['c1'])
        self.assertTrue(sagraph.is_interesting(n['kx1'], second))
        self.assertFalse(sagraph.is_interesting(n['kx1'], first))
        # With no stack the walk is in main's frame, where classify's exit returns nowhere.
        self.assertFalse(sagraph.is_interesting(n['kx1'], None))

    def test_assert_in_callers_handler_is_found_through_unwinding(self):
        # InterMethodCatch: the guard's failure side is test()'s uncaught[AIOOBE] node, which only
        # reaches the assert in main's handler by unwinding.
        n = inter_method_catch_graph().load().nodes
        stack = sagraph.push(None, n['c'])
        self.assertTrue(sagraph.is_interesting(n['u'], stack))
        self.assertFalse(sagraph.is_interesting(n['tx'], stack))
        self.assertEqual(n['u'].escapes, frozenset({tuple(AIOOBE_CHAIN)}))
        self.assertFalse(n['u'].canReturn)  # an escaping exception is not a return
        self.assertTrue(n['c'].reachesAssertSameLevel)

    def test_exception_caught_one_frame_up_is_a_same_level_path_there(self):
        # main -> middle -> inner; inner's ArithmeticException escapes middle (whose handler is for
        # another type) and main catches it with an assert. So middle's call site into inner does
        # not reach the assert at its own level, but escapes; main's call site into middle does.
        g = (GraphBuilder()
             .nodes_('e0', 'c1', 'r1', 'h').node('a', assertion=True).node('mx', exit=True)
             .nodes_('m0', 'c2', 'hm').node('mret', exit=True)
             .nodes_('i0', 'ig').node('ix', exit=True).node('u', chain=ARITH_CHAIN)
             .edge('e0', 'c1')
             .call('c1', 'm0', 'r1', ['mret'], handlers={'java.lang.ArithmeticException': 'h'})
             .edge('r1', 'mx').edge('h', 'a').edge('a', 'mx')
             .edge('m0', 'c2')
             .call('c2', 'i0', 'mret', ['ix'], handlers={'java.lang.UnsupportedOperationException': 'hm'})
             .edge('hm', 'mret')
             .edge('i0', 'ig').branch('ig', 'u', 'ix', phantom=True)
             .edge('u', 'h', 'EXCEPTIONAL_RETURN')
             .load())
        n = g.nodes
        self.assertFalse(n['c2'].reachesAssertSameLevel)
        self.assertEqual(n['c2'].escapes, frozenset({tuple(ARITH_CHAIN)}))
        self.assertTrue(n['c1'].reachesAssertSameLevel)
        stack = sagraph.push(sagraph.push(None, n['c1']), n['c2'])
        self.assertTrue(sagraph.is_interesting(n['u'], stack))
        self.assertFalse(sagraph.is_interesting(n['ix'], stack))

    def test_recursive_summaries_converge(self):
        # f(): fb: FALSE -> call f() (returns to fr) -> fr -> fx ; TRUE -> fx.
        # main: call f() -> r -> bm: FALSE -> a (assert), TRUE -> x.
        g = (GraphBuilder().nodes_('e0', 'c', 'r', 'bm').node('a', assertion=True).node('x', exit=True)
             .nodes_('f0', 'fb', 'fc', 'fr').node('fx', exit=True)
             .edge('e0', 'c').call('c', 'f0', 'r', ['fx'])
             .edge('r', 'bm').branch('bm', 'a', 'x').edge('a', 'x')
             .edge('f0', 'fb').branch('fb', 'fc', 'fx')
             .call('fc', 'f0', 'fr', ['fx']).edge('fr', 'fx')
             .load())
        n = g.nodes
        self.assertTrue(n['fc'].canReturn)
        self.assertFalse(n['fc'].reachesAssertSameLevel)
        # Three levels deep, the assert is found by returning through every frame.
        stack = None
        for site in ['c', 'fc', 'fc']:
            stack = sagraph.push(stack, n[site])
        self.assertTrue(sagraph.is_interesting(n['fc'], stack))

    def test_incomplete_callee_makes_the_call_interesting(self):
        g = (GraphBuilder().nodes_('e0', 'b', 'c', 'r').node('x', exit=True)
             .nodes_('f0').node('fi', incomplete=True).node('fx', exit=True)
             .edge('e0', 'b').branch('b', 'c', 'x')
             .call('c', 'f0', 'r', ['fx']).edge('r', 'x')
             .edge('f0', 'fi').edge('fi', 'fx')
             .load())
        self.assertTrue(sagraph.is_interesting(g.nodes['c'], None))
        self.assertFalse(sagraph.is_interesting(g.nodes['x'], None))

    def test_legacy_graph_without_return_addresses_is_marked_context_insensitively(self):
        # An old extractor's CALL edge carries no returnSite. Its call sites are then plain
        # fall-throughs, which the summaries cannot see through, so marking falls back to
        # following predecessors -- including the RETURN edge into the asserting return site.
        g = (GraphBuilder().nodes_('e0', 'c', 'r').node('a', assertion=True).node('x', exit=True)
             .nodes_('f0', 'fb').node('fx', exit=True)
             .edge('e0', 'c').edge('c', 'f0', 'CALL').edge('fx', 'r', 'RETURN')
             .edge('r', 'a').edge('a', 'x')
             .edge('f0', 'fb').branch('fb', 'fx', 'fx')
             .load())
        self.assertTrue(sagraph.is_interesting(g.nodes['fx'], None))


if __name__ == '__main__':
    unittest.main()


class AssertDistanceTest(unittest.TestCase):

    def graph(self):
        """
        e0 -> b1: FALSE -> b2, TRUE -> c
              b2: FALSE -> x (exit), TRUE -> a2 (assert)
              c:  FALSE -> d, TRUE -> x;   d: FALSE -> a1 (assert), TRUE -> x
        """
        return (GraphBuilder().nodes_('e0', 'b1', 'b2', 'c', 'd')
                .node('a1', assertion=True).node('a2', assertion=True).node('x', exit=True)
                .edge('e0', 'b1').branch('b1', 'b2', 'c').branch('b2', 'x', 'a2')
                .branch('c', 'd', 'x').branch('d', 'a1', 'x').edge('a1', 'x').edge('a2', 'x').load())

    def test_distance_counts_branches_still_to_decide(self):
        g = self.graph()
        dist = {id: g.nodes[id].assertDistance for id in ('a1', 'a2', 'd', 'c', 'b2', 'b1', 'e0')}
        self.assertEqual(dist, {'a1': 0, 'a2': 0, 'd': 1, 'c': 2, 'b2': 1, 'b1': 2, 'e0': 2})
        self.assertEqual(g.nodes['x'].assertDistance, float('inf'))

    def test_distance_crosses_calls(self):
        g = (GraphBuilder().nodes_('e0', 'c1', 'r1', 'f0', 'fb').node('fa', assertion=True)
             .node('fx', exit=True).node('x', exit=True)
             .edge('e0', 'c1').call('c1', 'f0', 'r1', ['fx']).edge('r1', 'x')
             .edge('f0', 'fb').branch('fb', 'fx', 'fa').edge('fa', 'fx').load())
        self.assertEqual(g.nodes['e0'].assertDistance, 1)

    def test_dfs_reports_distance_of_the_unexplored_side(self):
        g = self.graph()
        # Executed: b1 FALSE, then b2 FALSE. Unexplored: b1's TRUE side (c, 2 away) and b2's TRUE
        # side (a2, the assert itself).
        b2 = branch_node('b2', skipped=leaf())
        b1 = branch_node('b1', skipped=b2)
        distances = {}
        found = dfs(set(), None, b1, set(), set(), g.entry_node, distances=distances)
        self.assertEqual([n.id for n in found], ['b1', 'b2'])
        self.assertEqual({n.id: d for n, d in distances.items()}, {'b1': 2, 'b2': 0})
        found.sort(key=lambda n: distances.get(n, float('inf')))
        self.assertEqual([n.id for n in found], ['b2', 'b1'])

    def test_dfs_without_sa_reports_no_distances(self):
        b1 = branch_node('b1', skipped=leaf())
        distances = {}
        self.assertEqual([n.id for n in dfs(set(), None, b1, set(), set(), None, distances=distances)], ['b1'])
        self.assertEqual(distances, {})
