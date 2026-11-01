"""
The walk that finds the branches worth flipping.

dfs() walks the execution tree and, if there is one, the static pre-analysis (SA) graph in step,
pairing branch for branch by position (see SAGraph for the terms). At each branch of the tree it

1. **pairs** the branch with a graph branch (`_pair`): it follows the graph to its next branch and
   checks that branch's key and call stack against the trace. A mismatch is a *desync*: it is
   recorded (which rules out SAFE) and the walk is lost. A lost walk tries to find the graph again
   by the trace's key (*resync*). A branch the graph has no node for (in a static initializer, in a
   callback from library code, a choice branch) is *masked*: the walk holds its place. Only a
   *verified* pairing (checked by key and stack, or found by resync) prunes or slices anything.
2. **prunes**: a side from which no target point is interesting is neither a candidate nor walked.
3. **slices** (`_classify`): a branch the relevance analysis finds cannot affect the targets is no
   decision: it is not flipped and its constraint is left out of every query
   (StrategyService.collect_path_constrains). Nor is a branch a loop summary merged. Candidates are
   identified by the path's relevant decisions (its *sig*) and their key, so paths that differ only
   in other decisions are one path for the search.

Slicing trusts the graph about the region of a sliced branch (Relevance). Each path carries the
regions it is in (its *open regions*); whatever the graph cannot account for inside one -- a branch
it has no node for, a relevant branch, an uncaught exception that ends the path -- makes those
branches relevant for good (*poisons* them, Node.poisoned), and the caller walks again.

A lost or unverified walk prunes and slices nothing, which is always safe.
"""
from typing import NamedTuple

from data.BinaryExecutionTree.Node import Node
from data.BinaryExecutionTree.Leaf import Leaf
from data.BinaryExecutionTree.Tree import Tree
from data.StaticAnalysisGraph.SAGraph import SANode, CallStack, GraphIndex, is_interesting, \
    apply_stack_delta, check_branch, resync
from data.StaticAnalysisGraph.Relevance import in_region

import logging

import log
logger = log.get_logger()
# Log level of the per-node trace of the walk (enable with logger.setLevel(TRACE))
TRACE = logging.DEBUG - 5

# Whether dfs uses the graph's relevance analysis (see Relevance): set from --no-sa-slicing.
SLICING = True

# How the walk paired a branch with the graph (see _pair). Only VERIFIED and RESYNCED pairings prune
# or slice anything.
VERIFIED = "verified"      # the graph's branch, checked by key and call stack
RESYNCED = "resynced"      # found again by key, under the trace's call stack, after the walk was lost
UNVERIFIED = "unverified"  # followed, but a key or a call site is missing to check it
LOST = "lost"              # the graph cannot say where the execution is
MASKED = "masked"          # a branch the graph has no node for: the walk holds its place

# What a branch is for the search (see _classify).
DECISION = "decision"  # flipped, its constraint kept, its outcome part of the path's sig
SLICED = "sliced"      # irrelevant, or a summarized loop whose other effects are: opens a region
MERGED = "merged"      # merged by a loop summary with only local effects: needs no region


class _At(NamedTuple):
    """The walk's state on reaching a tree node."""
    node: object               # the tree node (a Node, a Leaf, or None)
    sa_node: SANode | None     # where the graph walk stands; None: lost
    sa_stack: CallStack        # the graph walk's call stack
    trace_stack: object        # the call stack as the trace reports it (SAGraph.apply_stack_delta)
    clinit_depth: int          # how many static initializers the path is in
    sig: int                   # the path's relevant decisions so far (see _sig)
    open_regions: tuple | None # the regions of sliced branches the path is in, innermost first:
                               # ((tree node, graph node, call stack), outer regions)
    walk_only: bool            # below a side that cannot reach a target, walked only because a
                               # path there ended abnormally (_abnormal_below): yields no candidates


