#!/bin/bash

# Running the target from the symbolic explorer
./../../../symbolic-explorer/.venv/bin/python3 ../../../symbolic-explorer/SymbolicExplorer.py \
    --mode annotation \
    --target VirtualDispatch \
    --classpath build \
    --agent ../../../symbolic-executor/lib/symbolic-executor.jar \
    --z3dir ../../../libs/java-library-path \
    --config swat.cfg \
    "$@" # additional args are passed through
