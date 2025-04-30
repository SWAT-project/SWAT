from fastapi import APIRouter
from fastapi import Query
import threading
from constraint.ConstraintService import ConstraintService
from parse.DataTransferObjects import ConstraintRequest

class ConstraintController:
    """
    API for handling constraints related to an endpoint.
    This controller is responsible for processing POST requests that contain 
    constraints data, such as traces and inputs, related to an endpoint.

    The constraints data is processed in a separate thread to ensure that the answer .
    """

    def __init__(self):
        self.router = APIRouter()
        self.router.add_api_route("/constraints/submit", self.post, methods=["POST"])

    @staticmethod
    def post(request: ConstraintRequest, endpointID: str = Query(...), traceID: str = Query(...)):
        """
        Handles a POST request to add constraints related to an endpoint.

        Extracts 'endpointID' and 'traceID' from the query parameters and 
        the constraints data (trace and inputs) from the request body. 
        It then creates a new thread to process and add these constraints 
        asynchronously.

        Parameters:
        None, but reads from the request object.

        Returns:
        A tuple containing a message indicating that the request is accepted 
        and a status code of 202 (Accepted).
        """

        trace = request.trace
        inputs = request.inputs
        symbolicContextLoss = request.symbolicContextLoss
        symbolicPrecisionLoss = request.symbolicPrecisionLoss

        # Start a new thread to add constraints
        thread = threading.Thread(target=ConstraintService.add_constraints, kwargs={
            'endpoint_id': endpointID,
            'trace_id': traceID,
            'trace': trace,
            'inputs': inputs,
            'symbolic_context_loss': symbolicContextLoss,
            'symbolic_precision_loss': symbolicPrecisionLoss})
        thread.start()
        thread.join() # To ensure trace is added in SV-Comp mode
        # Return a response indicating that the request has been accepted
        return {"message": "Accepted"}, 202
