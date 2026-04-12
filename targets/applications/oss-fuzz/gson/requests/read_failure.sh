#!/bin/bash

curl -X POST "http://localhost:8080/gson/read?lenient=false" \
     -H "Content-Type: application/json" \
     -d '{"name": "Alice", "age": }'