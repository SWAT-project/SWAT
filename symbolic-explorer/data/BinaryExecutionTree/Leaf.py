from __future__ import annotations

from typing import TYPE_CHECKING

from data.trace.Input import Input
from data.trace.UF import UF

if TYPE_CHECKING:
    from data.BinaryExecutionTree.Node import Node

GLOBAL_IID = -1

class Leaf:
    """
    Represents a leaf node in a tree structure. A leaf is a node that does not have any children.
    Each leaf node has a unique global ID, a reference to its parent, and a list of inputs associated with it.

    Attributes:
        parent (Node): A reference to the parent node of this leaf.
        inputs (list of Input): A list of inputs associated with this leaf.
        gid (int): A unique global identifier for this leaf node.
    """

    # Whether a run ended here with an uncaught exception (see dfs: it makes the sliced branches
    # whose regions it ended in relevant). A class default, also for leaves built with __new__.
    abnormal: bool = False

    def __init__(self, parent: Node | None, inputs: list[Input], ufs: list[UF]) -> None:
        """
        Initializes a new instance of the Leaf class.

        Parameters:
            parent (Node): The parent node of this leaf.
            inputs (list of Input): The inputs associated with this leaf.

        Returns:
            None
        """
        global GLOBAL_IID
        self.parent: Node | None = parent
        self.inputs: list[Input] = inputs
        self.ufs: list[UF] = ufs
        self.gid: int = GLOBAL_IID
        self.id: int = -1
        GLOBAL_IID -= 1

