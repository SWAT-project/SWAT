from data.BinaryExecutionTree.Node import Node


def dfs(visited, tree, node, solved_branches, unsat_branch_ids):
    possible_nodes = list()
    if node is not None \
            and node not in visited \
            and isinstance(node, Node):
        if (node.skipped is None or node.branched is None) \
                and node.gid not in solved_branches \
                and node.gid not in unsat_branch_ids \
                and node.kind != "Special":
            possible_nodes.append(node)
        visited.add(node)
        possible_nodes.extend(dfs(visited, tree, node.skipped, solved_branches, unsat_branch_ids))
        possible_nodes.extend(dfs(visited, tree, node.branched, solved_branches, unsat_branch_ids))
    return possible_nodes
