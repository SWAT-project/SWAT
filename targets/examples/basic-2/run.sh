#!/bin/bash

set -euxo pipefail

# change path to root of project
pushd "$(dirname "$0")"/../../..

# remove old state
rm -rf logs && mkdir logs
cd logs

java \
-Xmx32g \
-Dconfig.path=../targets/examples/basic-2/swat.cfg \
-Djava.library.path=../libs/java-library-path \
-Dagent.logging.level=DEBUG \
-javaagent:../symbolic-executor/lib/symbolic-executor.jar \
-jar ../targets/examples/basic-2/build/libs/basic-2.jar Test 1
# > ../targets/examples/basic-2/run.log 2>&1

pushd -0 && dirs -c

