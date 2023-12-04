from data.Database import Database


class ConstraintService:
    """
    A service class for handling constraints-related operations.
    
    This class provides functionalities to interact with the database 
    for constraint-related tasks, such as adding constraints based on
    traces and inputs received from API endpoints.
    """

    @staticmethod
    def add_constraints(endpoint_id, trace_id, trace, inputs):
        """
        Adds constraints to the database.

        This method takes various parameters from the keyword arguments,
        processes them, and then adds the trace and input data to the
        database for a given endpoint.

        Parameters:
        endpoint_id (str): The ID of the endpoint.
        trace_id (str): The ID of the trace.
        trace (dict): The trace data.
        inputs (dict): The input data associated with the trace.

        Returns:
        None: The result is the side effect of adding data to the database.
        """
        
        
        # Adding the trace and inputs to the database for the specified endpoint.
        Database.instance().add_trace(endpoint_id, trace_id, trace, inputs)
