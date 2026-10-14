#!/bin/bash
# Compiles every task in place (.class files next to the sources, as in sv-benchmarks).
# Usage: ./build.sh [suite...]   (default: all suites)
set -euo pipefail
JAVA_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../java" && pwd)"
JAVAC_FLAGS="--release 8 -g:source,lines -nowarn -Xlint:-options"

javac $JAVAC_FLAGS -d "$JAVA_DIR/common" $(find "$JAVA_DIR/common" -name '*.java')

if [ $# -eq 0 ]; then
    suites=$(find "$JAVA_DIR" -mindepth 1 -maxdepth 1 -type d ! -name common ! -name properties -printf '%f\n')
else
    suites="$*"
fi
for suite in $suites; do
    find "$JAVA_DIR/$suite" -mindepth 1 -maxdepth 1 -type d
done | xargs -P 32 -I{} sh -c "find {} -name '*.class' -delete && javac $JAVAC_FLAGS -cp '$JAVA_DIR/common' -d {} {}/*.java"
echo "Built $(find "$JAVA_DIR" -name Main.class | wc -l) tasks."
