#!/bin/bash
# Runs swat-benchmarks once per configuration, one run after the other, and prints the run dirs.
# Usage: ./run_configs.sh [svcomp test run options...]   e.g. --suite dispatcher
# Env: CONFIGS (default "sa nosa rank"), TIMEOUT (default 300), WORKERS (default 48)
set -euo pipefail
SV_COMP="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$SV_COMP"
if pgrep -f "python3? .*svcomp.py test run" > /dev/null; then
    echo "Another SV-COMP run is live; not starting." >&2
    exit 1
fi
"$SV_COMP/swat-benchmarks/scripts/build.sh" > /dev/null
for cfg in ${CONFIGS:-sa nosa rank}; do
    case $cfg in
        sa) flags="" ;;
        nosa) flags="--no-sa" ;;
        rank) flags="--sa-rank-distance" ;;
        *) echo "unknown config $cfg" >&2; exit 1 ;;
    esac
    ./scripts/svcomp test run --mode parallel --no-witness --categories valid-assert.prp \
        --benchmark-dir swat-benchmarks --workers "${WORKERS:-48}" \
        --testcase-timeout-s "${TIMEOUT:-300}" $flags "$@" > /dev/null 2>&1
    echo "$cfg $(ls -td runs/run_* | head -1)"
done
