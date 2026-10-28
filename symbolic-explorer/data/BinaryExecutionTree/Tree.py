from typing import List, Optional, Set, Union
from data.trace.Branch import Branch
from data.trace.Special import Special, UNMODELLED_EXCEPTION
from data.BinaryExecutionTree.Leaf import Leaf
from data.BinaryExecutionTree.Node import Node
from data.trace.Input import Input
from data.trace.UF import UF

import log
logger = log.get_logger()

class Tree:
    # Whether dfs() leaves out the branches below a repeated loop-head state (--no-loop-state-matching).
    LOOP_STATE_MATCHING = True

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
        # Traces that used an approximate model (TraceDTO.approximateModel). A solution built from one
        # is a guess, so the next run may take another path than predicted without anything being wrong.
        self.approximate_traces: Set[str] = set()
        # Traces whose execution had a StackOverflowError reach the target's code: their outcome
        # depends on the JVM's stack size, so a violation they show is not a verdict.
        self.stack_overflow_traces: Set[str] = set()
        # How often the next run did take another path than such a guessed solution predicted.
        self.expected_divergences: int = 0
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
        # Loop-head state matching: for each state hash, the smallest position at which an
        # executed path reached it (see register_loop_states).
        self.loop_state_first: dict[str, tuple] = {}
        # Branches the last dfs() left out because of a repeated loop-head state.
        self.loop_state_pruned = 0


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
        
    @staticmethod
    def structural(trace: list) -> list[tuple]:
        """
        The elements of a trace that are levels of the tree, each with whether an
        UNMODELLED_EXCEPTION marker came right before it.

        The marker is only in the trace of a run that throws, so it cannot be a level of its own:
        a run that does not throw has some other element at that depth. It is kept on the node
        after it instead, for the SA walk (see dfs).
        """
        elements = []
        marked = False
        for element in trace:
            if getattr(element, 'inst', None) == UNMODELLED_EXCEPTION:
                marked = True
                continue
            elements.append((element, marked))
            marked = False
        return elements

    def add(self, trace: list[Branch | Special], inputs: List[Input], ufs: List[UF]) -> bool:
        """
        Adds a trace to the tree: follows its path as far as the tree has it already, and appends
        the rest as a new path.

        If the trace has another element than the tree at some depth, the two runs did the same
        up to there and then differently without a branch to tell them apart. The tree keeps what
        it has, the rest of the trace is dropped, and a 'tree_mismatch' execution error records
        that the search is no longer complete.

        Args:
            trace (list): A list of trace elements defining the execution path.
            inputs (any): The inputs associated with the branch being added.
            ufs (any): The UFs associated with the branch being added.

        Returns:
            bool: Whether the whole trace fitted the tree.
        """
        self.register_loop_states(trace)
        elements = self.structural(trace)
        parent: Optional[Node] = None
        branched = False  # the side of parent that node is on
        node = self.root
        i = 0
        while i < len(elements) and isinstance(node, Node):
            element, marked = elements[i]
            if node.id != element.id or (node.kind == "Special") != isinstance(element, Special):
                message = f'depth {i}: tree has {node.kind} {node.id}, trace {element}'
                logger.warning(f"Trace {getattr(element, 'trace_id', '?')} does not fit the tree ({message}), dropping its rest")
                self.record_execution_error('tree_mismatch', message)
                return False
            if marked:
                node.unmodelled_exception = True
            if isinstance(element, Branch):
                node.constraint[element.trace_id] = element.constraint
            if element.has_branched:
                if node.branched is None and node.skipped is not None:
                    logger.info(f"New branch from node {node.id} (branched)")
            else:
                if node.skipped is None and node.branched is not None:
                    logger.info(f"New branch from node {node.id} (skipped)")
            parent, branched = node, element.has_branched
            node = node.branched if branched else node.skipped
            i += 1

        if i == len(elements) and node is not None:
            return True  # the path is in the tree already

        # node is None or a Leaf: the rest of the trace is a new path, ending in a Leaf
        for element, marked in elements[i:]:
            new = Node(parent, element, inputs, ufs)
            new.unmodelled_exception = marked
            self._attach(parent, branched, new)
            parent, branched = new, element.has_branched
        self._attach(parent, branched, Leaf(parent, inputs, ufs))
        return True

    def register_loop_states(self, trace: list[Branch | Special]):
        """
        Positions the loop-head states of a trace and records the smallest position of each.

        The position of a state is (d, k, m): d branches precede it, k is the outcomes of those
        branches read as a binary number (taken = 1, the first branch the most significant bit), and
        m states precede it. Positions compare lexicographically. The state goes with the branch
        that follows it: that branch, and every branch below it, needs no flip if the same state was
        reached at a smaller position (see dominated_by_loop_state and the executor's
        LoopStateMatcher for why). Two paths through the same tree node have the same prefix, so
        the node's positions do not depend on the trace that created it.
        """
        d, k, m = 0, 0, 0
        for element in trace:
            if not isinstance(element, Branch):
                continue
            states = getattr(element, 'loop_states', ())
            if states:
                positioned = []
                for state in states:
                    position = (d, k, m)
                    m += 1
                    positioned.append((state, position))
                    first = self.loop_state_first.get(state)
                    if first is None or position < first:
                        self.loop_state_first[state] = position
                element.positioned_loop_states = tuple(positioned)
            d += 1
            k = 2 * k + (1 if element.has_branched else 0)

    def dominated_by_loop_state(self, node: Node) -> bool:
        """Whether a state reached before this branch was reached at a smaller position already."""
        if not Tree.LOOP_STATE_MATCHING:
            return False
        for state, position in node.loop_states:
            if self.loop_state_first.get(state, position) < position:
                return True
        return False

    def _attach(self, parent: Optional[Node], branched: bool, child: Union[Node, Leaf]):
        """Makes child the root, or the given side of parent."""
        if parent is None:
            self.root = child
        elif branched:
            parent.branched = child
        else:
            parent.skipped = child

    def __deepcopy__(self, memo):
        """
        Copies the tree without recursing along its paths, which can be longer than the
        interpreter's recursion limit allows.
        """
        import copy
        cls = self.__class__
        result = cls.__new__(cls)
        memo[id(self)] = result
        # Copy the nodes shallowly first, so that copying their other attributes below does not
        # descend into the tree.
        nodes = []
        work = [self.root]
        while work:
            node = work.pop()
            if node is None or id(node) in memo:
                continue
            memo[id(node)] = copy.copy(node)
            nodes.append(node)
            if isinstance(node, Node):
                work.append(node.skipped)
                work.append(node.branched)
        for node in nodes:
            new = memo[id(node)]
            for name, value in node.__dict__.items():
                new.__dict__[name] = copy.deepcopy(value, memo)
        for name, value in self.__dict__.items():
            result.__dict__[name] = copy.deepcopy(value, memo)
        return result

