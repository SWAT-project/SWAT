#!/bin/bash
# Compiles every task in place (.class files next to the sources, as in sv-benchmarks). A suite's
# lib-* folders (see fetch_tools.sh) are compiled first and are on its tasks' classpath.
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
    cp="$JAVA_DIR/common"
    for lib in $(find "$JAVA_DIR/$suite" -mindepth 1 -maxdepth 1 -type d -name 'lib-*' | sort); do
        find "$lib" -name '*.class' -delete
        javac $JAVAC_FLAGS -cp "$cp" -d "$lib" $(find "$lib" -name '*.java')
        cp="$cp:$lib"
    done
    find "$JAVA_DIR/$suite" -mindepth 1 -maxdepth 1 -type d ! -name 'lib-*' \
        | xargs -r -P 32 -I{} sh -c "find {} -name '*.class' -delete && javac $JAVAC_FLAGS -cp '$cp' -d {} {}/*.java"
done
echo "Built $(find "$JAVA_DIR" -name Main.class | wc -l) tasks."
