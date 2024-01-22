#!/bin/bash

# change path to root of project
cd "$(dirname "$0")"
cd ../../..

# remove old state
rm -rf catg_tmp && mkdir catg_tmp
cd catg_tmp

java \
-Djanala.conf=../targets/basic-tests/long-shift/catg.conf \
-XX:-Inline \
-Xbootclasspath/a:\
../symbolic-executor/lib/symbolic-executor.jar \
-cp ../symbolic-executor/lib/symbolic-executor.jar \
-javaagent:../symbolic-executor/lib/symbolic-executor.jar \
-jar ../targets/basic-tests/long-shift/build/libs/LongShift.jar

# Idea for debugging: -verbose