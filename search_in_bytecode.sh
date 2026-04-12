#!/bin/bash

PATH="$1"
SEARCH_TERM="$2"

pushd logs/logs/instrumented
find $PATH -iname "*.class" -exec bash -c 'mkdir -p decompiled/$(dirname "$1") && javap -v "$1" > "decompiled/$1" && grep -H -i $SEARCH_TERM "decompiled/$1"' -- {} \;
popd