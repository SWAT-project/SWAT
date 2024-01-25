from data.BinaryExecutionTree.Leaf import Leaf
import logging as log
import json
from data.trace.Special import Special

GLOBAL_IID = 0  # Global unique identifier for nodes

class Node:
    """
    Represents a node in a binary execution tree.

    This class is used to create a tree structure where each node represents a decision point
    in the execution flow. It can branch into two directions (branched and skipped) based on 
    certain conditions. The tree can consist of `Node` objects and `Leaf` objects, with `Leaf` 
    representing the end of a branch.

    Attributes:
        parent (Node): The parent node in the tree.
        branched (Node): The child node representing the branch taken.
        skipped (Node): The child node representing the branch not taken.
        inputs (any): The inputs associated with this node.
        gid (int): A unique global identifier for the node.
        constraint (dict): A dictionary to hold constraints related to the node.
        id (any): The ID of the branch.
        trace_id (any): The trace ID of the branch.
        kind (str): The type of the node, either 'Special' or 'Branch'.

    Raises:
        ValueError: If the trace argument is None or empty, indicating that the node cannot be created.
    """

    def __init__(self, parent, trace, inputs):
        """
        Initializes a new instance of the Node class.

        Args:
            parent (Node): The parent node of this node.
            trace (list): A list of trace elements defining the execution path.
            inputs (any): The inputs associated with this node.
        """
        global GLOBAL_IID
        self.parent = parent
        self.branched = None
        self.skipped = None
        self.inputs = inputs
        self.gid = GLOBAL_IID  # Assign a unique global identifier
        GLOBAL_IID += 1
        self.constraint = {}

        # Validate trace argument
        if trace is None or len(trace) == 0:
            raise ValueError(f'Cannot create Node with no branch!')

        # Process the first element of the trace to determine the branch
        branch = trace.pop(0)

        # Create a child node based on remaining trace elements
        if len(trace) == 0:
            child = Leaf(parent=self, inputs=inputs)
        else:
            child = Node(parent=self, trace=trace, inputs=inputs)

        # Assign branch details to the node
        self.id = branch.id
        self.trace_id = branch.trace_id

        # Check if the branch is of type Special and set properties accordingly
        if isinstance(branch, Special):
            self.kind = "Special"
            self.branched = child
        else:
            self.kind = "Branch"
            self.constraint[branch.trace_id] = branch.constraint
            # Determine whether the branch was taken or skipped and assign child
            if branch.has_branched:
                self.branched = child
            else:
                self.skipped = child
