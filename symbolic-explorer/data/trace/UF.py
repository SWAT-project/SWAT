class UF:
    """
    Represents the definition of a single UF

    Attributes:
        definition (str): The smt formula that defines the UF
    """

    def __init__(self, definition):
        """
        Initializes a new instance of the UF class.

        Args:
            definition (str): The smt formula that defines the UF.
        """
        self.definition = definition

    def __str__(self):
        return f'Definition: {self.definition}'