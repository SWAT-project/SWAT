#!/usr/bin/env bash

# This script exercises the /newBook endpoint of the provided Spring Boot application.
# It sends a JSON payload representing a BookDto via a POST request.
# Make sure the server is running on localhost:8080 before executing this script.

API_URL="http://localhost:8091/books/newBook"

# Construct the JSON payload.
JSON_PAYLOAD='{
  "title": "My New Book",
  "year": "2024-12-10T00:00:00Z",
  "authorName": "John Green",
  "id": 123
}'

# Use curl to POST the JSON payload to the endpoint.
# The -H flag sets the header to indicate JSON,
# and -d sends the JSON data.
RESPONSE=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -d "$JSON_PAYLOAD" \
  "$API_URL")

# Print the response to stdout.
echo "Response from server:"
echo "$RESPONSE"