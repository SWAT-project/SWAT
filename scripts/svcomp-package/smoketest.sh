#!/bin/bash

cd "$(dirname "$0")"
output="$("bash" "run-swat.sh" "../../sv-benchmarks/java/properties/valid-assert.prp" "smoketest/common" "smoketest")"
if echo "$output" | grep -q "\[VERDICT valid-assert.prp\] == ERROR"; then
    echo "Smoketest passed!"
    exit 0
else
    echo "Smoketest failed!"
    exit 1
fi

