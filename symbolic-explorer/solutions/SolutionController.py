from fastapi import APIRouter, status, Response

from solutions.SolutionService import SolutionService


class SolutionController:

    def __init__(self):
        self.router = APIRouter()
        self.router.add_api_route("/solution/{quantity}", self.get, methods=["GET"], status_code=status.HTTP_200_OK)
        self.solution_service = SolutionService()

    # Gets the next solution
    def get(self, quantity, response: Response):
        if quantity == 'all':
            return self.solution_service.get_all_solutions()
        elif quantity == 'one':
            next_solution = self.solution_service.get_next_solution()
            return next_solution
        else:
            response.status_code = status.HTTP_400_BAD_REQUEST
            return {'message': 'Quantity: either "all" or "one".'}

