"""
Cross-checks the context-sensitive marking against a brute-force oracle on real extractor output.

For each graph, random walks from the entry (with the same call-stack semantics as the DFS walk)
sample (node, stack) states at branches. For both children of each sampled branch it compares

  * is_interesting(child, stack)          -- the summaries, evaluated against the stack
  * an explicit search over realizable paths from (child, stack), bounded in stack depth
  * context-insensitive marking           -- plain backward reachability over every edge

and reports any child the oracle can reach an assert from but is_interesting() calls
uninteresting (a soundness bug), and how many children each marking prunes.

    python3 tests/check_marking_on_graphs.py GRAPH.json [GRAPH.json ...]
"""

import os
import random
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))
sys.setrecursionlimit(100000)

from data.StaticAnalysisGraph import SAGraph as sagraph
from data.StaticAnalysisGraph.SAGraph import SAGraph, push, pop, depth, unwind

ORACLE_DEPTH = 6
ORACLE_STATES = 200_000


def oracle(node, stack):
    """True / False if an assert is (not) reachable on a realizable path; None if inconclusive."""
    worklist = [(node, stack)]
    seen = set()
    truncated = False
    while worklist:
        n, st = worklist.pop()
        key = (id(n), stack_key(st))
        if key in seen:
            continue
        seen.add(key)
        if len(seen) > ORACLE_STATES:
            return None
        if n.isAssert or n.isIncomplete:
            return True
        succ = list(n.next_exceptional)
        if n.next_branched is not None:
            succ.append(n.next_branched)
        if n.is_call_site():
            if depth(st) >= ORACLE_DEPTH + depth(stack):
                truncated = True
            else:
                worklist.append((n.next_fallthrough, push(st, n)))
        elif n.next_fallthrough is not None:
            succ.append(n.next_fallthrough)
        worklist.extend((s, st) for s in succ)
        if n.isExit and st is not None:
            frame, below = pop(st)
            worklist.append((frame.return_site, below))
        if n.exceptionChain is not None and st is not None:
            resumed = unwind(n.exceptionChain, st)
            if resumed is not None:
                worklist.append(resumed)
    return None if truncated else False


def stack_key(st):
    ids = []
    while st is not None:
        ids.append(id(st[0]))
        st = st[1]
    return tuple(ids)


def context_insensitive(graph):
    marked = set()
    work = [n for n in graph.nodes.values() if n.isAssert]
    while work:
        n = work.pop()
        if id(n) in marked:
            continue
        marked.add(id(n))
        work.extend(n.prev)
    return marked


def check(path, walks=30, steps=400, rng=random.Random(0)):
    g = SAGraph()
    g.load_json_graph(path)
    ci = context_insensitive(g)
    stats = dict(samples=0, cs_pruned=0, ci_pruned=0, unsound=0, oracle_none=0, cs_over=0)
    seen = set()
    for _ in range(walks):
        state = g.entry_node.walk_till_branch(None)
        for _ in range(steps):
            if state is None:
                break
            branch, stack = state
            for child in (branch.get_fallthrough_child(), branch.get_branched_child()):
                key = (id(child), stack_key(stack))
                if key in seen:
                    continue
                seen.add(key)
                cs = sagraph.is_interesting(child, stack)
                truth = oracle(child, stack)
                stats['samples'] += 1
                stats['cs_pruned'] += not cs
                stats['ci_pruned'] += id(child) not in ci
                if truth is None:
                    stats['oracle_none'] += 1
                elif truth and not cs:
                    stats['unsound'] += 1
                    print(f'UNSOUND {path}: {child.id} depth={depth(stack)}')
                elif cs and not truth:
                    stats['cs_over'] += 1
                if not cs and id(child) in ci:
                    pass
                elif cs and id(child) not in ci:
                    print(f'CS MORE PERMISSIVE THAN CI {path}: {child.id}')
            nxt = rng.choice([branch.get_fallthrough_child(), branch.get_branched_child()])
            state = nxt.walk_till_branch(stack)
    return stats


if __name__ == '__main__':
    total = {}
    for p in sys.argv[1:]:
        s = check(p)
        for k, v in s.items():
            total[k] = total.get(k, 0) + v
    print(total)