def dfs(tree: Tree | None, root, solved_branches: set[int], unsat_branch_ids: set[int],
        sa_entry: SANode | None = None, distances: dict[Node, float] | None = None) -> list[Node]:
    """
    The branches below `root` worth flipping: those with an unexplored side that is interesting,
    that are no sliced or merged branch, not solved or found infeasible already, and not covered
    by another path with the same relevant decisions.

    `sa_entry` is the graph's entry node (None: no graph). If `distances` is given, it receives
    each returned node's SA distance to a target point along its unexplored side; nodes without SA
    information are left out. The walk records its counts, desyncs and covered sides on `tree`.
    """
    if tree is None:
        tree = Tree()
    tree.begin_walk()
    counts = tree.walk
    index: GraphIndex | None = sa_entry.graph_index if sa_entry is not None else None
    possible_nodes = []
    # The (candidate identity, side) pairs explored, solved or found infeasible on some path.
    covered = set()

    # A pre-order over the tree: a node, then the subtree of its skipped side, then that of its
    # branched side. It keeps its own stack, since paths can be longer than the interpreter's
    # recursion limit allows.
    work = [_At(root, sa_entry, None, None, 0, 0, None, False)]
    while work:
        at = work.pop()
        node, sa_node, sa_stack, trace_stack, clinit_depth, sig, open_regions, walk_only = at
        if isinstance(node, Leaf) and open_regions is not None and node.abnormal:
            # The path ended with an uncaught exception inside the region of a sliced branch: the
            # other outcome might have gone on to a target.
            _poison(tree, open_regions, "path ended abnormally")
        if not isinstance(node, Node):
            continue

        if node.unmodelled_exception:
            # Some trace threw an exception the graph does not model (a null dereference, a cast, a
            # library call that threw, an Error) right before this node: the graph cannot say
            # where execution went, so the walk is lost -- until it finds the graph again by key
            # (resync, in _pair). The UNMODELLED_EXCEPTION marker itself is no tree node
            # (Tree.structural), so that runs which do and do not throw share the tree.
            sa_node, sa_stack = None, None

        if node.kind == "Special":
            # Static initializers delimit themselves with CLINIT / INVOKECLINIT_END markers. One left
            # by an exception emits no end marker: the rest of the path then counts as inside it.
            if node.is_clinit_start():
                clinit_depth += 1
            elif node.is_clinit_end():
                clinit_depth -= 1
                assert clinit_depth >= 0
            work.append(at._replace(node=node.branched, sa_node=sa_node, sa_stack=sa_stack,
                                    clinit_depth=clinit_depth))
            continue

        # A state this path reached before the branch was reached at a smaller position already:
        # nothing below can be reached only from here (Tree.register_loop_states).
        if node.loop_states and tree.dominated_by_loop_state(node):
            counts['loop_state_pruned'] += 1
            continue
        counts['branches'] += 1

        # 1. Pair the branch with a graph branch.
        if index is not None:
            # The frames entered and left since the previous branch, which happened before this one.
            trace_stack = apply_stack_delta(trace_stack, node.pops, node.pushes, index)
        # The graph has no node for a branch in a static initializer, in application code that
        # library code called back (the graph steps over the library call), or for a choice branch
        # (the executor's search for the element a symbolic index selects): the walk holds its place
        # until a branch it does have a node for.
        masked = clinit_depth > 0 or node.callback or node.choice
        if masked:
            pairing = MASKED
        else:
            sa_node, sa_stack, trace_stack, pairing = _pair(tree, node, sa_node, sa_stack, trace_stack, index)
        counts[pairing] += 1
        verified = pairing in (VERIFIED, RESYNCED)

        # 2. Prune: which sides can still reach a target point.
        if verified:
            skip_is_interesting = is_interesting(sa_node.next_fallthrough, sa_stack)
            branch_is_interesting = is_interesting(sa_node.next_branched, sa_stack)
            counts['pruned_sides'] += (not skip_is_interesting) + (not branch_is_interesting)
        else:
            skip_is_interesting = branch_is_interesting = True

        # 3. Slice: close the regions the branch is outside of, then see what it is for the search.
        if open_regions is not None and verified:
            while open_regions is not None:
                _, owner_sa, owner_stack = open_regions[0]
                if in_region(owner_sa, owner_stack, sa_node, sa_stack):
                    break
                open_regions = open_regions[1]
        role = _classify(node, sa_node, verified)
        node.sliced = role != DECISION
        if role != DECISION:
            counts[role] += 1
        if role == DECISION and open_regions is not None:
            _poison(tree, open_regions, f"branch {node.key or node.id} in its region"
                    + ("" if verified else " (not paired with the graph)"))
        node.ident = (sig, node.key if node.key is not None else ('id', node.id))
        if role == DECISION:
            sig_skipped = _sig(tree.sig_ids, node.ident, False)
            sig_branched = _sig(tree.sig_ids, node.ident, True)
            if node.skipped is not None:
                covered.add((node.ident, False))
            if node.branched is not None:
                covered.add((node.ident, True))
            if node.gid in solved_branches or node.gid in unsat_branch_ids:
                covered.add((node.ident, node.branched is None))  # its open side
        else:
            sig_skipped = sig_branched = sig
            if role == SLICED:
                open_regions = ((node, sa_node, sa_stack), open_regions)

        # One line per branch node and round: at INFO this was most of the log (270 MB in 900 s on
        # MinePump) and much of the explorer's own CPU time, so it is below DEBUG.
        if logger.isEnabledFor(TRACE):
            place = ("CALLBACK" if node.callback else "CHOICE" if node.choice else "CLINIT") if masked \
                else sa_node.id if sa_node is not None else "-"
            logger.log(TRACE, f"[DFS] @{node.id}/{place} {pairing} "
                              f"({'branched' if node.branched else ''}{'skipped' if node.skipped else ''}, {role}): "
                              f"skip_is_interesting={skip_is_interesting}, branch_is_interesting={branch_is_interesting}")

        # The node itself, if its open side is worth flipping.
        if role == DECISION and not walk_only and not node.concrete_only \
                and ((node.skipped is None and skip_is_interesting) or (node.branched is None and branch_is_interesting)) \
                and node.gid not in solved_branches and node.gid not in unsat_branch_ids:
            possible_nodes.append(node)
            if distances is not None and sa_node is not None and not masked:
                unexplored = sa_node.next_fallthrough if node.skipped is None else sa_node.next_branched
                distances[node] = unexplored.targetDistance

        # Walk on along both sides, each with its own successor in the graph. The branched side goes
        # on the stack first, so that the skipped side's subtree is walked before it. A side that
        # cannot reach a target is still walked (for the regions only) if a path below it ended
        # abnormally inside an open region.
        if sa_node is not None and not masked:
            sa_skipped, sa_branched = sa_node.next_fallthrough, sa_node.next_branched
        else:
            sa_skipped = sa_branched = sa_node
        for child, sa_child, interesting, child_sig in ((node.branched, sa_branched, branch_is_interesting, sig_branched),
                                                        (node.skipped, sa_skipped, skip_is_interesting, sig_skipped)):
            if interesting and not walk_only:
                child_walk_only = False
            elif open_regions is not None and _abnormal_below(child):
                child_walk_only = True
            else:
                continue
            work.append(_At(child, sa_child, sa_stack, trace_stack, clinit_depth, child_sig, open_regions,
                            child_walk_only))

    # Candidates whose side a path with the same relevant decisions has covered already, and
    # repeats of the same candidate, are left out.
    tree.covered = covered
    kept = []
    seen = set()
    for n in possible_nodes:
        side = (n.ident, n.branched is None)  # (True: the branched side is the open one)
        if side in covered or side in seen:
            counts['covered'] += 1
            if distances is not None:
                distances.pop(n, None)
            continue
        seen.add(side)
        kept.append(n)
    return kept


