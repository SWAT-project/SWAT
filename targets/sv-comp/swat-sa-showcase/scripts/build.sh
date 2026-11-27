#!/bin/bash
# Compiles every task in place (.class files next to the sources, as in sv-benchmarks).
# Usage: ./build.sh
set -euo pipefail
JAVA_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../java" && pwd)"
JAVAC_FLAGS="--release 8 -g:source,lines -nowarn -Xlint:-options"
javac $JAVAC_FLAGS -d "$JAVA_DIR/common" $(find "$JAVA_DIR/common" -name '*.java')
for d in $(find "$JAVA_DIR" -mindepth 2 -maxdepth 2 -type d ! -path "$JAVA_DIR/common/*"); do
    find "$d" -name '*.class' -delete
    javac $JAVAC_FLAGS -cp "$JAVA_DIR/common" -d "$d" $(find "$d" -name '*.java')
done
echo "Built $(find "$JAVA_DIR" -name Main.class | wc -l) tasks."
