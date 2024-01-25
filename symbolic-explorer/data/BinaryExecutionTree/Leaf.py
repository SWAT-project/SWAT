from data.trace.Input import Input

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

    def __init__(self, parent: 'Node', inputs: [Input]) -> None:
        """
        Initializes a new instance of the Leaf class.

        Parameters:
            parent (Node): The parent node of this leaf.
            inputs (list of Input): The inputs associated with this leaf.

        Returns:
            None
        """
        global GLOBAL_IID
        self.parent = parent
        self.inputs = inputs
        self.gid = GLOBAL_IID
        self.id = -1
        GLOBAL_IID -= 1

