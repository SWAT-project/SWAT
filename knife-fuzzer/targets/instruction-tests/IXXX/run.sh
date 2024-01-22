#!/bin/bash

set -euxo pipefail

# change path to root of project
pushd "$(dirname "$0")"/../../..

# remove old state
rm -rf logs && mkdir logs
cd logs

java \
-Xmx32g \
-Dswat.cfg=../targets/instruction-tests/IXXX/swat.cfg \
-javaagent:../symbolic-executor/lib/symbolic-executor.jar \
-jar ../targets/instruction-tests/IXXX/build/libs/IXXX.jar > ../targets/instruction-tests/IXXX/run.log 2>&1

pushd -0 && dirs -c

