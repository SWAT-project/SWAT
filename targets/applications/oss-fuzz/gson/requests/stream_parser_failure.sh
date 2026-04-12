#!/bin/bash

curl -X POST "http://localhost:8080/gson/streamParser" \
     -H "Content-Type: application/json" \
     -d '[{"id":1}, {"id":2,]'