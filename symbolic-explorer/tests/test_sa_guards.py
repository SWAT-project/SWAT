"""
Unit tests for when the static pre-analysis may prune at all: only under valid-assert, and only for
programs that run no application code outside main's call tree (other threads, shutdown hooks,
uncaught-exception handlers).

Run from symbolic-explorer/:
    python3 -m unittest tests.test_sa_guards -v
"""

import os
import sys
import tempfile
import unittest
from types import SimpleNamespace

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from data.StaticAnalysisGraph.SAGraph import is_interesting
from svcomp.StaticPreAnalysis import StaticPreAnalysis, SAStatus
from tests.test_sa_walk import GraphBuilder

HOOK = "virtualinvoke $r1.<java.lang.Runtime: void addShutdownHook(java.lang.Thread)>($r2)"


def hook_graph(hook_statement=HOOK, expanded=False, callbacks=True):
    """
    main: hk (a library call); if (x > 5) flag = 1; return  -- the shutdown hook asserts flag != 1.
    The extractor marks the library call an assertion point, as a callback reaches an assert.

        e0 -> hk -> b: FALSE -> s (flag = 1) -> x (exit), TRUE -> x
    With `expanded`, hk is an application method the graph calls into instead (callee k0 -> kx).
    """
    g = (GraphBuilder().node('e0').node('hk', assertion=callbacks).nodes_('r', 'b', 's').node('x', exit=True)
         .edge('e0', 'hk').edge('r', 'b').branch('b', 's', 'x').edge('s', 'x'))
    g.nodes[1]['statement'] = hook_statement
    if expanded:
        g.node('k0').node('kx', exit=True).edge('k0', 'kx').call('hk', 'k0', 'r', ['kx'])
    else:
        g.edge('hk', 'r')
    if callbacks:
        g.metadata['callbacksReachingAssert'] = ['<H: void run()>']
    return g


class AfterMainTest(unittest.TestCase):

    def test_leaving_main_is_interesting_when_a_callback_reaches_an_assert(self):
        # Both sides of b only lead to main's exit; the hook runs after it.
        n = hook_graph().load().nodes
        self.assertTrue(is_interesting(n['b'].get_fallthrough_child(), None))
        self.assertTrue(is_interesting(n['b'].get_branched_child(), None))

    def test_leaving_main_is_not_interesting_without_such_a_callback(self):
        n = hook_graph(callbacks=False).load().nodes
        self.assertFalse(is_interesting(n['b'].get_fallthrough_child(), None))
        self.assertFalse(is_interesting(n['b'].get_branched_child(), None))


class AsyncEntryTest(unittest.TestCase):

    def sites(self, statement, expanded=False):
        return hook_graph(statement, expanded).load().async_entry_sites()

    def test_library_calls_that_run_code_elsewhere(self):
        for statement in [
            HOOK,
            "virtualinvoke $r1.<java.lang.Thread: void start()>()",
            "virtualinvoke $r1.<Main$Worker: void start()>()",  # a Thread subclass
            "interfaceinvoke $r1.<java.lang.Thread$Builder: java.lang.Thread start(java.lang.Runnable)>($r2)",
            "staticinvoke <java.lang.Thread: void setDefaultUncaughtExceptionHandler(java.lang.Thread$UncaughtExceptionHandler)>($r1)",
            "interfaceinvoke $r1.<java.util.concurrent.ExecutorService: java.util.concurrent.Future submit(java.lang.Runnable)>($r2)",
            "staticinvoke <java.util.concurrent.CompletableFuture: java.util.concurrent.CompletableFuture runAsync(java.lang.Runnable)>($r1)",
            "virtualinvoke $r1.<java.util.Timer: void schedule(java.util.TimerTask,long)>($r2, 1L)",
        ]:
            self.assertEqual(len(self.sites(statement)), 1, statement)

    def test_other_calls(self):
        for statement in [
            "virtualinvoke $r1.<java.util.regex.Matcher: int start()>()",
            "virtualinvoke $r1.<java.lang.Thread: void join()>()",
            "virtualinvoke $r1.<java.io.PrintStream: void println(java.lang.String)>($r2)",
            "$r1 = new java.lang.Thread",
        ]:
            self.assertEqual(self.sites(statement), [], statement)

    def test_application_methods_of_the_same_name_do_not_count(self):
        self.assertEqual(self.sites("virtualinvoke $r1.<Main$Task: void execute()>()", expanded=True), [])


class PropertyGuardTest(unittest.TestCase):

    def run_sa(self, prop, graph=None):
        with tempfile.TemporaryDirectory() as d:
            path = os.path.join(d, 'graph.json')
            (graph or hook_graph(statement_ok())).save(path)
            args = SimpleNamespace(sa_file=path, sa_path=None, property=prop, parallel_sa=False,
                                   logdir=d, target='Main')
            sa = StaticPreAnalysis(args)
            sa.start()
            sa.wait()
            return sa, sa.take_graph()

    def test_valid_assert_loads_the_graph(self):
        sa, graph = self.run_sa('valid-assert.prp')
        self.assertEqual(sa.status, SAStatus.LOADED)
        self.assertIsNotNone(graph)

    def test_other_properties_load_nothing(self):
        for prop in ['no-runtime-exception.prp', 'no-deadlock.prp', None]:
            sa, graph = self.run_sa(prop)
            self.assertEqual(sa.status, SAStatus.DISABLED, prop)
            self.assertIsNone(graph)
            self.assertFalse(sa.stats(None)['enabled'])
            self.assertTrue(sa.stats(None)['requested'])

    def test_graph_with_code_outside_mains_call_tree_is_not_used(self):
        sa, graph = self.run_sa('valid-assert.prp', hook_graph())
        self.assertEqual(sa.status, SAStatus.REJECTED)
        self.assertIsNone(graph)
        self.assertIn('addShutdownHook', sa.stats(None)['rejected_reason'])


def statement_ok():
    return "virtualinvoke $r1.<java.io.PrintStream: void println(java.lang.String)>($r2)"


if __name__ == '__main__':
    unittest.main()
