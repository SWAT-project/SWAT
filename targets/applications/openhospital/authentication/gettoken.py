#!/usr/bin/python3

import http.client
import json

print("{u'app1': {}}")

conn = http.client.HTTPConnection("localhost:8080")

payload = "{\"username\": \"admin\", \"password\": \"admin\"}"

headers = {
'Content-Type': "application/json"
}

conn.request("POST", "/auth/login", payload, headers)

res = conn.getresponse()
data = res.read()

jsonData = json.loads(data.decode("utf-8"))
print("Authorization: Bearer % s" % jsonData['token'])