# VISUALIZATIONS
    def get_constraint_label(self, parent: Node, node: Union[Node, Leaf]):
        """Get the constraint label for an edge."""
        if isinstance(node, Leaf):
            return ""
        if node.trace_id in parent.constraint:
            return str(parent.constraint[node.trace_id])
        return ""

    def add_to_dot(self, node: Optional[Union[Node, Leaf]], graph, parent: Optional[Node] = None):
        """Add the nodes and edges below node to the DOT graph."""
        work = [(node, parent)]
        while work:
            node, parent = work.pop()
            if node is None:
                continue
            graph.add_node(node.gid, label=str(node.gid) + ':' + str(node.id))
            if parent is not None:
                # Add an edge with constraint as a label
                constraint_label = self.get_constraint_label(parent, node)
                graph.add_edge(parent.gid, node.gid, label=constraint_label)

            if isinstance(node, Node):
                work.append((node.skipped, node))
                work.append((node.branched, node))

    def plot_tree(self, idx):
       """Plot the tree using Graphviz and save to a file."""
       #log.info(self.to_string())
       import pygraphviz as pgv
       G = pgv.AGraph(directed=True, strict=True, rankdir='TB')
       self.add_to_dot(self.root, G)
       G.layout(prog="dot")
       G.draw(f"tree_{idx}.png")
