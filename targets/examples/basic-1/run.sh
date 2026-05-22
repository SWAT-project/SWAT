#!/bin/bash
# Check if a target is provided, otherwise use Example1.java
target=${1:-Example1}

mode=${2:-local}

# Validate mode
if [ "$mode" != "explorer" ] && [ "$mode" != "local" ]; then
    echo "Invalid mode: $mode. Use 'explorer' or 'local'."
    exit 1
fi

# Check if help is requested
if [ "$1" == "--help" ] || [ "$1" == "-h" ]; then
    echo "Usage: $0 [target] [mode]"
    echo "target: The Java file to compile and run (default: Example1.java)"
    echo "mode:   'explorer' to run with the symbolic explorer, otherwise runs with the symbolic executor"
    exit 0
fi
# Log the file being compiled
echo "Running $target" in $mode mode

# Execute based on the mode
if [ "$mode" == "explorer" ]; then
    # Running the target from the symbolic explorer
    ../../../symbolic-explorer/.venv/bin/python3 ../../../symbolic-explorer/SymbolicExplorer.py \
        --mode simple \
        --target $target \
        --classpath . \
        --agent ../../../symbolic-executor/lib/symbolic-executor.jar \
        --z3dir ../../../libs/java-library-path
else
    # Running the target from the symbolic executor
    java \
    -Djava.library.path=../../../libs/java-library-path \
    -javaagent:../../../symbolic-executor/lib/symbolic-executor.jar \
    $target
fi