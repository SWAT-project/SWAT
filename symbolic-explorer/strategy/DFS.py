from data.BinaryExecutionTree.Node import Node
from data.BinaryExecutionTree.Leaf import Leaf
from data.StaticAnalysisGraph.SAGraph import SANode

def dfs(visited: set[Node], tree, node: Node | Leaf | None, solved_branches: set[int], unsat_branch_ids: set[int], sa_node: SANode | None = None) -> list[Node]:
    possible_nodes = []
    
    if node is not None \
      and node not in visited \
      and isinstance(node, Node):

        visited.add(node)

        if node.kind == "Special": # skip over Special nodes
            possible_nodes.extend(dfs(visited, tree, node.branched, solved_branches, unsat_branch_ids, sa_node))
            return possible_nodes
        else:
            assert node.kind == "Branch"
    
        # Get information on interesting paths. If sa_node is None, all paths should be considered interesting
        if sa_node:
            sa_node = sa_node.walk_till_branch()
        skip_is_interesting = (sa_node is None) or sa_node.get_fallthrough_child().onPathToAssert
        branch_is_interesting = (sa_node is None) or sa_node.get_branched_child().onPathToAssert
        
        print(f"[DFS] @{node.id}/{sa_node and sa_node.id} ({"branched" if node.branched else ""}{"skipped" if node.skipped else ""}): skip_is_interesting={skip_is_interesting}, branch_is_interesting={branch_is_interesting}")
        
        # Add the node itself, if eligible
        if (node.skipped is None and skip_is_interesting) or (node.branched is None and branch_is_interesting):
            if node.gid not in solved_branches \
            and node.gid not in unsat_branch_ids \
            and node.kind != "Special":
                possible_nodes.append(node)
        
        # Only walk the tree further if the path is interesting (leads to an assert) or if we don't have information (sa_node is None)
        if skip_is_interesting:
            possible_nodes.extend(dfs(visited, tree, node.skipped, solved_branches, unsat_branch_ids, sa_node and sa_node.get_fallthrough_child()))
        if branch_is_interesting:
            possible_nodes.extend(dfs(visited, tree, node.branched, solved_branches, unsat_branch_ids, sa_node and sa_node.get_branched_child()))
    
    return possible_nodes
