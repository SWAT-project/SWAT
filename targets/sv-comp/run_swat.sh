#!/bin/bash

# Path to the args.txt file
ARGS_FILE="args.txt"

# Check if the args.txt file exists
if [ ! -f "$ARGS_FILE" ]; then
    echo "File $ARGS_FILE does not exist!"
    exit 1
fi

# Read the args.txt file line by line
while IFS= read -r line; do
    # Execute the python script with the arguments from the line
    /opt/homebrew/bin/PYTHON3 /Users/nils/dev/knife-fuzzer/symbolic-explorer/service-manager/symbolic-explorer.py -cp $line
done < "$ARGS_FILE"
