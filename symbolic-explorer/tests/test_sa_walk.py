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
        self.metadata = {} # further graph metadata, e.g. callbacksReachingAssert

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
                              'isIncomplete': any(n.get('isIncomplete') for n in self.nodes),
                              **self.metadata}}
        with tempfile.NamedTemporaryFile('w', suffix='.json', delete=False) as f:
            json.dump(graph, f)
            path = f.name
        try:
            g = SAGraph()
            g.load_json_graph(path)
            return g
        finally:
            os.unlink(path)

    def save(self, path: str):
        """Writes the graph's JSON to `path`, as the extractor would."""
        graph = {'nodes': self.nodes, 'edges': self.edges, 'entryNodeId': self.entry,
                 'exitNodeIds': self.exits,
                 'metadata': {'assertionPointIds': self.asserts, **self.metadata}}
        with open(path, 'w') as f:
            json.dump(graph, f)


def branch_node(name, skipped=None, branched=None, key=None, pops=0, pushes=(), callback=False, choice=False):
    """A tree node for one branch event; `None` children are unexplored sides."""
    n = Node.__new__(Node)
    n.parent = None
    n.id = name
    n.trace_id = name
    n.gid = name
    n.kind = 'Branch'
    n.concrete_only = False
    n.inst = None
    n.key = key
    n.pops = pops
    n.pushes = pushes
    n.callback = callback
    n.choice = choice
    n.constraint = {}
    n.skipped = skipped
    n.branched = branched
    return n


def special_node(name, branched=None, inst=None):
    """A tree node for a special (non-branch) event; dfs passes through to `branched`."""
    n = branch_node(name, branched=branched)
    n.kind = 'Special'
    n.inst = inst
    return n


def leaf():
    lf = Leaf.__new__(Leaf)
    lf.parent = None
    lf.inputs = []
    return lf


