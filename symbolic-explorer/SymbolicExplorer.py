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
from driver.HTTPDriver import HTTPDriver
from driver.SimpleDriver import SimpleDriver

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
        driver = TargetDriver(self.args)
        driver.run()

    def run_passive(self):
        driver = PassiveDriver(args)
        driver.run_dse(True)


    def run_sv_comp(self):
        driver = SVCompDriver(args)
        driver.run()

    def run_http(self):
        driver = HTTPDriver(self.args)
        driver.run()

    def run_simple(self):
        driver = SimpleDriver(self.args)
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
    parser.add_argument("-m", "--mode", choices=['passive', 'annotation', 'args', 'sv-comp', 'http', 'simple'], default='annotation',
                             help="Choose the desired mode")
    parser.add_argument("-t", "--target", help="Full path to the target JAR file")
    parser.add_argument("-prp", "--property", help="Which property to verify")
    parser.add_argument("-s", "--symbolicvars", nargs='+', help="The types and amount of the symbolic "
                                                                     "variables")
    parser.add_argument("-cp", "--classpath", nargs='+', help="List of paths to include in the classpath")
    parser.add_argument("-l", "--logdir", help="Logging directory", default="logs")
    parser.add_argument("-a", "--agent", help="Directory of the agent")
    parser.add_argument("-jp", "--java_path", help="Path to the java executable", default="java")
    parser.add_argument("-c", "--config", help="Directory of the config")
    parser.add_argument("-z3", "--z3dir", help="Directory of the z3 Java Binding")
    parser.add_argument("-p", "--port", help="The port to use for the server", default=8078)
    parser.add_argument("-i", "--ip_addr", help="IP address to Listen on", default="127.0.0.1")
    parser.add_argument("--optimize", action="store_true", help="Enable Z3 optimizer to minimize variable values (slower but may produce simpler solutions)", default=False)
    parser.add_argument("--log-smt-formulas", action="store_true", help="Log SMT formulas and models to disk for debugging", default=False)

    # HTTP-specific arguments
    parser.add_argument("--host", help="HTTP target host", default="localhost")  
    parser.add_argument("--http-port", help="HTTP target port", type=int, default=8080)
    parser.add_argument("-u", "--url-template", help="URL template with symbolic variables (e.g., /api/path/<int_0>?x=<float_1>)")
    parser.add_argument("--http-method", choices=['GET', 'POST', 'PUT', 'DELETE'], default='GET', help="HTTP method")


if __name__ == '__main__':

    # Parse arguments
    parser = argparse.ArgumentParser(description="The symbolic explorer")
    init_args(parser=parser)
    args = parser.parse_args()
    
    # Initialize loggers
    log.initialize_loggers(args.logdir)

    # Set configuration flags in Database
    from data.Database import Database
    Database.instance().optimize_solutions = args.optimize
    Database.instance().log_smt_formulas = args.log_smt_formulas
    Database.instance().args = args  # Store args for access to logdir

    s = SymbolicExplorer(args)

    if args.mode == "annotation" or args.mode == "args":
        thread = threading.Thread(target=s.run_active)
    elif args.mode == "passive":
        thread = threading.Thread(target=s.run_passive)
    elif args.mode == "sv-comp":
        thread = threading.Thread(target=s.run_sv_comp)
    elif args.mode == "http":
        thread = threading.Thread(target=s.run_http)
    elif args.mode == "simple":
        thread = threading.Thread(target=s.run_simple)
    else:
        print("Invalid mode.")
        sys.exit(1)

    thread.start()

    uvicorn.run(s.app, host=args.ip_addr, port=int(args.port))
