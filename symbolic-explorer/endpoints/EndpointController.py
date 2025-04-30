from fastapi import APIRouter, status

from data.Database import Database


class EndpointController:

    def __init__(self):
        self.router = APIRouter()
        self.router.add_api_route("/endpoints", self.get, methods=["GET"], status_code=status.HTTP_200_OK)

    def get(self):
        return Database.instance().get_endpoints()
