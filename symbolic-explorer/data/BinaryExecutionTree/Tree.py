from data.trace.Branch import Branch
from data.BinaryExecutionTree.Leaf import Leaf
from data.BinaryExecutionTree.Node import Node
#import pygraphviz as pgv
class Tree:
    """
    Represents a binary execution tree used to model decision points in an execution flow.

    This tree is composed of `Node` and `Leaf` objects, where each `Node` can branch into
    two directions and a `Leaf` represents the end of a branch.

    Attributes:
        root (Node/Leaf): The root node of the tree. It can be either a `Node` or a `Leaf`.
        endpoint_id (any): An identifier for the endpoint associated with this tree.
    """

    def __init__(self, endpoint_id):
        """
        Initializes a new instance of the Tree class.

        Args:
            endpoint_id (any): An identifier for the endpoint this tree is associated with.
        """
        self.root = None
        self.endpoint_id = endpoint_id

    def add(self, trace, inputs):
        """
        Adds a branch to the tree based on the provided trace and inputs.

        Args:
            trace (list): A list of trace elements defining the execution path.
            inputs (any): The inputs associated with the branch being added.
        """
        self.root = self.add_recursive(None, self.root, trace, inputs)

    def add_recursive(self, parent, node, trace, inputs):
        """
        Recursively adds nodes or leaves to the tree.

        Args:
            parent (Node): The parent node of the current node.
            node (Node/Leaf): The current node to add to.
            trace (list): The remaining trace elements.
            inputs (any): The inputs associated with the current node.

        Returns:
            Node/Leaf: The newly added or modified node or leaf.

        Raises:
            ValueError: If branch IDs in the trace don't match the node's ID.
        """
        if node is None:
            # Create a new Node or Leaf if the current node is None
            return Node(parent, trace, inputs) if len(trace) > 0 else Leaf(parent, inputs)

        if len(trace) > 0:
            if isinstance(node, Node):
                new_node = trace.pop(0)

                # Ensure branch IDs match
                if node.id != new_node.id:
                    raise ValueError(f'Branch id\'s dont match: {node.id} : {new_node.id}')

                # Update constraints for the node
                if isinstance(new_node, Branch):
                    node.constraint[new_node.trace_id] = new_node.constraint

                # Recurse into the correct branch based on the trace
                if new_node.has_branched:
                    node.branched = self.add_recursive(node, node.branched, trace, inputs)
                else:
                    node.skipped = self.add_recursive(node, node.skipped, trace, inputs)
            else:
                # Create a new Node or Leaf if the current node is a Leaf
                return Node(parent, trace, inputs) if len(trace) > 0 else Leaf(parent, inputs)

        return node
    def get_constraint_label(self, parent, node):
        return None
        """Get the constraint label for an edge."""
        if isinstance(node, Leaf):
            return ""
        if node.trace_id in parent.constraint:
            return str(parent.constraint[node.trace_id])
        return ""

    def add_to_dot(self, node, graph, parent=None):
        return None
        """Recursively add nodes and edges to the DOT graph."""
        if node is not None:
            graph.add_node(node.gid, label=str(node.gid) + ':' + str(node.id))
            if parent is not None:
                # Add an edge with constraint as a label
                constraint_label = self.get_constraint_label(parent, node)
                graph.add_edge(parent.gid, node.gid, label=constraint_label)

            if isinstance(node, Node):
                self.add_to_dot(node.branched, graph, node)
                self.add_to_dot(node.skipped, graph, node)

    def plot_tree (self, idx):
        return None
        """Plot the tree using Graphviz and save to a file."""
        #log.info(self.to_string())
        G = pgv.AGraph(directed=True, strict=True, rankdir='TB')
        self.add_to_dot(self.root, G)
        G.layout(prog="dot")
        G.draw(f"tree_{idx}.png")
