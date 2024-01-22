#!/bin/bash

set -euxo pipefail

# change path to root of project
pushd "$(dirname "$0")"/../../..

# remove old state
rm -rf logs && mkdir logs
cd logs

java \
-Xmx32g \
-Dswat.cfg=../targets/basic-tests/string/swat.cfg \
-javaagent:../symbolic-executor/lib/symbolic-executor.jar \
-jar ../targets/basic-tests/string/build/libs/string.jar > ../targets/basic-tests/string/run.log 2>&1

pushd -0 && dirs -c

