#!/bin/bash

curl -X POST "http://localhost:8080/java-diff-utils/diffInline" \
     -H "Content-Type: application/json" \
     -d '{
           "doc1": "public class Hello { public static void main(String[] args) { System.out.println(\"Hello World\"); } }",
           "doc2": "public class Hello { public static void main(String[] args) { System.out.println(\"Hello, World!\"); } }"
         }'
