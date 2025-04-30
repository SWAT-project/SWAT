import argparse
import sys
import threading
from urllib.request import Request
import logging
import uvicorn
from fastapi import FastAPI
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

import log
from constraint.ConstraintController import ConstraintController
from solutions.SolutionController import SolutionController
from coverage.BranchCoverageController import BranchCoverageController
from coverage.SimpleInstructionCoverageController import SimpleInstructionCoverageController
from endpoints.EndpointController import EndpointController

from driver.TargetDriver import TargetDriver
from driver.PassiveDriver import PassiveDriver
from driver.SVCompDriver import SVCompDriver

sys.setrecursionlimit(10000000)
logging.basicConfig(level=logging.DEBUG)

class SymbolicExplorer:

    app = FastAPI(debug=True)

    def __init__(self, args):
        self.args = args

        self.constraint_controller = ConstraintController()
        self.app.include_router(self.constraint_controller.router)

        self.solution_controller = SolutionController()
        self.app.include_router(self.solution_controller.router)

        self.branch_coverage_controller = BranchCoverageController()
        self.app.include_router(self.branch_coverage_controller.router)

        self.instr_coverage_controller = SimpleInstructionCoverageController()
        self.app.include_router(self.instr_coverage_controller.router)

        self.endpoint_controller = EndpointController()
        self.app.include_router(self.endpoint_controller.router)


    def run_active(self):
        driver = TargetDriver()
        driver.run(self.args)

    def run_passive(self):
        driver = PassiveDriver(args)
        driver.run_dse(True)


    def run_sv_comp(self):
        driver = SVCompDriver(args)
        driver.run()

    @staticmethod
    @app.get("/health")
    async def health():
        return {"message": "An apple a day ..."}

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

    # def init(self):
    #     args = self.parser.parse_args()
    #     if args.mode == 'active':
    #         self.run_svcomp()
    #     elif args.mode == 'passive':
    #         pass


def init_args(parser):
    parser.add_argument("-m", "--mode", choices=['passive', 'active', 'sv-comp'], default='passive',
                             help="Choose the desired mode")
    parser.add_argument("-t", "--target", help="Full path to the target JAR file")
    parser.add_argument("-s", "--symbolicvars", nargs='+', help="The types and amount of the symbolic "
                                                                     "variables")
    parser.add_argument("-cp", "--classpath", nargs='+', help="List of paths to include in the classpath")
    parser.add_argument("-l", "--logdir", help="Logging directory")
    parser.add_argument("-a", "--agent", help="Directory of the agent")
    parser.add_argument("-jp", "--java_path", help="Path to the java executable", default="java")
    parser.add_argument("-c", "--config", help="Directory of the config")
    parser.add_argument("-z3", "--z3dir", help="Directory of the z3 Java Binding")
    parser.add_argument("-p", "--port", help="The port to use for the server", default=8078)


if __name__ == '__main__':

    # Parse arguments
    parser = argparse.ArgumentParser(description="The symbolic explorer")
    init_args(parser=parser)
    args = parser.parse_args()
    
    # Initialize loggers
    log.initialize_loggers(args.logdir)

    s = SymbolicExplorer(args)

    if args.mode == "active":
        thread = threading.Thread(target=s.run_active)
    elif args.mode == "passive":
        thread = threading.Thread(target=s.run_passive)
    elif args.mode == "sv-comp":
        thread = threading.Thread(target=s.run_sv_comp)
    else:
        print("Invalid mode.")
        sys.exit(1)

    thread.start()

    uvicorn.run(s.app, host="0.0.0.0", port=int(args.port))
