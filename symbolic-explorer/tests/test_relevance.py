"""
Unit tests for the relevance analysis (Relevance.py) and its use in dfs (slicing).

Graphs are built as the extractor's JSON, with data-flow facts, and loaded through
SAGraph.load_json_graph(); trees are built by hand as in test_sa_walk.

Run from symbolic-explorer/:
    python3 -m unittest tests.test_relevance -v
"""

import os
import sys
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from data.BinaryExecutionTree.Leaf import Leaf
from strategy.DFS import dfs
from strategy.StrategyService import StrategyService
from tests.test_sa_walk import GraphBuilder, branch_node, keyed, leaf

ASSERTION_CHAIN = ['java.lang.AssertionError', 'java.lang.Error', 'java.lang.Throwable']
ISE_CHAIN = ['java.lang.IllegalStateException', 'java.lang.RuntimeException', 'java.lang.Exception',
             'java.lang.Throwable']


class FactGraph(GraphBuilder):
    """A GraphBuilder whose nodes carry data-flow facts and a method."""

    def __init__(self):
        super().__init__()
        self.metadata['dataflowFacts'] = 1

    def at(self, id, method='main', dataflow=(), control=(), terminates=False, call=None, **kw):
        self.node(id, **kw)
        n = self.nodes[-1]
        n['metadata'] = f'method: {method}'
        if dataflow:
            n['dataflow'] = [[list(w), list(r)] for w, r in dataflow]
        if control:
            n['control'] = list(control)
        if terminates:
            n['terminates'] = True
        if call is not None:
            n['call'] = call
        return self


def example(body=None, extra=None):
    """
    The design example:

        val = nondet();
        for (i = 0; i < 100; i++) { if (i < val) print(i); }   // h, b, p
        assert val != C;                                       // a, ae

    `body` replaces the facts of the print node p, `extra(builder)` adds more nodes and edges.
    """
    g = FactGraph()
    g.at('v', dataflow=[(['l:val', 'g:input'], ['g:input'])])
    g.at('i0', dataflow=[(['l:i'], [])])
    g.at('h', control=['l:i'])
    g.at('b', control=['l:i', 'l:val'])
    g.at('p', **(body or {'dataflow': [(['g:out'], ['l:i'])]}))
    g.at('inc', dataflow=[(['l:i'], ['l:i'])])
    g.at('a', control=['l:val'])
    g.at('ae', assertion=True, dataflow=[(['l:e'], [])])
    g.at('thr', dataflow=[(['g:exc'], ['l:e'])])
    g.at('unc', chain=ASSERTION_CHAIN)
    g.at('ret', exit=True)
    g.edge('v', 'i0').edge('i0', 'h')
    g.branch('h', 'b', 'a')            # i >= 100: leave the loop
    g.branch('b', 'p', 'inc')          # i >= val: skip the print
    g.edge('p', 'inc').edge('inc', 'h')
    g.branch('a', 'ae', 'ret')         # val != C: no assertion error
    g.edge('ae', 'thr').edge('thr', 'unc', 'THROW')
    if extra is not None:
        extra(g)
    return g


def irrelevant(g):
    return {n.id for n in g.nodes.values() if n.is_branch() and n.irrelevant}


