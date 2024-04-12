import argparse
import sys
import threading
import time
from contextlib import asynccontextmanager
from urllib.request import Request

import uvicorn
import fastapi, os
from fastapi import FastAPI
from fastapi import Query
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from config.config_manager import ConfigManager
from config.config_loader import load_config_from_file
from constraint.ConstraintService import ConstraintService
from parse.DataTransferObjects import ConstraintRequest
from driver.TargetDriver import TargetDriver
from log import logger
import asyncio

sys.setrecursionlimit(10000000)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting the server...")
    # load the file path from environment variable
    config_path = os.getenv("swat.config.path")
    config_data = load_config_from_file(config_path)
    app.state.config_manager = ConfigManager(config_path, config_data)
    # Create and start the thread, passing the shutdown event to the TargetDriver
    #thread = threading.Thread(target=run_target, daemon=True)
    #thread.start()
    task = asyncio.create_task(asyncio.to_thread(run_target))
    app.state.background_task = task  # Store the task reference in the application state
    logger.info("Server startup complete!")
    yield
    logger.info("Stopping the server...")
    task = app.state.background_task  # Retrieve the task reference
    task.cancel()  # Request cancellation of the task

    try:
        await task  # Wait for the task to be cancelled
    except asyncio.CancelledError:
        pass  # Task was cancelled, so we can safely ignore the CancelledError
    logger.info("Server shutdown complete!")


parser = argparse.ArgumentParser(description="The symbolic explorer")
app = FastAPI(lifespan=lifespan, debug=True)



@app.post("/constraints/submit")
async def submit_constraints(request: ConstraintRequest, endpointID: str = Query(...),
                             traceID: str = Query(...)):
    ConstraintService.add_constraints(endpoint_id=endpointID, trace_id=traceID, trace=request.trace,
                                      inputs=request.inputs)
    return {"message": "Accepted"}


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


def run_target():
    time.sleep(5)
    driver = TargetDriver(app.state.config_manager)
    target_path = os.getenv("swat.target.path")
    driver.run(target_path)
    logger.info("Target driver has finished")

