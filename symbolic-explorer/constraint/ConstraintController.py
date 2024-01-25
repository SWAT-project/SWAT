from flask_restful import Resource
from flask import request
import threading
from constraint.ConstraintService import ConstraintService
from parse.TraceParser import parse_trace, parse_inputs
from log import request_logger as logger

class ConstraintController(Resource):
    """
    A Flask-Restful resource for handling constraints related to an endpoint.
    This controller is responsible for processing POST requests that contain 
    constraints data, such as traces and inputs, related to an endpoint.

    The constraints data is processed in a separate thread to avoid blocking 
    the main execution thread of the Flask application.
    """

    def post(self):
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

        # Retrieve endpoint ID and trace ID from query parameters
        endpoint_id = request.args.get('endpointID')
        trace_id = request.args.get('traceID')

        # Retrieve constraints data from the request body
        content = request.json

        # Parse the trace and inputs from the constraints data
        trace = parse_trace(content['trace'], trace_id=trace_id)
        inputs = parse_inputs(content['inputs'])
       
        # Start a new thread to add constraints
        thread = threading.Thread(target=ConstraintService.add_constraints, kwargs={
            'endpoint_id': endpoint_id,
            'trace_id': trace_id,
            'trace': trace,
            'inputs': inputs})
        thread.start()

        # Return a response indicating that the request has been accepted
        return {"message": "Accepted"}, 202