class RelevanceTest(unittest.TestCase):

    def test_the_loop_of_the_example_is_irrelevant(self):
        g = example().load()
        self.assertEqual(irrelevant(g), {'h', 'b'})
        self.assertEqual({n.id for n in g.nodes['b'].region}, {'p'})
        self.assertIn('main|l:val', _rel(g))
        self.assertNotIn('main|l:i', _rel(g))

    def test_writing_what_the_assert_reads_is_relevant(self):
        g = example({'dataflow': [(['l:val'], ['l:i'])]}).load()
        self.assertEqual(irrelevant(g), set())

    def test_reading_input_is_relevant(self):
        # val is an input, so the input cursor is relevant: reading another input in the region
        # shifts which input val gets.
        g = example({'dataflow': [(['l:x', 'g:input'], ['g:input'])]}).load()
        self.assertEqual(irrelevant(g), set())

    def test_ending_the_program_is_relevant(self):
        g = example({'terminates': True}).load()
        self.assertEqual(irrelevant(g), set())

    def test_an_irrelevant_write_through_a_relevant_branch_is_relevant(self):
        # The region writes x, which decides whether the assert is reached.
        def extra(g):
            g.at('gx', control=['l:x'])
            g.edges = [e for e in g.edges if not (e['source'] == 'h' and e['target'] == 'a')]
            g.edge('h', 'gx', 'TRUE_BRANCH')
            g.branch('gx', 'a', 'ret')
        g = example({'dataflow': [(['l:x'], ['l:i'])]}, extra).load()
        self.assertEqual(irrelevant(g), set())

    def test_a_callee_writing_a_relevant_field_is_relevant(self):
        # p calls m(), which writes f; the assert's guard reads f.
        def extra(g):
            g.at('me', method='m', dataflow=[(['f:f:int'], [])])
            g.at('mx', method='m', exit=True)
            g.edge('me', 'mx')
            g.at('rs', dataflow=[])
            g.edges = [e for e in g.edges if not (e['source'] == 'p')]
            g.call('p', 'me', 'rs', callee_exits=['mx'])
            g.edge('rs', 'inc')
            g.at('rf', dataflow=[(['l:val'], ['f:f:int'])])
            g.edges = [e for e in g.edges if not (e['source'] == 'v')]
            g.edge('v', 'rf').edge('rf', 'i0')
        g = example({'call': {'args': [], 'base': []}}, extra).load()
        self.assertEqual(irrelevant(g), set())

    def test_a_callee_writing_an_unread_field_is_irrelevant(self):
        def extra(g):
            g.at('me', method='m', dataflow=[(['f:log:int'], ['f:log:int'])])
            g.at('mx', method='m', exit=True)
            g.edge('me', 'mx')
            g.at('rs')
            g.edges = [e for e in g.edges if not (e['source'] == 'p')]
            g.call('p', 'me', 'rs', callee_exits=['mx'])
            g.edge('rs', 'inc')
        g = example({'call': {'args': [['l:i']], 'base': []}}, extra).load()
        self.assertEqual(irrelevant(g), {'h', 'b'})

    def test_an_exception_a_caller_catches_and_survives_is_relevant(self):
        # p calls m(), which may throw an exception that main catches and then goes on.
        def extra(g):
            g.at('me', method='m', control=['p:0'])
            g.at('mt', method='m')
            g.at('mu', method='m', chain=ISE_CHAIN)
            g.at('mx', method='m', exit=True)
            g.branch('me', 'mx', 'mt')
            g.edge('mt', 'mu', 'THROW')
            g.at('rs')
            g.at('hd', dataflow=[(['l:c'], ['g:exc'])])
            g.edges = [e for e in g.edges if not (e['source'] == 'p')]
            g.call('p', 'me', 'rs', callee_exits=['mx'], handlers={'java.lang.IllegalStateException': 'hd'})
            g.edge('rs', 'inc').edge('hd', 'a')
            g.edge('mu', 'hd', 'EXCEPTIONAL_RETURN')
        g = example({'call': {'args': [['l:i']], 'base': []}}, extra).load()
        self.assertEqual(irrelevant(g) & {'h', 'b'}, set())

    def test_an_exception_nobody_catches_is_irrelevant(self):
        # As above, but nothing catches it: the program ends with an uncaught exception.
        def extra(g):
            g.at('me', method='m', control=['p:0'])
            g.at('mt', method='m')
            g.at('mu', method='m', chain=ISE_CHAIN)
            g.at('mx', method='m', exit=True)
            g.branch('me', 'mx', 'mt')
            g.edge('mt', 'mu', 'THROW')
            g.at('rs')
            g.edges = [e for e in g.edges if not (e['source'] == 'p')]
            g.call('p', 'me', 'rs', callee_exits=['mx'])
            g.edge('rs', 'inc')
        g = example({'call': {'args': [['l:i']], 'base': []}}, extra).load()
        self.assertEqual(irrelevant(g), {'h', 'b', 'me'})

    def test_what_decides_a_caught_exception_is_relevant(self):
        # sa-soundness/npe-catch-local: box = b == 7 ? null : new Box(); try { r = box.v; }
        # catch (NullPointerException e) { assert ... }. The dereference is an implicit branch.
        g = FactGraph()
        g.at('in', dataflow=[(['l:b', 'g:input'], ['g:input'])])
        g.at('t', control=['l:b'])
        g.at('nul', dataflow=[(['l:box'], [])])
        g.at('new', dataflow=[(['l:box'], [])])
        g.at('deref', dataflow=[(['l:r'], ['l:box', 'f:v:int'])])
        g.at('hd', dataflow=[(['l:e'], ['g:exc'])])
        g.at('ae', assertion=True)
        g.at('ret', exit=True)
        g.edge('in', 't')
        g.branch('t', 'new', 'nul')
        g.edge('nul', 'deref').edge('new', 'deref')
        g.edge('deref', 'ret').edge('deref', 'hd', 'EXCEPTION')
        g.edge('hd', 'ae').edge('ae', 'ret')
        self.assertEqual(irrelevant(g.load()), set())

    def test_without_facts_nothing_is_irrelevant(self):
        g = example()
        g.metadata.pop('dataflowFacts')
        self.assertEqual(irrelevant(g.load()), set())


def _rel(g):
    """The relevant locations of `g`."""
    from data.StaticAnalysisGraph.Relevance import Relevance
    r = Relevance(g.nodes.values())
    r.compute()
    return r.relevant_locations


