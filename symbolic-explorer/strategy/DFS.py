from data.BinaryExecutionTree.Node import Node
from data.BinaryExecutionTree.Leaf import Leaf
from data.StaticAnalysisGraph.SAGraph import SANode, CallStack, is_interesting, \
    apply_stack_delta, check_branch, resync
from data.StaticAnalysisGraph.Relevance import in_region
from data.trace.Special import UNMODELLED_EXCEPTION

import logging

import log
logger = log.get_logger()
# Log level of the per-node trace of the walk (enable with logger.setLevel(TRACE))
TRACE = logging.DEBUG - 5

# Whether dfs uses the graph's relevance analysis (see Relevance): set from --no-sa-slicing.
SLICING = True


def dfs(visited: set[Node], tree, node: Node | Leaf | None, solved_branches: set[int], unsat_branch_ids: set[int], sa_node: SANode | None = None, clinit_depth: int = 0, sa_stack: CallStack = None, distances: dict[Node, float] | None = None, sa_index=None, trace_stack=None) -> list[Node]:
    # sa_node and sa_stack together are where the SA walk stands: the node, and the call stack of
    # call sites the walk entered to get there. The same graph node means different things under
    # different stacks -- a method's exit returns to whichever call site is on top.
    # If `distances` is given, it receives each returned node's SA distance to an assertion point
    # along its unexplored side; nodes without SA information are left out.
    # sa_index and trace_stack: the graph's keys, and the call stack as the trace reports it (see
    # SAGraph.apply_stack_delta), against which every branch of the walk is checked.
    #
    # Relevance (slicing): a branch the graph marks irrelevant (Relevance), and that the walk paired
    # with it for sure, is *sliced*: it is not flipped, and its constraint is left out of every query
    # (StrategyService.collect_path_constrains). Each path carries the regions of the sliced
    # branches it is in (open regions); anything the graph cannot account for inside one -- a
    # branch it has no node for, a relevant branch, an exceptional end of the path -- makes those
    # branches relevant again for good (Node.poisoned), and the caller walks again. A candidate is
    # identified by its key and its path's relevant decisions (its sig) rather than by its tree
    # node, so that a candidate whose side another path with the same relevant decisions explored,
    # solved or found infeasible already is left out: paths that differ only in irrelevant
    # decisions are one path for the search.
    possible_nodes = []
    if tree is None:
        tree = _Scratch()
    sig_ids = getattr(tree, 'sig_ids', None)
    if sig_ids is None:
        sig_ids = tree.sig_ids = {}
    covered = set()
    tree.poisoned_in_walk = False
    tree.sliced_in_walk = 0
    if sa_index is None and sa_node is not None:
        sa_index = sa_node.graph_index

    # The walk is a pre-order over the tree: a node, then the subtree of its skipped side, then
    # that of its branched side. It keeps its own stack, since paths can be longer than the
    # interpreter's recursion limit allows; each entry is a node with the walk's state there.
    # walk_only: the entry is below a side that cannot reach a target, walked only because a path
    # there ended abnormally (Node.abnormal_below), which may make sliced branches relevant: it
    # yields no candidates.
    work = [(node, sa_node, clinit_depth, sa_stack, trace_stack, 0, None, False)]
    while work:
        node, sa_node, clinit_depth, sa_stack, trace_stack, sig, open_regions, walk_only = work.pop()
        assert clinit_depth >= 0
        if isinstance(node, Leaf) and open_regions is not None and getattr(node, 'abnormal', False):
            # The path ended with an uncaught exception inside the region of a sliced branch: the
            # other outcome might have gone on to a target.
            _poison(tree, open_regions, "path ended abnormally")
        if node is None or node in visited or not isinstance(node, Node):
            continue

        visited.add(node)

        if node.unmodelled_exception:
            # Some trace threw an exception the graph does not model right before this node (see
            # below): the graph cannot say where execution went from there.
            sa_node, sa_stack = None, None

        # Static initializers delimit themselves with CLINIT / INVOKECLINIT_END markers, so we
        # know whether the branches below this node were executed inside a <clinit>. The SA
        # graph does not model static initializers, so those branches have no counterpart there.
        if node.is_clinit_start():
            clinit_depth += 1
        elif node.is_clinit_end():
            # Note: A <clinit> left via an exception emits no end marker, so the markers are not guaranteed to be balanced.
            clinit_depth -= 1

        if node.kind == "Special": # skip over Special nodes (includes CLINIT / INVOKECLINIT_END)
            if node.inst == UNMODELLED_EXCEPTION:
                # The executor saw an exception the graph does not model (a null dereference, a
                # cast, a library call that threw, an Error): the graph cannot say where execution
                # went, so the walk has no information -- until it finds the graph again at a
                # later branch by key (resync, below).
                sa_node, sa_stack = None, None
            work.append((node.branched, sa_node, clinit_depth, sa_stack, trace_stack, sig, open_regions, walk_only))
            continue
        else:
            assert node.kind == "Branch"

        # A state this path reached before the branch was reached at a smaller position already:
        # nothing below can be reached only from here (Tree.register_loop_states).
        if getattr(node, 'loop_states', ()) and tree.dominated_by_loop_state(node):
            tree.loop_state_pruned += 1
            continue  # this subtree only; the walk goes on with the other entries

        # The frames entered and left since the previous branch, which happened before this one.
        if sa_index is not None:
            trace_stack = apply_stack_delta(trace_stack, node.pops, node.pushes, sa_index)

        # If clinit_depth > 0, we do not have corresponding SA information, so pretend that there is no SA and DON'T WALK the SA graph.
        # Likewise for a branch in application code that library code called back: the graph walks
        # over the library call, so the walk holds its place until the callback returns.
        # A choice branch (the executor's search for the element a symbolic index selects) has no
        # graph node either: both of its sides continue from the walk's current place.
        mask_sa_node = clinit_depth > 0 or node.callback or node.choice
        # Whether the walk's pairing of this branch with a graph node was checked (check_branch).
        # A pairing that could not be checked is still followed, so that a later branch can be
        # checked against the walk, but this branch is not pruned by it.
        verified = False

        # Get information on interesting paths. If sa_node is None, all paths should be considered interesting
        if sa_node and not mask_sa_node:
            walked = sa_node.walk_till_branch(sa_stack) # idempotent
            sa_node, sa_stack = walked if walked is not None else (None, None)
            if sa_node is not None:
                in_step = check_branch(sa_node, sa_stack, node.key, trace_stack)
                if in_step is False:
                    # The walk paired this branch with the wrong graph node: its pruning so far on
                    # this path may be wrong too where a branch could not be checked, so the
                    # verdict cannot be SAFE (SVCompDriver).
                    logger.warning(f"[DFS] SA desync @{node.id}: trace {node.key}, graph {sa_node.id} {sa_node.key}")
                    tree.record_execution_error('sa_desync', f'trace {node.key}, graph {sa_node.key}')
                    sa_node, sa_stack = None, None
                elif in_step is not None:
                    verified = True
                    trace_stack = sa_stack # the same frames; sharing the cells keeps later checks cheap
        if sa_node is None and not mask_sa_node and sa_index is not None:
            resumed = resync(node.key, trace_stack, sa_index)
            if resumed is not None:
                sa_node, sa_stack = resumed
                verified = True # found by the trace's key under the trace's own call stack
        unpruned = mask_sa_node or (sa_node is None) or not verified
        skip_is_interesting = unpruned or is_interesting(sa_node.get_fallthrough_child(), sa_stack)
        branch_is_interesting = unpruned or is_interesting(sa_node.get_branched_child(), sa_stack)

        # Relevance: close the regions this branch is outside of (innermost first), then slice it,
        # or make the regions it is still inside of relevant.
        if open_regions is not None and verified:
            while open_regions is not None:
                owner_node, owner_sa, owner_stack = open_regions[0]
                if in_region(owner_sa, owner_stack, sa_node, sa_stack):
                    break
                open_regions = open_regions[1]
        sliced = (SLICING and verified and sa_node.irrelevant
                  and not getattr(node, 'poisoned', False))
        # A branch a loop summary merged is no decision either: both outcomes lead to the merged
        # state, so it is not flipped and its constraint is left out like a sliced one's. It needs
        # no region: its code has no effects beyond the merged locals.
        merged = getattr(node, 'merged', False)
        # A loop the executor summarized although its body does other things too (a call, a field
        # or array access) is merged only if the graph shows that those cannot matter: its region
        # changes nothing relevant but its method's locals (Relevance: summary_ok). Like a sliced
        # branch it then opens a region, which whatever the graph cannot see inside poisons.
        # Unhonored, the closed forms with the kept constraints describe the same path: sound.
        conditional = merged and getattr(node, 'merged_conditional', False)
        if conditional:
            merged = (SLICING and verified and getattr(sa_node, 'summary_ok', False)
                      and not getattr(node, 'poisoned', False))
        node.sliced = sliced or merged
        if not sliced and not merged and open_regions is not None:
            _poison(tree, open_regions, f"branch {node.key or node.id} in its region"
                    + ("" if verified else " (not paired with the graph)"))
        ident = (sig, node.key if node.key is not None else ('id', node.id))
        node.ident = ident
        if merged and not sliced and not conditional:
            sig_skipped = sig_branched = sig
        elif sliced or merged:
            tree.sliced_in_walk += 1
            open_regions = ((node, sa_node, sa_stack), open_regions)
            sig_skipped = sig_branched = sig
        else:
            sig_skipped = _sig(sig_ids, ident, False)
            sig_branched = _sig(sig_ids, ident, True)
            if node.skipped is not None:
                covered.add((ident, False))
            if node.branched is not None:
                covered.add((ident, True))
            if node.gid in solved_branches or node.gid in unsat_branch_ids:
                covered.add((ident, node.branched is None))  # its open side
        
        # One line per branch node and round: at INFO this was most of the log (270 MB in 900 s on
        # MinePump) and much of the explorer's own CPU time, so it is below DEBUG.
        if logger.isEnabledFor(TRACE):
            logger.log(TRACE, f"[DFS] @{node.id}/{("CALLBACK" if node.callback else "CHOICE" if node.choice else "CLINIT") if mask_sa_node else sa_node and sa_node.id} ({"branched" if node.branched else ""}{"skipped" if node.skipped else ""}): skip_is_interesting={skip_is_interesting}, branch_is_interesting={branch_is_interesting}")
        
        # Add the node itself, if eligible
        if not node.sliced and not walk_only and ((node.skipped is None and skip_is_interesting) or (node.branched is None and branch_is_interesting)):
            if node.gid not in solved_branches \
            and node.gid not in unsat_branch_ids \
            and node.kind != "Special" \
            and not node.concrete_only:
                possible_nodes.append(node)
                if distances is not None and sa_node and not mask_sa_node:
                    unexplored = sa_node.get_fallthrough_child() if node.skipped is None else sa_node.get_branched_child()
                    distances[node] = unexplored.assertDistance

        # Walk the SA graph in step with the tree. Each side needs its own successor held in its
        # own variable: rebinding one shared local here would leave the branched walk
        # descending from the fallthrough child instead of from this branch.
        sa_skipped = sa_node
        sa_branched = sa_node
        if sa_node and not mask_sa_node:
            # walk_till_branch() returned a node with both children, or None.
            sa_skipped = sa_node.get_fallthrough_child()
            sa_branched = sa_node.get_branched_child()

        # Only walk the tree further if the path is interesting (leads to an assert) or if we
        # don't have information (sa_node is None). The branched side goes on the stack first, so
        # that the skipped side's subtree is walked before it.
        # A side that cannot reach a target is still walked (for the regions only) if a path below
        # it ended abnormally inside an open region.
        if branch_is_interesting and not walk_only:
            work.append((node.branched, sa_branched, clinit_depth, sa_stack, trace_stack, sig_branched, open_regions, False))
        elif open_regions is not None and _abnormal_below(node.branched):
            work.append((node.branched, sa_branched, clinit_depth, sa_stack, trace_stack, sig_branched, open_regions, True))
        if skip_is_interesting and not walk_only:
            work.append((node.skipped, sa_skipped, clinit_depth, sa_stack, trace_stack, sig_skipped, open_regions, False))
        elif open_regions is not None and _abnormal_below(node.skipped):
            work.append((node.skipped, sa_skipped, clinit_depth, sa_stack, trace_stack, sig_skipped, open_regions, True))

    # Candidates whose side a path with the same relevant decisions has covered already, and
    # repeats of the same candidate, are left out.
    tree.covered = covered
    kept = []
    seen = set()
    for n in possible_nodes:
        side = (n.ident, n.branched is None)  # (True: the branched side is the open one)
        if side in covered or side in seen:
            tree.dedup_in_walk = getattr(tree, 'dedup_in_walk', 0) + 1
            if distances is not None:
                distances.pop(n, None)
            continue
        seen.add(side)
        kept.append(n)
    return kept


class _Scratch:
    """What dfs keeps on the tree, for a walk given no tree (tests)."""
    def __init__(self):
        self.sig_ids = {}
        self.covered = set()
        self.loop_state_pruned = 0

    def record_execution_error(self, kind, message):
        pass

    def dominated_by_loop_state(self, node):
        return False


def _abnormal_below(child) -> bool:
    """Whether a path through `child` ended with an uncaught exception."""
    return child is not None and (getattr(child, 'abnormal_below', False) or getattr(child, 'abnormal', False))


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
        if not getattr(owner, 'poisoned', False):
            owner.poisoned = True
            tree.poisoned_in_walk = True
            tree.poisoned_total = getattr(tree, 'poisoned_total', 0) + 1
            logger.debug(f"[DFS] Sliced branch {owner.key or owner.id} is relevant after all: {why}")
        open_regions = open_regions[1]
