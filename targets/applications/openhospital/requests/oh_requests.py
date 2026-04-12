import random

import requests
import argparse
import json
import random

host = "http://localhost:8080"

def login():
    path = "/auth/login"
    url = host + path
    payload = {"username": "admin", "password": "admin"}
    headers = {'Content-Type': "application/json"}

    response = requests.post(url, json=payload, headers=headers)

    return response.json()["token"]
    # jsonData = json.loads(data.decode("utf-8"))
    # print("Authorization: Bearer % s" % jsonData['token'])


def get_patients_by_code(bearer_token, param_1):
    path = "/patients/" + param_1
    url = host + path
    headers = {'Content-Type': "application/json", "Authorization": "Bearer " + bearer_token}

    response = requests.get(url, headers=headers)
    print(response.text)


def get_patients(bearer_token):
    path = "/patients"
    url = host + path
    headers = {'Content-Type': "application/json", "Authorization": "Bearer " + bearer_token}

    response = requests.get(url, headers=headers)
    print(response.json())


def add_patient(bearer_token):
    path = "/patients"
    url = host + path
    headers = {'Content-Type': "application/json", "Authorization": "Bearer " + bearer_token}
    nonce = random.randrange(0, 4294967296)
    payload = {
        "name": "M_" + str(nonce) + " " + "Muster_" + str(nonce),
        "firstName": "M_" + str(nonce),
        "secondName": "Muster_" + str(nonce),
        "age": random.randrange(1, 90),
        "agetype": "biological",
        "birthDate": f"{random.randrange(1900, 2024):d}-{random.randrange(1,12):02d}-{random.randrange(1, 31):02d}",
        "motherName": "MN_" + str(nonce),
        "fatherName": "FN_" + str(nonce),
        "sex": random.choice(("M", "F")),
        "city": random.choice(("Berlin", "Leipzig", "Stuttgart")),
        "bloodType": random.choice(("0-", "0+", "A-", "A+", "B-", "B+", "AB-", "AB+"))
    }
    print(payload)

    response = requests.post(url, json=payload, headers=headers)
    print(response.status_code)
    print(response.json())


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="Some requests for openhospital")
    parser.add_argument("-r", "--request", choices=['login', 'getPatientsByCode', 'getPatients', 'addPatient'],
                        default='getPatientsByCode',
                        help="Choose the desired request")
    parser.add_argument("-p1", "--param_1", type=str, help="First parameter for the request")
    args = parser.parse_args()

    bearer_token = login()

    if args.request != 'login':
        if args.request == 'getPatientsByCode':
            get_patients_by_code(bearer_token, args.param_1)
        elif args.request == 'getPatients':
            get_patients(bearer_token)
        elif args.request == 'addPatient':
            add_patient(bearer_token)
        else:
            print("Request not implemented")

