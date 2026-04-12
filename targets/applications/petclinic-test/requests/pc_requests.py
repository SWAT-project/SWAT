import requests
import argparse
import json

def new_pet(params):
    return


def new_owner(params):
    global host

    if len(params) == 3:
        path = "/owners/new"
        url = host + path
        headers = {'Content-Type': "application/json",}

        response = requests.post(url,
                                 data={
                                     "address": params[0],
                                     "city": params[1],
                                     "telephone": params[2]},
                                 headers=headers)

        print(response.text)
    else:
        print("Invalid parameter length")


def get_owners(params):
    path = "/owners"
    url = host + path
    headers = {'Content-Type': "application/json",}

    response = requests.get(url,
                             headers=headers)

    print(response.text)


def new_visit(params):
    return


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="Some requests for pet-clinic")
    parser.add_argument("-r", "--request", choices=['new_pet', 'new_owner', 'get_owners', 'new_visit'],
                        default='new_owner',
                        help="Choose the desired request")
    parser.add_argument("-p", "--params", nargs="*", help="Request parameters")
    args = parser.parse_args()

    host = "http://localhost:8080"
    rqs = {
        "new_pet": new_pet,
        "new_owner": new_owner,
        "get_owners": get_owners,
        "new_visit": new_visit
    }

    params = []
    if args.params is not None:
        params = args.params

    if args.request in rqs:
        rqs[args.request](params)