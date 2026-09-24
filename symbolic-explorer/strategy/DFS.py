from data.BinaryExecutionTree.Node import Node
from data.BinaryExecutionTree.Leaf import Leaf
from data.StaticAnalysisGraph.SAGraph import SANode

import log
logger = log.get_logger()

def dfs(visited: set[Node], tree, node: Node | Leaf | None, solved_branches: set[int], unsat_branch_ids: set[int], sa_node: SANode | None = None, clinit_depth: int = 0) -> list[Node]:
    assert clinit_depth >= 0
    possible_nodes = []
    
    if node is not None \
      and node not in visited \
      and isinstance(node, Node):

        visited.add(node)
        
        # Static initializers delimit themselves with CLINIT / INVOKECLINIT_END markers, so we
        # know whether the branches below this node were executed inside a <clinit>. The SA
        # graph does not model static initializers, so those branches have no counterpart there.
        if node.is_clinit_start():
            clinit_depth += 1
        elif node.is_clinit_end():
            # Note: A <clinit> left via an exception emits no end marker, so the markers are not guaranteed to be balanced.
            clinit_depth -= 1

        if node.kind == "Special": # skip over Special nodes (includes CLINIT / INVOKECLINIT_END)
            possible_nodes.extend(dfs(visited, tree, node.branched, solved_branches, unsat_branch_ids, sa_node, clinit_depth))
            return possible_nodes
        else:
            assert node.kind == "Branch"
        
        # If clinit_depth > 0, we do not have corresponding SA information, so pretend that there is no SA and DON'T WALK the SA graph
        mask_sa_node = clinit_depth > 0
    
        # Get information on interesting paths. If sa_node is None, all paths should be considered interesting
        if sa_node:
            sa_node = sa_node.walk_till_branch() # idempotent
        skip_is_interesting = mask_sa_node or (sa_node is None) or sa_node.get_fallthrough_child().onPathToAssert
        branch_is_interesting = mask_sa_node or (sa_node is None) or sa_node.get_branched_child().onPathToAssert
        
        logger.info(f"[DFS] @{node.id}/{"CLINIT" if mask_sa_node else sa_node and sa_node.id} ({"branched" if node.branched else ""}{"skipped" if node.skipped else ""}): skip_is_interesting={skip_is_interesting}, branch_is_interesting={branch_is_interesting}")
        
        # Add the node itself, if eligible
        if (node.skipped is None and skip_is_interesting) or (node.branched is None and branch_is_interesting):
            if node.gid not in solved_branches \
            and node.gid not in unsat_branch_ids \
            and node.kind != "Special":
                possible_nodes.append(node)
        
        # Walk the SA graph in step with the tree. Each side needs its own successor held in its
        # own variable: rebinding one shared local here would leave the branched recursion
        # descending from the fallthrough child instead of from this branch.
        sa_skipped = sa_node
        sa_branched = sa_node
        if sa_node and not mask_sa_node:
            # walk_till_branch() returned a node with both children, or None.
            sa_skipped = sa_node.get_fallthrough_child()
            sa_branched = sa_node.get_branched_child()

        # Only walk the tree further if the path is interesting (leads to an assert) or if we don't have information (sa_node is None)
        if skip_is_interesting:
            possible_nodes.extend(dfs(visited, tree, node.skipped, solved_branches, unsat_branch_ids, sa_skipped, clinit_depth))
        if branch_is_interesting:
            possible_nodes.extend(dfs(visited, tree, node.branched, solved_branches, unsat_branch_ids, sa_branched, clinit_depth))
    
    return possible_nodes
