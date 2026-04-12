from typing import List
from data.Database import Database
from parse.TraceParser import Parser
from data.trace.Input import Input
from data.trace.UF import UF

import log
logger = log.get_logger()

class ConstraintService:
    """
    A service class for handling constraints-related operations.
    
    This class provides functionalities to interact with the database 
    for constraint-related tasks, such as adding constraints based on
    traces and inputs received from API endpoints.
    """

    @staticmethod
    def add_constraints(endpoint_id, trace_id, trace, inputs, ufs, symbolic_context_loss, symbolic_precision_loss, reference_semantic_change=False):
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
        ufs (dict): Definition of all UFs that are used
        symbolic_context_loss (bool): A flag indicating whether the symbolic context was lost.
        symbolic_precision_loss (bool): A flag indicating whether the symbolic precision was lost (UFs introduced).
        reference_semantic_change (bool): A flag indicating whether reference equality semantics changed.

        Returns:
        None: The result is the side effect of adding data to the database.
        """

        trace = Parser.parse_trace(trace, trace_id=trace_id)
        inputs: List[Input] = Parser.parse_inputs(inputs)
        ufs: List[UF] = Parser.parse_ufs(ufs)
        #logger.info(f'[CONSTRAINT SERVICE] Received: {[t.__str__() for t in trace]}')
        # Adding the trace and inputs to the database for the specified endpoint.
        Database.instance().add_trace(endpoint_id, trace_id, trace, inputs, ufs, symbolic_context_loss, symbolic_precision_loss, reference_semantic_change)
        logger.info(f'[CONSTRAINT SERVICE] Added trace {trace_id} to endpoint {endpoint_id}')
