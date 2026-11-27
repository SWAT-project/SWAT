#!/bin/bash
# Runs swat-sa-showcase once per configuration, one run after the other, and prints the run dirs.
# Usage: ./run_configs.sh [svcomp test run options...]   e.g. --suite summaries
# Env: CONFIGS (default "sa nosa"), TIMEOUT (default 300), WORKERS (default 14)
#   sa       everything on: the default configuration plus SA pruning under no-runtime-exception
#            (--sa-no-runtime-exception, opt-in in the harness); every configuration but nosa has it
#   nosa     no SA feature at all: no graph (pruning, slicing), no loop summaries, no loop-state
#            matching, no property pre-check (see "Runs without SA" in SWAT/CLAUDE.md)
#   noprecheck  everything but the property pre-check
#   noslice  the graph for pruning, without slicing
#   nosum    everything but loop summaries
#   nostate  everything but loop-state matching
set -euo pipefail
SV_COMP="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$SV_COMP"
if pgrep -f "python3? .*svcomp.py test run" > /dev/null; then
    echo "Another SV-COMP run is live; not starting." >&2
    exit 1
fi
"$SV_COMP/swat-sa-showcase/scripts/build.sh" > /dev/null
NRE="--sa-no-runtime-exception"
for cfg in ${CONFIGS:-sa nosa}; do
    case $cfg in
        sa) flags="$NRE" ;;
        nosa) flags="--no-sa --no-loop-summaries --no-property-precheck --config-file sv-comp-noSA.cfg" ;;
        noprecheck) flags="$NRE --no-property-precheck" ;;
        noslice) flags="$NRE --no-sa-slicing" ;;
        nosum) flags="$NRE --no-loop-summaries" ;;
        nostate)
            nostate_cfg=$(mktemp --suffix=.cfg)
            sed 's/^loopStateMatching=true/loopStateMatching=false/' sv-comp.cfg > "$nostate_cfg"
            flags="$NRE --config-file $nostate_cfg" ;;
        *) echo "unknown config $cfg" >&2; exit 1 ;;
    esac
    ./scripts/svcomp test run --mode parallel --no-witness --categories valid-assert.prp --categories no-runtime-exception.prp \
        --search-order coverage --benchmark-dir swat-sa-showcase --workers "${WORKERS:-14}" \
        --testcase-timeout-s "${TIMEOUT:-300}" $flags "$@" > /dev/null 2>&1
    echo "$cfg $(ls -td runs/run_* | head -1)"
done
