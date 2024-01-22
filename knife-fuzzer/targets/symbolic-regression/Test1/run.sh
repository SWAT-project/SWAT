#!/bin/bash

set -euxo pipefail

# change path to root of project
pushd "$(dirname "$0")"/../../..

# remove old state
rm -rf logs && mkdir logs
cd logs

java \
-Xmx32g \
-Dswat.cfg=../targets/instruction-tests/Test1/swat.cfg \
-javaagent:../symbolic-executor/lib/symbolic-executor.jar \
-jar ../targets/instruction-tests/Test1/build/libs/Test1.jar > ../targets/instruction-tests/Test1/run.log 2>&1

pushd -0 && dirs -c