def _pair(tree, node: Node, sa_node: SANode | None, sa_stack: CallStack, trace_stack, index: GraphIndex | None):
    """
    Pairs the tree's branch `node` with a graph branch: follows the graph from `sa_node` to its next
    branch and checks that against the trace; if the walk is lost (or just found out of step),
    finds the graph again by the trace's key. Returns (sa_node, sa_stack, trace_stack, pairing):
    the graph branch and its call stack (None: lost), the trace stack, and how the branch was
    paired (VERIFIED, RESYNCED, UNVERIFIED or LOST). A pairing that cannot be checked (a key or a
    call site missing) is still followed, so that a later branch can be checked against the walk,
    but it is UNVERIFIED.
    """
    if sa_node is not None:
        walked = sa_node.walk_till_branch(sa_stack)  # idempotent
        sa_node, sa_stack = walked if walked is not None else (None, None)
    if sa_node is not None:
        in_step = check_branch(sa_node, sa_stack, node.key, trace_stack)
        if in_step is True:
            # The same frames: sharing the cells keeps later checks cheap (SAGraph.same_stacks).
            return sa_node, sa_stack, sa_stack, VERIFIED
        if in_step is None:
            return sa_node, sa_stack, trace_stack, UNVERIFIED
        # The walk paired this branch with the wrong graph node: its pruning so far on this path
        # may be wrong too where a branch could not be checked, so the verdict cannot be SAFE
        # (SVCompDriver).
        logger.warning(f"[DFS] SA desync @{node.id}: trace {node.key}, graph {sa_node.id} {sa_node.key}")
        tree.record_execution_error('sa_desync', f'trace {node.key}, graph {sa_node.key}')
        tree.walk['desyncs'] += 1
    resumed = resync(node.key, trace_stack, index)
    if resumed is None:
        return None, None, trace_stack, LOST
    # Found by the trace's key under the trace's own call stack.
    return resumed[0], resumed[1], trace_stack, RESYNCED


