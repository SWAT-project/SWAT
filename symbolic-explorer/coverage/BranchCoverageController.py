from fastapi import APIRouter, status, Query
import threading

from coverage.BranchCoverageService import BranchCoverageService
from parse.DataTransferObjects import CoverageRequest

import log
logger = log.get_logger()


class BranchCoverageController:

    def __init__(self):
        self.router = APIRouter()
        self.router.add_api_route("/branch-coverage/submit", self.post, methods=["POST"],
                                  status_code=status.HTTP_202_ACCEPTED)

    def post(self, request: CoverageRequest, endpointID: str = Query(...), traceID: str = Query(...)):
        logger.info(f'Coverage received for endpoint {endpointID} and trace {traceID}')
        visited_ids = request.ids
        total_branch_inst = request.total
        thread = threading.Thread(target=BranchCoverageService.add_coverage,
                                  kwargs={'ids': visited_ids, 'total': total_branch_inst})
        thread.start()
        return {"message": "Accepted"}