def keyed(g: SAGraph) -> SAGraph:
    """Gives every branch and call site of `g` its node id as key, so a tree can name them."""
    index = g.entry_node.graph_index
    for n in g.nodes.values():
        if n.is_branch() or n.is_call_site():
            n.key = n.id
            index.keys[n.id] = n
    return g


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
        g = keyed(shared_callee_graph().load())
        n3 = branch_node('n3', skipped=leaf(), branched=None, key='bm', pops=1)
        n2 = branch_node('n2', skipped=n3, branched=leaf(), key='kb', pops=1, pushes=('c2',))
        n1 = branch_node('n1', skipped=n2, branched=leaf(), key='kb', pushes=('c1',))
        self.assertEqual(candidates(n1, g), set())

    def test_unchecked_pairing_is_not_pruned(self):
        # As above, but the trace carries no keys: the walk still pairs bm's event with bm, but
        # cannot check that pairing, so it does not prune by it.
        g = shared_callee_graph().load()
        n3 = branch_node('n3', skipped=leaf(), branched=None)
        n2 = branch_node('n2', skipped=n3, branched=leaf())
        n1 = branch_node('n1', skipped=n2, branched=leaf())
        self.assertEqual(candidates(n1, g), {'n3'})

    def test_unchecked_stack_is_not_pruned(self):
        # The keys match, but a frame the trace reports has no call site in the graph (a library
        # call, or a call site without a key), so the walk's stack cannot be checked.
        # Under the first call to classify(), kb's sides cannot reach an assert (precision_graph).
        # (b0 has no key, so its pruned side is still walked into.)
        g = keyed(precision_graph().load())
        k_first = branch_node('k_first', skipped=leaf(), branched=None, key='kb', pushes=('c1',))
        n0 = branch_node('n0', skipped=k_first, branched=leaf())
        self.assertEqual(candidates(n0, g), set())
        k_first.pushes = (None,)
        self.assertEqual(candidates(n0, g), {'k_first'})

    def test_unexplored_side_leading_to_assert_is_kept(self):
        g = keyed(shared_callee_graph().load())
        n3 = branch_node('n3', skipped=None, branched=leaf(), key='bm', pops=1)
        n2 = branch_node('n2', skipped=n3, branched=leaf(), key='kb', pops=1, pushes=('c2',))
        n1 = branch_node('n1', skipped=n2, branched=leaf(), key='kb', pushes=('c1',))
        self.assertEqual(candidates(n1, g), {'n3'})

    def test_unmodelled_exception_makes_everything_after_it_interesting(self):
        # main: if (b1) exit; if (b2) exit; assert. b2's TRUE side cannot reach the assert, so it is
        # pruned -- unless the executor reported an exception the graph does not model before b2:
        # from there on the walk has no information.
        g = keyed(GraphBuilder()
             .nodes_('e0', 'b1', 'b2').node('a', assertion=True).node('x', exit=True)
             .edge('e0', 'b1').branch('b1', 'b2', 'x').branch('b2', 'a', 'x').edge('a', 'x')
             .load())
        n2 = branch_node('n2', skipped=leaf(), branched=None, key='b2')
        n1 = branch_node('n1', skipped=n2, branched=leaf(), key='b1')
        self.assertEqual(candidates(n1, g), set())
        n1.skipped = special_node('x', branched=n2, inst='UNMODELLED_EXCEPTION')
        n2.key = None # without a key the walk cannot find the graph again (see KeyCheckTest)
        self.assertEqual(candidates(n1, g), {'n2'})

    def exception_graph(self):
        """
        main: if (b1) exit; c = f(); [handler h: if (b2) exit; assert];  f: if (bf) ...; return
        h is the handler of main's call to f; b2's TRUE side cannot reach the assert.
        """
        g = (GraphBuilder()
             .nodes_('e0', 'b1', 'c1', 'r1', 'h', 'b2').node('a', assertion=True).node('x', exit=True)
             .nodes_('f0', 'bf', 'fd').node('fx', exit=True).node('fu', chain=['?Exception'])
             .edge('e0', 'b1').branch('b1', 'c1', 'x')
             .call('c1', 'f0', 'r1', ['fx'], handlers={'java.lang.Throwable': 'h'})
             .edge('r1', 'x').edge('h', 'b2').branch('b2', 'a', 'x').edge('a', 'x')
             .edge('f0', 'bf').branch('bf', 'fd', 'fx').edge('fd', 'fx').edge('fd', 'fu', 'EXCEPTION'))
        next(e for e in g.edges if e['type'] == 'CALL')['unknownHandlers'] = {'?Exception': ['h']}
        for n in g.nodes:
            n['metadata'] = 'method: ' + ('f' if n['id'].startswith('f') or n['id'] == 'bf' else 'main')
        return g.load()

    def tree_through_exception(self, events, after_branch_in_f=True):
        """b1 (skipped), [bf in f], then `events` (special nodes), then b2 with its TRUE side unexplored."""
        n2 = branch_node('n2', skipped=leaf(), branched=None)
        nxt = n2
        for name, inst in reversed(events):
            nxt = special_node(name, branched=nxt, inst=inst)
        if after_branch_in_f:
            nxt = branch_node('nf', skipped=nxt, branched=leaf())
        return branch_node('n1', skipped=nxt, branched=leaf())

    def test_after_an_unmodelled_exception_the_walk_gives_up(self):
        g = self.exception_graph()
        # Without keys the walk cannot find the graph again (see KeyCheckTest for resync).
        tree = self.tree_through_exception([('u', 'UNMODELLED_EXCEPTION')])
        self.assertEqual(candidates(tree, g), {'n2'})

    def test_guard_failure_is_followed_into_callers_handler(self):
        # Events: the guard in test() fails (skipped), then the branch in main's handler. Its TRUE
        # side skips the assert, so the unexplored branched side is uninteresting -- which the walk
        # can only tell if it unwound from uncaught[AIOOBE] into main's handler. Stopping at the
        # uncaught node means "no information" and keeps it.
        g = keyed(inter_method_catch_graph().load())
        n2 = branch_node('n2', skipped=leaf(), branched=None, key='hb', pops=1)
        n1 = branch_node('n1', skipped=n2, branched=leaf(), key='g', pushes=('c',))
        self.assertEqual(candidates(n1, g), set())

    def test_guard_failure_keeps_handler_assert_reachable(self):
        g = keyed(inter_method_catch_graph().load())
        n2 = branch_node('n2', skipped=None, branched=leaf(), key='hb', pops=1)
        n1 = branch_node('n1', skipped=n2, branched=leaf(), key='g', pushes=('c',))
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
        keyed(g)
        n3 = branch_node('n3', skipped=None, branched=None, key='h2b', pops=1)
        n2 = branch_node('n2', skipped=n3, branched=leaf(), key='g', pops=1, pushes=('c2',))
        n1 = branch_node('n1', skipped=leaf(), branched=n2, key='g', pushes=('c1',))
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
        keyed(g)
        n4 = branch_node('n4', skipped=leaf(), branched=None, key='bm', pops=3)
        n3 = branch_node('n3', skipped=leaf(), branched=n4, key='fb', pushes=('fc',))
        n2 = branch_node('n2', skipped=n3, branched=leaf(), key='fb', pushes=('fc',))
        n1 = branch_node('n1', skipped=n2, branched=leaf(), key='fb', pushes=('c',))
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



