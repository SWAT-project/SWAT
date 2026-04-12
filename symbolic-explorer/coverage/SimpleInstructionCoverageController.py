from fastapi import APIRouter, status, Query
from coverage.SimpleInstructionCoverageService import SimpleInstructionCoverageService
from parse.DataTransferObjects import CoverageRequest
import threading

import log
logger = log.get_logger()


class SimpleInstructionCoverageController:

    def __init__(self):
        self.router = APIRouter()
        self.router.add_api_route("/instr-coverage/submit", self.post, methods=["POST"],
                                  status_code=status.HTTP_202_ACCEPTED)

    def post(self, request: CoverageRequest, endpointID: str = Query(...), traceID: str = Query(...)):
        logger.info(f'Coverage received for endpoint {endpointID} and trace {traceID}')
        visited_ids = request.ids
        total_inst = request.total

        thread = threading.Thread(target=SimpleInstructionCoverageService.add_coverage,
                                  kwargs={'endpoint': endpointID, 'ids': visited_ids, 'total': total_inst})
        thread.start()
        return {"message": "Accepted"}
