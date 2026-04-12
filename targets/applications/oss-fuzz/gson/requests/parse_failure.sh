#!/bin/bash

curl -X POST "http://localhost:8080/gson/parse" \
     -H "Content-Type: application/json" \
     -d '{"city": Berlin}'