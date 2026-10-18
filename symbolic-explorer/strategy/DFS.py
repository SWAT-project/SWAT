from data.BinaryExecutionTree.Node import Node
from data.BinaryExecutionTree.Leaf import Leaf
from data.StaticAnalysisGraph.SAGraph import SANode, CallStack, is_interesting, \
    apply_stack_delta, check_branch, resync
from data.trace.Special import UNMODELLED_EXCEPTION

import log
logger = log.get_logger()

def dfs(visited: set[Node], tree, node: Node | Leaf | None, solved_branches: set[int], unsat_branch_ids: set[int], sa_node: SANode | None = None, clinit_depth: int = 0, sa_stack: CallStack = None, distances: dict[Node, float] | None = None, sa_index=None, trace_stack=None) -> list[Node]:
    # sa_node and sa_stack together are where the SA walk stands: the node, and the call stack of
    # call sites the walk entered to get there. The same graph node means different things under
    # different stacks -- a method's exit returns to whichever call site is on top.
    # If `distances` is given, it receives each returned node's SA distance to an assertion point
    # along its unexplored side; nodes without SA information are left out.
    # sa_index and trace_stack: the graph's keys, and the call stack as the trace reports it (see
    # SAGraph.apply_stack_delta), against which every branch of the walk is checked.
    possible_nodes = []
    if sa_index is None and sa_node is not None:
        sa_index = sa_node.graph_index

    # The walk is a pre-order over the tree: a node, then the subtree of its skipped side, then
    # that of its branched side. It keeps its own stack, since paths can be longer than the
    # interpreter's recursion limit allows; each entry is a node with the walk's state there.
    work = [(node, sa_node, clinit_depth, sa_stack, trace_stack)]
    while work:
        node, sa_node, clinit_depth, sa_stack, trace_stack = work.pop()
        assert clinit_depth >= 0
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
            work.append((node.branched, sa_node, clinit_depth, sa_stack, trace_stack))
            continue
        else:
            assert node.kind == "Branch"

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
        
        logger.info(f"[DFS] @{node.id}/{("CALLBACK" if node.callback else "CHOICE" if node.choice else "CLINIT") if mask_sa_node else sa_node and sa_node.id} ({"branched" if node.branched else ""}{"skipped" if node.skipped else ""}): skip_is_interesting={skip_is_interesting}, branch_is_interesting={branch_is_interesting}")
        
        # Add the node itself, if eligible
        if (node.skipped is None and skip_is_interesting) or (node.branched is None and branch_is_interesting):
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
        if branch_is_interesting:
            work.append((node.branched, sa_branched, clinit_depth, sa_stack, trace_stack))
        if skip_is_interesting:
            work.append((node.skipped, sa_skipped, clinit_depth, sa_stack, trace_stack))

    return possible_nodes
