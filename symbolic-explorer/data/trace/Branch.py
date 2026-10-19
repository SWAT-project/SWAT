class Branch:
    """
    Represents a branch in an execution trace, capturing the details of a decision point.

    This class is used to encapsulate information about a specific point in the execution flow 
    where a branching decision occurs. It includes details such as the branch's identifier, 
    trace identifier, whether the branch was taken, and any associated constraints.

    Attributes:
        id (any): The identifier of the branch. This can be used to distinguish between different branches.
        trace_id (any): An identifier associated with the execution trace of the branch.
        has_branched (bool): A flag indicating whether this branch was taken in the execution flow.
        constraint (any): The constraint or condition associated with the branch.
    """

    def __init__(self, id: int, trace_id: str, has_branched: bool, constraint: str, concrete_only: bool = False,
                 key: str | None = None, pops: int = 0, pushes: tuple = (), callback: bool = False, choice: bool = False,
                 loop_states: tuple = ()):
        """
        Initializes a new instance of the Branch class.

        Args:
            id (any): The identifier of the branch.
            trace_id (any): An identifier associated with the execution trace of the branch.
            has_branched (bool): A flag indicating whether this branch was taken.
            constraint (any): The constraint or condition associated with the branch.
        """
        self.id = id
        self.trace_id = trace_id
        self.has_branched = has_branched
        self.constraint = constraint
        # Recorded only to keep the SA walk in step (e.g. the bounds check of an untracked array):
        # it has no symbolic constraint and is never worth solving.
        self.concrete_only = concrete_only
        # Where the branch is, to check the SA walk against (see TraceItem).
        self.key = key
        self.pops = pops
        self.pushes = pushes
        # In application code that library code called back, which the SA graph does not have.
        self.callback = callback
        # Added by the executor to enumerate the values of a symbolic operand; not in the SA graph.
        self.choice = choice
        # The loop-head states reached since the previous branch (see Tree.register_loop_states).
        self.loop_states = loop_states
        
    def __str__(self):
        return f'[(B) - {self.id} - ({"T" if self.has_branched else "F"})]'