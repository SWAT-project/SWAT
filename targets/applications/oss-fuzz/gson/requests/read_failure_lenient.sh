#!/bin/bash

curl -X POST "http://localhost:8080/gson/read?lenient=true" \
     -H "Content-Type: application/json" \
     -d "{name:'Alice', age:}"