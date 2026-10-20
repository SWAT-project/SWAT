#!/bin/bash
# Downloads the real-tools libraries' sources from Maven Central into java/real-tools/lib-*/
# (gitignored); build.sh compiles them. Unmodified upstream sources, kept under their licenses.
set -euo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../java/real-tools" && pwd)"
MAVEN=https://repo1.maven.org/maven2
fetch() {  # lib-dir maven-path
    rm -rf "$DIR/$1" && mkdir -p "$DIR/$1"
    curl -sSfL "$MAVEN/$2-sources.jar" -o "$DIR/$1/sources.jar"
    (cd "$DIR/$1" && unzip -q sources.jar && rm -rf sources.jar META-INF)
}
fetch lib-getopt gnu/getopt/java-getopt/1.0.13/java-getopt-1.0.13
rm -f "$DIR/lib-getopt/gnu/getopt/GetoptDemo.java"
fetch lib-opencsv net/sf/opencsv/opencsv/2.3/opencsv-2.3
# Only the parser: the rest needs java.sql and bean introspection.
find "$DIR/lib-opencsv" -name '*.java' ! -name CSVParser.java -delete
echo "Fetched: $(ls "$DIR" | grep '^lib-' | tr '\n' ' ')"