class FakeTree:
    """Collects what dfs records (record_execution_error)."""
    def __init__(self):
        self.execution_errors = []

    def record_execution_error(self, kind, message):
        self.execution_errors.append({'kind': kind, 'message': message})


class KeyCheckTest(unittest.TestCase):
    """Branch keys and stack deltas: dfs checks its walk against the trace, and finds the graph again."""

    B1, CALL, BF, B2 = 'M|1|if|0', 'M|2|call:f()I|0', 'F|10|if|0', 'M|3|if|0'

    def graph(self):
        """As DfsTest.exception_graph, with keys on the branches and the call site."""
        g = DfsTest.exception_graph(self)
        g.nodes['b1'].key, g.nodes['c1'].key, g.nodes['bf'].key, g.nodes['b2'].key = \
            self.B1, self.CALL, self.BF, self.B2
        index = g.entry_node.graph_index
        index.keys.update({n.key: n for n in g.nodes.values() if n.key is not None})
        return g

    THROWS = (('u', 'UNMODELLED_EXCEPTION'),)

    def tree(self, b2_key=None, b2_pops=1, bf_key=None, bf_pushes=None, events=THROWS):
        """
        n1 (b1, skipped), nf (bf in f, skipped), `events`, n2 (b2, TRUE side unexplored). b2 is
        only reached through main's handler: by default f throws an exception the graph does not
        model, so the walk is lost after nf and can only find b2 by resync.
        """
        n2 = branch_node('n2', skipped=leaf(), branched=None, key=b2_key or self.B2, pops=b2_pops)
        nxt = n2
        for name, inst in reversed(events):
            nxt = special_node(name, branched=nxt, inst=inst)
        nf = branch_node('nf', skipped=nxt, branched=leaf(), key=bf_key or self.BF,
                         pushes=bf_pushes if bf_pushes is not None else (self.CALL,))
        return branch_node('n1', skipped=nf, branched=leaf(), key=self.B1)

    def run_dfs(self, tree, g):
        fake = FakeTree()
        found = {n.id for n in dfs(set(), fake, tree, set(), set(), g.entry_node)}
        return found, [e for e in fake.execution_errors if e['kind'] == 'sa_desync']

    def test_walk_in_step_is_not_flagged(self):
        g = self.graph()
        _, desyncs = self.run_dfs(self.tree(), g)
        self.assertEqual(desyncs, [])

    def test_wrong_key_is_a_desync(self):
        g = self.graph()
        _, desyncs = self.run_dfs(self.tree(bf_key='F|10|if|1'), g)
        self.assertEqual(len(desyncs), 1)

    def test_wrong_stack_is_a_desync(self):
        g = self.graph()
        # The trace says bf runs two frames up, the walk entered one.
        found, desyncs = self.run_dfs(self.tree(bf_pushes=(self.CALL, self.CALL)), g)
        self.assertEqual(len(desyncs), 1)

    def test_missing_keys_are_not_checked(self):
        g = self.graph()
        g.nodes['bf'].key = None
        _, desyncs = self.run_dfs(self.tree(bf_key='F|10|if|1'), g)
        self.assertEqual(desyncs, [])

    def test_resync_after_an_unmodelled_exception(self):
        g = self.graph()
        # The walk is lost after the exception and finds b2 again by its key, with the stack from
        # the trace (f's frame popped): b2's TRUE side cannot reach the assert.
        found, desyncs = self.run_dfs(self.tree(), g)
        self.assertEqual(found, set())
        self.assertEqual(desyncs, [])

    def test_no_resync_without_a_key(self):
        g = self.graph()
        del g.entry_node.graph_index.keys[self.B2]
        found, _ = self.run_dfs(self.tree(), g)
        self.assertEqual(found, {'n2'})

    def test_no_resync_when_the_stack_does_not_lead_to_the_branch(self):
        g = self.graph()
        # The trace still has f's frame (no pop), but b2 is in main: the stack and the key disagree.
        found, _ = self.run_dfs(self.tree(b2_pops=0), g)
        self.assertEqual(found, {'n2'})

    def test_no_resync_through_an_unknown_call_site(self):
        g = self.graph()
        found, _ = self.run_dfs(self.tree(b2_pops=0, bf_pushes=(None,)), g)
        self.assertEqual(found, {'n2'})

    def test_callback_branches_are_masked(self):
        g = self.graph()
        plain, _ = self.run_dfs(self.tree(), g)
        # Before f is called, library code called back into application code: two branches the
        # graph does not have, in a frame pushed by a library call. Unmasked, they would be a desync.
        tree = self.tree()
        nf = tree.skipped
        cb2 = branch_node('cb2', skipped=nf, branched=leaf(), key='App.cb()V|7|if|1', callback=True)
        cb1 = branch_node('cb1', skipped=cb2, branched=leaf(), key='App.cb()V|7|if|0',
                          pushes=('M|1|call:read()I|0',), callback=True)
        tree.skipped = cb1
        nf.pops = 1
        found, desyncs = self.run_dfs(tree, g)
        self.assertEqual(desyncs, [])
        self.assertEqual(found, plain)  # b2 is still pruned after the callback
        for n in (cb1, cb2):
            n.callback = False
        _, desyncs = self.run_dfs(tree, g)
        self.assertEqual(len(desyncs), 1)

    def test_choice_branches_are_masked(self):
        g = self.graph()
        plain, _ = self.run_dfs(self.tree(), g)
        # Before f is called, an access at a symbolic index adds two choice branches the graph does
        # not have, one with an unexplored side. Masked, the walk holds its place over them, and
        # they are still solver targets.
        tree = self.tree()
        nf = tree.skipped
        ch2 = branch_node('ch2', skipped=None, branched=nf, key='M|1|aaload|0#idx1', choice=True)
        ch1 = branch_node('ch1', skipped=ch2, branched=leaf(), key='M|1|aaload|0#idx0', choice=True)
        tree.skipped = ch1
        found, desyncs = self.run_dfs(tree, g)
        self.assertEqual(desyncs, [])
        self.assertEqual(found, plain | {'ch2'})
        for n in (ch1, ch2):
            n.choice = False
        _, desyncs = self.run_dfs(tree, g)
        self.assertEqual(len(desyncs), 1)

    def test_open_ordinal_matches_any_on_its_line(self):
        g = self.graph()
        g.nodes['bf'].key = 'F|10|if|*'
        _, desyncs = self.run_dfs(self.tree(bf_key='F|10|if|3'), g)
        self.assertEqual(desyncs, [])
        _, desyncs = self.run_dfs(self.tree(bf_key='F|11|if|0'), g)
        self.assertEqual(len(desyncs), 1)


