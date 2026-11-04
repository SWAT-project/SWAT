#!/bin/bash

cd "$(dirname "$0")"
rm -rf witness
output="$("bash" "run-swat.sh" "../../sv-benchmarks/java/properties/valid-assert.prp" "smoketest/common" "smoketest")"
if ! echo "$output" | grep -q "\[VERDICT valid-assert.prp\] == ERROR"; then
    echo "Smoketest failed: no violation verdict!"
    exit 1
fi
# The violation must come with a YAML witness (format 2.3) with a target waypoint, written by
# the explorer to witness/witness.yml
if ! grep -q 'entry_type: "violation_sequence"' witness/witness.yml 2>/dev/null \
    || ! grep -q 'type: "target"' witness/witness.yml; then
    echo "Smoketest failed: no violation witness with a target waypoint in witness/witness.yml!"
    exit 1
fi
echo "Smoketest passed!"
exit 0

