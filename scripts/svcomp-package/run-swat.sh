#!/bin/bash


# Check if the '-v' argument is among the arguments, indicating that the user wants to see the version
for arg in "$@"
do
  # Check if '-v' is among the arguments
  if [ "$arg" == "-v" ]; then
    # Execute the Java command with base dir of this script
    pushd "$(dirname "$0")" > /dev/null
    java -cp ./symbolic-executor/lib/symbolic-executor.jar de.uzl.its.swat.Version
    popd > /dev/null
    # Exit with status 0
    exit 0
  fi
done

# get last argument
last_path="${@: -1}"

# get second last argument
second_last_path="${*: -2:1}"

# check if there is a Main.java in last_path
if [ -f "$second_last_path/Main.java" ]; then
    # switch the last two arguments
    set -- "${@:1:$(($#-2))}" "$last_path" "$second_last_path"
fi

# The witness is created by run_swat.py directly from the given input paths.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VENV_PYTHON="$SCRIPT_DIR/.venv_ubuntu_24_04_1__x86_64/bin/python3"

if [ ! -x "$VENV_PYTHON" ]; then
    echo "Error: Python runtime not found at $VENV_PYTHON"
    exit 1
fi

echo "Running SWAT with arguments: $@"
"$VENV_PYTHON" -u "$SCRIPT_DIR/run_swat.py" "$@" > out.log 2>&1
cat out.log