class MultiCalleeTest(unittest.TestCase):
    """A call site with several known callees: summaries take the union, the walk resyncs inside."""

    CALL, BF, BG, B2 = 'M|2|call:run()V|0', 'F|10|if|0', 'G|20|if|0', 'M|3|if|0'

    def graph(self):
        """
        main: c1 = x.run() -- F.run or G.run; then if (b2) ...;  F.run: if (bf) assert;
        G.run: if (bg) ...  (no assert). Keys on the branches and the call site.
        """
        g = (GraphBuilder()
             .nodes_('e0', 'c1', 'r1', 'b2', 'x2').node('mx', exit=True)
             .nodes_('f0', 'bf', 'fd').node('fa', assertion=True).node('fx', exit=True)
             .nodes_('g0', 'bg', 'gd').node('gx', exit=True)
             .edge('e0', 'c1')
             .call('c1', 'f0', 'r1', ['fx']).call('c1', 'g0', 'r1', ['gx'])
             .edge('r1', 'b2').branch('b2', 'mx', 'x2').edge('x2', 'mx')
             .edge('f0', 'bf').branch('bf', 'fd', 'fa').edge('fa', 'fx').edge('fd', 'fx')
             .edge('g0', 'bg').branch('bg', 'gd', 'gx').edge('gd', 'gx'))
        for n in g.nodes:
            n['metadata'] = 'method: ' + ('F' if n['id'] in ('f0', 'bf', 'fd', 'fa', 'fx')
                                          else 'G' if n['id'] in ('g0', 'bg', 'gd', 'gx') else 'main')
            n['key'] = {'c1': self.CALL, 'bf': self.BF, 'bg': self.BG, 'b2': self.B2}.get(n['id'])
            if n['key'] is None:
                del n['key']
        return g.load()

    def run_dfs(self, tree, g):
        fake = FakeTree()
        found = {n.id for n in dfs(set(), fake, tree, set(), set(), g.entry_node)}
        return found, [e for e in fake.execution_errors if e['kind'] == 'sa_desync']

    def test_summaries_take_the_union(self):
        g = self.graph()
        self.assertTrue(g.nodes['c1'].reachesAssertSameLevel)  # through F.run
        self.assertTrue(g.nodes['e0'].reachesAssertSameLevel)
        self.assertFalse(g.nodes['r1'].reachesAssertSameLevel)

    def test_walk_stops_at_the_call_site(self):
        g = self.graph()
        self.assertIsNone(g.nodes['c1'].walk_till_branch(None))

    def test_resync_in_the_callee_that_ran(self):
        g = self.graph()
        # G.run ran: its branch bg is found by key, under the call site c1. Neither side of bg
        # reaches an assert, so its unexplored side is pruned.
        ng = branch_node('ng', skipped=leaf(), branched=None, key=self.BG, pushes=(self.CALL,))
        found, desyncs = self.run_dfs(ng, g)
        self.assertEqual(found, set())
        self.assertEqual(desyncs, [])
        # F.run ran: bf's TRUE side reaches the assert.
        nf = branch_node('nf', skipped=leaf(), branched=None, key=self.BF, pushes=(self.CALL,))
        found, _ = self.run_dfs(nf, g)
        self.assertEqual(found, {'nf'})

    def test_no_resync_into_a_method_the_call_site_cannot_call(self):
        g = self.graph()
        g.nodes['c1'].callees = [g.nodes['f0'], g.nodes['fx']]  # G.run is not a callee any more
        ng = branch_node('ng', skipped=leaf(), branched=None, key=self.BG, pushes=(self.CALL,))
        found, _ = self.run_dfs(ng, g)
        self.assertEqual(found, {'ng'})  # no information

