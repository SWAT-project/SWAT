from typing import List
from data.Database import Database
from parse.TraceParser import Parser
from data.trace.Input import Input
from data.trace.UF import UF

from parse.DataTransferObjects import TraceItem, InputItem, UFItem, InvocationItem
from data.trace.Special import Special
from data.trace.Branch import Branch

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
    def add_constraints(endpoint_id: str, trace_id: str, trace: List[TraceItem], inputs: List[InputItem], ufs: List[UFItem],
                        symbolic_context_loss: bool, symbolic_precision_loss: bool,
                        reference_semantic_change: bool = False,
                        missing_invocations: List[InvocationItem] = None):
        """
        Adds constraints to the database.

        This method takes various parameters from the keyword arguments,
        processes them, and then adds the trace and input data to the
        database for a given endpoint.

        Parameters:
        endpoint_id (str): The ID of the endpoint.
        trace_id (str): The ID of the trace.
        trace (list): The trace data.
        inputs (list): The input data associated with the trace.
        ufs (list): Definition of all UFs that are used
        symbolic_context_loss (bool): A flag indicating whether the symbolic context was lost.
        symbolic_precision_loss (bool): A flag indicating whether the symbolic precision was lost (UFs introduced).
        reference_semantic_change (bool): A flag indicating whether reference equality semantics changed.
        missing_invocations (list): Methods that could not be modelled symbolically during this trace.

        Returns:
        None: The result is the side effect of adding data to the database.
        """

        # logger.info(f'[CONSTRAINT SERVICE] Received trace: {[t.__str__() for t in trace]}')
        trace_parsed: List[Branch | Special] = Parser.parse_trace(trace, trace_id=trace_id)
        # logger.info(f'[CONSTRAINT SERVICE] Parsed trace: {[t.__str__() for t in trace_parsed]}')
        inputs_parsed: List[Input] = Parser.parse_inputs(inputs)
        # logger.info(f'[CONSTRAINT SERVICE] Parsed inputs: {[i.__str__() for i in inputs_parsed]}')
        ufs_parsed: List[UF] = Parser.parse_ufs(ufs)
        # Flatten the missing-invocation DTOs into plain dicts so the Database/Tree stay decoupled
        # from the pydantic request models.
        missing_invocations_data = [item.model_dump() for item in (missing_invocations or [])]
        # Adding the trace and inputs to the database for the specified endpoint.
        Database.instance().add_trace(endpoint_id, trace_id, trace_parsed, inputs_parsed, ufs_parsed, symbolic_context_loss, symbolic_precision_loss, reference_semantic_change, missing_invocations_data)
        logger.info(f'[CONSTRAINT SERVICE] Added trace {trace_id} to endpoint {endpoint_id}')