class Tree:
    """What dfs keeps on the tree."""
    def __init__(self):
        self.sig_ids = {}

    def record_execution_error(self, kind, message):
        self.error = (kind, message)

    def dominated_by_loop_state(self, node):
        return False


def link(*nodes):
    """Sets the parents of a hand-built path or tree."""
    for n in nodes:
        for c in (n.skipped, n.branched):
            if c is not None:
                c.parent = n


class SlicingTest(unittest.TestCase):

    def setUp(self):
        self.g = keyed(example().load())

    def iteration(self, took_print, rest, name):
        """h (stay in the loop), then b, printing or not, then `rest` (the next tree node)."""
        if took_print:
            b = branch_node(f'b{name}', skipped=rest, key='b')
        else:
            b = branch_node(f'b{name}', branched=rest, key='b')
        h = branch_node(f'h{name}', skipped=b, key='h')
        b.constraint = {b.trace_id: f'(declare-fun val () Int) (assert (< i{name} val))'}
        link(h, b)
        return h

    def assert_tail(self, name, end):
        a = branch_node(f'a{name}', branched=end, key='a')  # val != C: the assert not reached
        a.constraint = {a.trace_id: '(declare-fun val () Int) (assert (distinct val 3))'}
        h = branch_node(f'hx{name}', branched=a, key='h')
        link(h, a)
        return h, a

    def test_the_loop_is_not_flipped_and_its_constraints_are_left_out(self):
        tail, a = self.assert_tail('', leaf())
        root = self.iteration(False, self.iteration(True, tail, '1'), '0')
        tree = Tree()
        found = dfs(set(), tree, root, set(), set(), self.g.entry_node)
        self.assertEqual([n.id for n in found], ['a'])
        # One trace took the whole path: only the b nodes have symbolic constraints.
        n = a
        while n is not None:
            n.constraint = {'T': c for c in n.constraint.values()}
            n.trace_id = 'T'
            n = n.parent
        dropped = []
        self.assertEqual(StrategyService.collect_path_constrains(a, dropped), [])
        self.assertEqual({d.id for d in dropped}, {'b0', 'b1'})

    def test_paths_that_differ_only_in_irrelevant_decisions_are_one(self):
        # Two paths: print in iteration 0 or not, then leave; each reaches the assert unflipped.
        tail1, a1 = self.assert_tail('1', leaf())
        tail2, a2 = self.assert_tail('2', leaf())
        b = branch_node('b0', skipped=tail1, branched=tail2, key='b')
        root = branch_node('h0', skipped=b, key='h')
        link(root, b)
        found = dfs(set(), Tree(), root, set(), set(), self.g.entry_node)
        self.assertEqual([n.id for n in found], ['a1'])
        # Once one of them was flipped, the other is covered.
        a1.skipped = leaf()
        found = dfs(set(), Tree(), root, set(), set(), self.g.entry_node)
        self.assertEqual(found, [])

    def test_a_branch_the_graph_has_no_node_for_poisons_the_region(self):
        # A callback runs in the print and branches: the region of b is not what the graph says.
        tail, a = self.assert_tail('', leaf())
        cb = branch_node('cb', skipped=tail, key=None, callback=True)
        b = branch_node('b0', skipped=cb, key='b')
        root = branch_node('h0', skipped=b, key='h')
        link(root, b, cb)
        tree = Tree()
        found = dfs(set(), tree, root, set(), set(), self.g.entry_node)
        self.assertTrue(getattr(b, 'poisoned', False))
        self.assertTrue(tree.poisoned_in_walk)
        found = dfs(set(), tree, root, set(), set(), self.g.entry_node)  # as select_branch does
        self.assertEqual({n.id for n in found}, {'h0', 'b0', 'a', 'cb'})
        self.assertFalse(b.sliced)

    def test_an_abnormal_end_in_the_region_poisons_it(self):
        end = leaf()
        end.abnormal = True
        b = branch_node('b0', skipped=end, key='b')
        root = branch_node('h0', skipped=b, key='h')
        link(root, b)
        b.abnormal_below = root.abnormal_below = True
        tree = Tree()
        dfs(set(), tree, root, set(), set(), self.g.entry_node)
        self.assertTrue(getattr(b, 'poisoned', False))
        self.assertTrue(getattr(root, 'poisoned', False))

    def test_a_closed_region_is_not_poisoned(self):
        # The callback comes after the loop, outside every region.
        cb = branch_node('cb', skipped=leaf(), key=None, callback=True)
        a = branch_node('a', branched=cb, key='a')
        hx = branch_node('hx', branched=a, key='h')
        b = branch_node('b0', skipped=hx, key='b')
        root = branch_node('h0', skipped=b, key='h')
        link(root, b, hx, a, cb)
        tree = Tree()
        dfs(set(), tree, root, set(), set(), self.g.entry_node)
        self.assertFalse(getattr(b, 'poisoned', False))
        self.assertFalse(getattr(root, 'poisoned', False))


if __name__ == '__main__':
    unittest.main()
