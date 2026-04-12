#!/bin/bash
# Check if an argument is provided, otherwise use Example1.java
target=${1:-Example1.java}

# Log the file being compiled
echo "Compiling $target"

# Compile the specified target file
javac -cp ../../../annotations/build/libs/annotations-1.0.jar $target