def _classify(node: Node, sa_node: SANode | None, verified: bool) -> str:
    """
    What the branch is for the search: a DECISION, SLICED or MERGED.

    A loop summary's branch (Node.merged) is no decision: both outcomes lead to the merged state.
    If the executor merged a loop whose body does other things too (a call, a field or array
    access; Node.merged_conditional), that holds only if the graph shows that those cannot matter
    (Relevance: summary_ok), and like a sliced branch it then opens a region. Unhonored, the closed
    forms with the kept constraints describe the same path: sound.
    """
    if node.merged and not node.merged_conditional:
        return MERGED
    if SLICING and verified and not node.poisoned \
            and (sa_node.irrelevant or (node.merged and sa_node.summary_ok)):
        return SLICED
    return DECISION


def _abnormal_below(child) -> bool:
    """Whether a path through `child` ended with an uncaught exception."""
    if isinstance(child, Leaf):
        return child.abnormal
    return child is not None and child.abnormal_below


def _sig(sig_ids: dict, ident: tuple, side: bool) -> int:
    """The sig of a path that has the relevant decisions of sig `ident[0]`, then took `side` at
    the branch `ident[1]`: a number for the sequence of relevant decisions, interned on the tree so
    that it is the same in every walk."""
    key = (ident, side)
    s = sig_ids.get(key)
    if s is None:
        s = len(sig_ids) + 1
        sig_ids[key] = s
    return s


def _poison(tree, open_regions, why: str) -> None:
    """Makes the sliced branches whose regions are open relevant again, for good."""
    while open_regions is not None:
        owner = open_regions[0][0]
        if not owner.poisoned:
            owner.poisoned = True
            tree.walk['poisoned'] += 1
            tree.poisoned_total += 1
            logger.debug(f"[DFS] Sliced branch {owner.key or owner.id} is relevant after all: {why}")
        open_regions = open_regions[1]


def describe_walk(counts) -> str:
    """One line on what a walk did, from its counts (Tree.walk)."""
    c = counts
    line = (f"{c['branches']} branches: {c[VERIFIED] + c[RESYNCED]} paired with the graph ({c[RESYNCED]} by resync), "
            f"{c[UNVERIFIED]} unverified, {c[LOST]} lost, {c[MASKED]} masked; {c['pruned_sides']} sides pruned")
    if c[SLICED] or c[MERGED] or c['covered'] or c['poisoned']:
        line += (f"; {c[SLICED]} sliced, {c[MERGED]} merged, {c['covered']} candidates covered by a path with "
                 f"the same relevant decisions, {c['poisoned']} sliced branches found relevant")
    if c['desyncs']:
        line += f"; {c['desyncs']} desyncs"
    if c['loop_state_pruned']:
        line += f"; {c['loop_state_pruned']} subtrees left out below a repeated loop-head state"
    return line
