from constraint.ConstraintService import ConstraintService
from parse.DataTransferObjects import ConstraintRequest
from parse.TraceParser import parse_inputs
from parse.TraceParser import parse_trace
from fastapi import FastAPI, BackgroundTasks, HTTPException, Query
from pydantic import BaseModel
import uvicorn
import threading
import sys
import argparse
from svcomp.SVCompHandler import SVCompHandler


from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from starlette.exceptions import HTTPException as StarletteHTTPException

from fastapi import FastAPI, BackgroundTasks

sys.setrecursionlimit(10000000)
parser = argparse.ArgumentParser(description="The symbolic explorer")






class DSEService:

    @staticmethod
    def init_args():
        
        parser.add_argument("-m", "--mode", choices=['standard', 'sv-comp'], default='standard', help="Choose the desired mode")
        parser.add_argument("-cp", "--classpath", nargs='+', help="List of paths to include in the classpath")
        parser.add_argument("-d", "--basedir",  help="Base directory")
        parser.add_argument("-a", "--agent",  help="Directory of the agent")
        parser.add_argument("-c", "--config",  help="Directory of the config")
        parser.add_argument("-z3", "--z3dir",  help="Directory of the z3 Java Binding")

            
    
    @staticmethod
    def run_svcomp():
        args = parser.parse_args()
        svcomp_handler = SVCompHandler()
        svcomp_handler.run(args.basedir, args.classpath, args.agent, args.config, args.z3dir)

    
def create_app():
    app = FastAPI(debug=True)
    return app

app = create_app()




def add_constraints_to_service(endpoint_id, trace_id, trace, inputs):
    trace = parse_trace(trace, trace_id=trace_id)
    inputs = parse_inputs(inputs)
    ConstraintService.add_constraints(endpoint_id=endpoint_id, trace_id=trace_id, trace=trace, inputs=inputs)

#@app.post("/constraints/submit")
#async def submit_constraints(background_tasks: BackgroundTasks, request: ConstraintRequest, endpointID: str = Query(...), traceID: str = Query(...)):
#    background_tasks.add_task(add_constraints_to_service, endpointID, traceID, request.trace, request.inputs)
#    return {"message": "Accepted"}
@app.post("/constraints/submit")
async def submit_constraints(request: ConstraintRequest, endpointID: str = Query(...), traceID: str = Query(...)):
    add_constraints_to_service(endpointID, traceID, request.trace, request.inputs)
    return {"message": "Accepted"}


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request, exc):
    print(f"Request validation error: {exc.errors()}")
    return JSONResponse(status_code=422, content={"detail": exc.errors()})

@app.exception_handler(StarletteHTTPException)
async def http_exception_handler(request, exc):
    print(f"HTTP Exception: {exc.detail}")
    return JSONResponse(status_code=exc.status_code, content={"detail": exc.detail})

if __name__ == '__main__':

    DSEService.init_args()


    thread = threading.Thread(target=DSEService.run_svcomp)
    thread.start()

    uvicorn.run(app, host="0.0.0.0", port=8078)
