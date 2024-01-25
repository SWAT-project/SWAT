class Solution:
    """
    Represents a symbolic solution. 

    Attributes:
        solution (any): The solution computed by the solver for a specific path.
        inputs (any): The concrete inputs that were used to reach the new branching points
    """

    def __init__(self, sol=None, inputs=None):
        """
        Initializes a new instance of the Solution class.

        Args:
            solution (any): The solution computed by the solver for a specific path.
            inputs (any): The concrete inputs that were used to reach the new branching points
        """
        self.solution = sol
        self.inputs = inputs

    def get_solution(self):
        """
        Retrieves the solution.

        Returns:
            any: The solution stored in this instance.
        """
        return self.solution

    def get_input(self):
        """
        Retrieves the inputs associated with the solution.

        Returns:
            any: The inputs used to generate the solution.
        """
        return self.inputs
