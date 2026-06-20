from typing import List, Optional, Set, Union
from data.trace.Branch import Branch
from data.trace.Special import Special
from data.BinaryExecutionTree.Leaf import Leaf
from data.BinaryExecutionTree.Node import Node
from data.trace.Input import Input
from data.trace.UF import UF

import log
logger = log.get_logger()
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

    def __init__(self, endpoint_id: Union[str, int]):
        """
        Initializes a new instance of the Tree class.

        Args:
            endpoint_id (any): An identifier for the endpoint this tree is associated with.
        """
        self.root = None
        self.endpoint_id = endpoint_id
        self.symbolic_context_loss = False
        self.symbolic_precision_loss = False
        self.reference_semantic_change = False
        self.uncaught_exceptions: int = 0
        self.symbolic_vars: Set = set()
        self.ufs: Set = set()
        # Missing invocations accumulated across all traces of this testcase, keyed by
        # (owner, name, desc, isInstance, isSymbolic). Each value aggregates count and context_loss.
        self.missing_invocations: dict = {}
        # Execution errors detected in the executor's stdout (internal SWAT assertions or
        # [SWAT Exception]s) that make the verdict untrustworthy. These cannot ride the trace
        # because they halt the executor JVM before it sends one, so they are surfaced here.
        self.execution_errors: list = []


    def record_inputs(self, inputs: List[Input]):
        """
        Records the inputs associated with the tree.

        Args:
            inputs (list): A list of inputs associated with the tree.
        """
        for input in inputs:
            self.symbolic_vars.add(input.name)
                                   
    def record_ufs(self, ufs: List[UF]):
        """
        Records the UFs associated with the tree.

        Args:
            ufs (list): A list of UFs associated with the tree.
        """
        for uf in ufs:
            self.ufs.add(uf.definition)
                                   

    def record_missing_invocations(self, invocations: Optional[List[dict]]):
        """
        Accumulates missing invocations across all traces of this testcase.

        Args:
            invocations (list): Dicts with keys owner, name, desc, isInstance, isSymbolic,
                contextLoss, count. Entries are deduped by
                (owner, name, desc, isInstance, isSymbolic); counts are summed and contextLoss
                is OR-ed across traces. The full set is the superset of missing invocations;
                entries with context_loss=True form the context-loss subset.
        """
        if not invocations:
            return
        for inv in invocations:
            key = (inv['owner'], inv['name'], inv['desc'], inv['isInstance'], inv['isSymbolic'])
            count = int(inv.get('count', 1))
            context_loss = bool(inv.get('contextLoss', False))
            existing = self.missing_invocations.get(key)
            if existing is None:
                self.missing_invocations[key] = {
                    'owner': inv['owner'],
                    'name': inv['name'],
                    'desc': inv['desc'],
                    'isInstance': inv['isInstance'],
                    'isSymbolic': inv['isSymbolic'],
                    'context_loss': context_loss,
                    'count': count,
                }
            else:
                existing['count'] += count
                existing['context_loss'] = existing['context_loss'] or context_loss

    def record_execution_error(self, kind: str, message: str):
        """
        Records an execution error surfaced from the executor's stdout.

        Deduplicated by (kind, message) so repeated exploration rounds don't inflate the list.

        Args:
            kind (str): A short category, e.g. "swat_assertion" or "swat_exception".
            message (str): The offending output line.
        """
        entry = {'kind': kind, 'message': message}
        if entry not in self.execution_errors:
            self.execution_errors.append(entry)

    def record_context_loss(self):
        logger.warning("Context loss recorded!")
        self.symbolic_context_loss = True
        
    def record_precision_loss(self):
        logger.warning("Precision loss recorded!")
        self.symbolic_precision_loss = True

    def record_reference_semantic_change(self):
        logger.warning("Reference semantic change recorded: user-de-interned strings compared via Objects.equals")
        self.reference_semantic_change = True
        
    def add(self, trace: list[Branch | Special], inputs: List[Input], ufs: List[UF]):
        """
        Adds a branch to the tree based on the provided trace and inputs.

        Args:
            trace (list): A list of trace elements defining the execution path.
            inputs (any): The inputs associated with the branch being added.
            ufs (any): The UFs associated with the branch being added.
        """
        self.root = self.add_recursive(None, self.root, trace.copy(), inputs, ufs)

    def add_recursive(self, parent: Optional[Node], node: Optional[Union[Node, Leaf]], trace: list[Branch | Special], inputs: List[Input], ufs: List[UF]):
        """
        Recursively adds nodes or leaves to the tree.

        Args:
            parent (Node): The parent node of the current node.
            node (Node/Leaf): The current node to add to.
            trace (list): The remaining trace elements.
            inputs (any): The inputs associated with the current node.
            ufs (any): The UFs associated with the current node.

        Returns:
            Node/Leaf: The newly added or modified node or leaf.

        Raises:
            ValueError: If branch IDs in the trace don't match the node's ID.
        """
        if node is None:
            # Create a new Node or Leaf if the current node is None
            return Node(parent, trace, inputs, ufs) if len(trace) > 0 else Leaf(parent, inputs, ufs)

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
                    if node.branched is None and node.skipped is not None:
                        logger.info(f"New branch from node {node.id} (branched)")
                    node.branched = self.add_recursive(node, node.branched, trace, inputs, ufs)
                else:
                    if node.skipped is None and node.branched is not None:
                        logger.info(f"New branch from node {node.id} (skipped)")
                    node.skipped = self.add_recursive(node, node.skipped, trace, inputs, ufs)
            else:
                # Create a new Node or Leaf if the current node is a Leaf
                return Node(parent, trace, inputs, ufs) if len(trace) > 0 else Leaf(parent, inputs, ufs)

        return node
    def get_constraint_label(self, parent: Node, node: Union[Node, Leaf]):
        return None
        """Get the constraint label for an edge."""
        if isinstance(node, Leaf):
            return ""
        if node.trace_id in parent.constraint:
            return str(parent.constraint[node.trace_id])
        return ""

    def add_to_dot(self, node: Optional[Union[Node, Leaf]], graph, parent: Optional[Node] = None):
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

#    def plot_tree(self, idx):
#        return None
#        """Plot the tree using Graphviz and save to a file."""
#        #log.info(self.to_string())
#        G = pgv.AGraph(directed=True, strict=True, rankdir='TB')
#        self.add_to_dot(self.root, G)
#        G.layout(prog="dot")
#        G.draw(f"tree_{idx}.png")
