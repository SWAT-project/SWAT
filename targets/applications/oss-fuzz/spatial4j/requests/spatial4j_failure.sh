curl -X POST "http://localhost:8080/spatial4j/readShapeFromWkt" \
     -H "Content-Type: text/plain" \
     -d "BUFFER(POINT(30 10), -5)"