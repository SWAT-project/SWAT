import argparse
import sys
import threading
import time
from urllib.request import Request

import uvicorn
from fastapi import FastAPI
from fastapi import Query
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from constraint.ConstraintService import ConstraintService
from parse.DataTransferObjects import ConstraintRequest
from driver.TargetDriver import TargetDriver

sys.setrecursionlimit(10000000)


class SymbolicExplorer:
    parser = argparse.ArgumentParser(description="The symbolic explorer")
    app = FastAPI(debug=True)

    def init_args(self):
        self.parser.add_argument("-m", "--mode", choices=['passive', 'active'], default='passive',
                                 help="Choose the desired mode")
        self.parser.add_argument("-t", "--target", help="Full path to the target JAR file")
        self.parser.add_argument("-s", "--symbolicvars", nargs='+', help="The types and amount of the symbolic "
                                                                         "variables")
        self.parser.add_argument("-cp", "--classpath", nargs='+', help="List of paths to include in the classpath")
        self.parser.add_argument("-l", "--logdir", nargs='+', help="Logging directory")
        self.parser.add_argument("-d", "--basedir", help="Base directory")
        self.parser.add_argument("-a", "--agent", help="Directory of the agent")
        self.parser.add_argument("-c", "--config", help="Directory of the config")
        self.parser.add_argument("-z3", "--z3dir", help="Directory of the z3 Java Binding")

    def run_target(self):
        args = self.parser.parse_args()
        driver = TargetDriver()
        driver.run(args)

    @staticmethod
    @app.post("/constraints/submit")
    async def submit_constraints(request: ConstraintRequest, endpointID: str = Query(...),
                                 traceID: str = Query(...)):
        ConstraintService.add_constraints(endpoint_id=endpointID, trace_id=traceID, trace=request.trace,
                                          inputs=request.inputs)
        return {"message": "Accepted"}

    @staticmethod
    @app.exception_handler(RequestValidationError)
    async def validation_exception_handler(request: Request, exc: RequestValidationError):
        errors = exc.errors()
        details = []
        for error in errors:
            details.append({
                "loc": error.get('loc', []),
                "msg": error.get('msg', ''),
                "type": error.get('type', '')
            })
        response_content = {
            "detail": "Validation Error",
            "errors": details,
            "method": request.method,
            "path": request.url.path,
        }
        print(f"Request validation error: {response_content}")
        return JSONResponse(status_code=422, content=response_content)

    @staticmethod
    @app.exception_handler(StarletteHTTPException)
    async def http_exception_handler(request: Request, exc: StarletteHTTPException):
        response_content = {
            "detail": exc.detail,
            "method": request.method,
            "path": request.url.path,
            "suggestion": "Check the URL and method for correctness."
        }
        print(f"HTTP Exception: {response_content}")
        return JSONResponse(status_code=exc.status_code, content=response_content)


    def init(self):
        args = self.parser.parse_args()
        if args.mode == 'active':
            self.run_svcomp()
        elif args.mode == 'passive':
            pass


if __name__ == '__main__':
    s = SymbolicExplorer()
    s.init_args()

    thread = threading.Thread(target=s.run_target)
    thread.start()

    uvicorn.run(s.app, host="0.0.0.0", port=8078)
