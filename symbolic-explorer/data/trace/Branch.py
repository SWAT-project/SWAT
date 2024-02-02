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

    def __init__(self, id, trace_id, has_branched, constraint):
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
