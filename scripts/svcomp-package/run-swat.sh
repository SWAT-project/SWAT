#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# '-v' is what the BenchExec tool-info module passes; '--version' is the
# SV-COMP archive convention. Both print a single version line.
for arg in "$@"
do
  if [ "$arg" == "-v" ] || [ "$arg" == "--version" ]; then
    # The package version (e.g. svcomp27-rc1) from BUILD_INFO.txt, written by
    # scripts/package-svcomp.sh.
    version="$(sed -n 's/^version=//p' "$SCRIPT_DIR/BUILD_INFO.txt" 2>/dev/null)"
    if [ -n "$version" ]; then
      echo "$version"
      exit 0
    fi
    exec java -cp "$SCRIPT_DIR/symbolic-executor/lib/symbolic-executor.jar" de.uzl.its.swat.Version
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

# The violation witness is written by the explorer to witness/ from the given input paths.

# Python dependencies are shipped in libs/python (built for one CPython
# minor version, recorded in libs/python/PYTHON_VERSION) and used with the
# system python3. Override the interpreter with SWAT_PYTHON if needed.
PYTHON="${SWAT_PYTHON:-python3}"
PYTHON_LIBS="$SCRIPT_DIR/libs/python"
expected_py="$(cat "$PYTHON_LIBS/PYTHON_VERSION" 2>/dev/null)"
actual_py="$("$PYTHON" -c 'import sys; print("%d.%d" % sys.version_info[:2])' 2>/dev/null)"
if [ -z "$expected_py" ]; then
    echo "Error: Python dependencies not found at $PYTHON_LIBS"
    exit 1
fi
if [ "$actual_py" != "$expected_py" ]; then
    echo "Error: SWAT's Python dependencies were built for Python $expected_py, but '$PYTHON' is '${actual_py:-not runnable}'"
    exit 1
fi
export PYTHONPATH="$PYTHON_LIBS${PYTHONPATH:+:$PYTHONPATH}"
export PYTHONNOUSERSITE=1

echo "Running SWAT with arguments: $@"
"$PYTHON" -u "$SCRIPT_DIR/run_swat.py" "$@" > out.log 2>&1

# SV-COMP requires tool output below 2 MB. Long explorations can log far more,
# so past the cap print the head, every verdict line, and the tail (which holds
# the final verdict and witness). The full log stays in out.log.
MAX_STDOUT_BYTES=$((1536 * 1024))
log_size="$(stat -c %s out.log)"
if [ "$log_size" -le "$MAX_STDOUT_BYTES" ]; then
    cat out.log
else
    head -c $((256 * 1024)) out.log
    echo
    echo "[run-swat.sh] output truncated: ${log_size} bytes total; verdict lines and tail follow"
    grep -F '[VERDICT ' out.log | head -n 200 | cut -c1-500
    echo "[run-swat.sh] ... tail ..."
    tail -c $((1024 * 1024)) out.log
fi
