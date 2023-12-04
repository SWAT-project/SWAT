#!/bin/bash


# Check if the '-v' argument is among the arguments, indicating that the user wants to see the version
for arg in "$@"
do
  # Check if '-v' is among the arguments
  if [ "$arg" == "-v" ]; then
    # Execute the Java command with base dir of this script
    pushd "$(dirname "$0")" > /dev/null
    java -cp ./knife-fuzzer/symbolic-executor/lib/symbolic-executor.jar de.uzl.its.swat.Version
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

# copy Main.java for Witness creation
last_path="${@: -1}"
file_to_copy="${last_path}/Main.java"

# Check if the file exists
if [ -f "$file_to_copy" ]; then
    # Copy the Main.java file to the current directory
    cp "$file_to_copy" ./WitnessCreator/.
    echo "File copied successfully."
else
    echo "Error: File not found."
fi


source knife-fuzzer/symbolic-explorer/venv/bin/activate
python3 -u run_swat.py "$@" > out.log 2>&1
cat out.log
