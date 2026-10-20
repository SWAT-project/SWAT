from data.BinaryExecutionTree.Leaf import Leaf
import logging as log
import json
from typing import List, Optional

from data.trace.Input import Input
from data.trace.Special import Special, CLINIT, CLINIT_END
from data.trace.UF import UF

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
        inst (str): For 'Special' nodes, the instruction that produced the marker
            (e.g. '...instruction.CLINIT'); None for 'Branch' nodes.

    Raises:
        ValueError: If no trace element is given, indicating that the node cannot be created.
    """

    def __init__(self, parent: Optional['Node'], branch, inputs: List[Input], ufs: List[UF]):
        """
        Initializes a new instance of the Node class for one trace element. Its children are left
        unset; Tree links the nodes of a trace into a path.

        Args:
            parent (Node): The parent node of this node.
            branch (Branch | Special): The trace element this node stands for.
            inputs (any): The inputs associated with this node.
            ufs (any): The UFs associated with this node.
        """
        global GLOBAL_IID
        self.parent = parent
        self.branched = None
        self.skipped = None
        self.inputs = inputs
        self.ufs = ufs
        self.gid = GLOBAL_IID  # Assign a unique global identifier
        GLOBAL_IID += 1
        self.constraint = {}
        self.inst = None  # Only set for Special nodes
        self.concrete_only = False  # A branch recorded only to keep the SA walk in step, see Branch
        # Only for 'Branch' nodes: its key, and the frame stack change since the previous branch
        # (see Branch). The same on every trace through the node, since its path is the same.
        self.key = None
        self.pops = 0
        self.pushes = ()
        self.callback = False  # in a callback from library code: no counterpart in the SA graph
        self.choice = False  # added by the executor to enumerate an operand's values: not in the SA graph
        # Whether some trace threw an exception the SA graph does not model right before this node
        # (an UNMODELLED_EXCEPTION marker, which is not a node of its own, see Tree.structural).
        self.unmodelled_exception = False
        # Only for 'Branch' nodes: the loop-head states reached before this branch, since the
        # previous one, each with its position (see Tree.register_loop_states).
        self.loop_states = ()

        if branch is None:
            raise ValueError(f'Cannot create Node with no branch!')

        # Assign branch details to the node
        self.id = branch.id
        self.trace_id = branch.trace_id

        # Check if the branch is of type Special and set properties accordingly
        if isinstance(branch, Special):
            self.kind = "Special"
            self.inst = branch.inst
        else:
            self.kind = "Branch"
            self.concrete_only = branch.concrete_only
            self.key = branch.key
            self.pops = branch.pops
            self.pushes = branch.pushes
            self.callback = branch.callback
            self.choice = branch.choice
            self.loop_states = getattr(branch, 'positioned_loop_states', ())
            self.constraint[branch.trace_id] = branch.constraint

    def is_clinit_start(self) -> bool:
        """
        Checks whether this node marks the beginning of a static initializer (<clinit>).

        Returns:
            bool: True if this is the CLINIT marker of a static initializer.
        """
        return self.kind == "Special" and self.inst == CLINIT

    def is_clinit_end(self) -> bool:
        """
        Checks whether this node marks the end of a static initializer (<clinit>).

        Returns:
            bool: True if this is the INVOKECLINIT_END marker of a static initializer.
        """
        return self.kind == "Special" and self.inst == CLINIT_END

    def __str__(self):
        """
        Returns a string representation of the node.

        Returns:
            str: A string representation of the node.
        """
        return f'Node({self.id}) -> Branched: {self.branched}, Skipped: {self.skipped}'