class ContextSensitiveMarkingTest(unittest.TestCase):

    def test_callee_branch_is_interesting_only_in_the_asserting_context(self):
        # Both calls to classify() have their branch half explored. Context-insensitively both
        # unexplored sides are interesting, because the second caller reaches an assert; with the
        # call stack only the one under the second call is.
        g = keyed(precision_graph().load())
        k_first = branch_node('k_first', skipped=leaf(), branched=None, key='kb', pushes=('c1',))
        k_second = branch_node('k_second', skipped=leaf(), branched=None, key='kb', pushes=('c2',))
        n0 = branch_node('n0', skipped=k_first, branched=k_second, key='b0')
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

    def unknown_exception_graph(self, unknown_handlers, middle=False):
        """
        main calls f (through middle() if `middle`) inside a try whose handler h asserts.
        f: if (..) { deref (may throw an exception of unknown type) } -- like npe-catch-up1.
        """
        b = (GraphBuilder()
             .nodes_('e0', 'c1', 'r1', 'h').node('a', assertion=True).node('mx', exit=True)
             .nodes_('k0', 'kb', 'kd').node('kx', exit=True).node('ku', chain=['?Exception']))
        callee = 'k0'
        if middle:
            b.nodes_('m0', 'c2').node('mret', exit=True).node('mu', chain=['?Exception'])
            callee = 'm0'
        b.edge('e0', 'c1')
        b.call('c1', callee, 'r1', ['mret'] if middle else ['kx'],
               handlers={'java.lang.NullPointerException': 'h'})
        next(e for e in b.edges if e['source'] == 'c1' and e['type'] == 'CALL')['unknownHandlers'] = unknown_handlers
        b.edge('r1', 'mx').edge('h', 'a').edge('a', 'mx')
        if middle:
            b.edge('m0', 'c2').call('c2', 'k0', 'mret', ['kx'])
        b.edge('k0', 'kb').branch('kb', 'kd', 'kx').edge('kd', 'kx').edge('kd', 'ku', 'EXCEPTION')
        return b.load()

    def test_unknown_exception_reaches_a_listed_handler_in_the_caller(self):
        n = self.unknown_exception_graph({'?Exception': ['h']}).nodes
        stack = sagraph.push(None, n['c1'])
        self.assertTrue(sagraph.is_interesting(n['kd'], stack))   # may throw into main's handler
        self.assertFalse(sagraph.is_interesting(n['kx'], stack))  # cannot
        self.assertEqual(n['kd'].escapes, frozenset({('?Exception',)}))
        self.assertTrue(n['c1'].reachesAssertSameLevel)

    def test_unknown_exception_is_not_caught_by_an_unlisted_handler(self):
        n = self.unknown_exception_graph({}).nodes
        self.assertFalse(sagraph.is_interesting(n['kd'], sagraph.push(None, n['c1'])))
        self.assertFalse(n['c1'].reachesAssertSameLevel)

    def test_unknown_exception_unwinds_through_frames_without_handlers(self):
        n = self.unknown_exception_graph({'?Exception': ['h']}, middle=True).nodes
        stack = sagraph.push(sagraph.push(None, n['c1']), n['c2'])
        self.assertTrue(sagraph.is_interesting(n['kd'], stack))
        self.assertFalse(sagraph.is_interesting(n['kx'], stack))
        self.assertIn(('?Exception',), n['c2'].escapes)  # middle has no handler: it escapes
        self.assertTrue(n['c1'].reachesAssertSameLevel)

    def test_unknown_exception_also_escapes_past_a_listed_handler(self):
        # A listed handler might not catch it, so the escape stays in the call site's summary.
        n = self.unknown_exception_graph({'?Exception': ['h']}).nodes
        self.assertIn(('?Exception',), n['c1'].escapes)

    def test_walk_gives_up_at_an_unknown_exception_escape(self):
        n = self.unknown_exception_graph({'?Exception': ['h']}).nodes
        self.assertIsNone(n['ku'].walk_till_branch(sagraph.push(None, n['c1'])))

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


class ConcreteOnlyBranchTest(unittest.TestCase):

    def test_concrete_only_guard_keeps_the_walk_in_step_but_is_no_candidate(self):
        # e0 -> g: a phantom bounds guard (in bounds: b, out of bounds: u, which leaves main)
        #       b: FALSE -> x (exit), TRUE -> a (assert)
        g = (GraphBuilder().nodes_('e0', 'g', 'b').node('u', chain=AIOOBE_CHAIN)
             .node('a', assertion=True).node('x', exit=True)
             .edge('e0', 'g').branch('g', 'u', 'b', phantom=True).branch('b', 'x', 'a').edge('a', 'x').load())
        # The executor records the guard of an untracked array as a concrete-only branch: in bounds,
        # so taken. Without it, b would be paired with g and its FALSE side (u) pruned.
        b = branch_node('b', skipped=leaf())
        guard = branch_node('g', branched=b)
        guard.concrete_only = True
        self.assertEqual(candidates(guard, g), {'b'})
