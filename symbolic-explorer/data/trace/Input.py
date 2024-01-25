class Input:
    """
    Represents a symbolic input with its concrete value from a trace.

    Attributes:
        name (str): The symbolic name of the input.
        value (any): The concrete value of the input.
        type (str): The data type of the input parameter.
        lower_bound (any): The (symbolic) lower bound for the value of the input parameter, if applicable.
        upper_bound (any): The (symbolic) upper bound for the value of the input parameter, if applicable.
    """

    def __init__(self, name, value, type, lower_bound, upper_bound):
        """
        Initializes a new instance of the Input class.

        Args:
            name (str): The symbolic name of the input.
            value (any): The concrete value of the input.
            type (str): The data type of the input parameter.
            lower_bound (any): The (symbolic) lower bound for the value of the input parameter, if applicable.
            upper_bound (any): The (symbolic) upper bound for the value of the input parameter, if applicable.
        """
        self.name = name
        self.value = value
        self.type = type
        self.lower_bound = lower_bound
        self.upper_bound = upper_bound
