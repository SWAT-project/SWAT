#!/bin/bash

set -euxo pipefail

# change path to root of project
pushd "$(dirname "$0")"/../../..

# remove old state
rm -rf logs && mkdir logs
cd logs

java \
-Xmx32g \
-Djava.library.path=../libs/java-library-path \
-Dlogging.level=DEBUG \
-Dconfig.path=../targets/instruction-tests/TRYCATCH/swat.cfg \
-javaagent:../symbolic-executor/lib/symbolic-executor.jar \
-jar ../targets/instruction-tests/TRYCATCH/build/libs/TRYCATCH.jar > ../targets/instruction-tests/TRYCATCH/run.log 2>&1

pushd -0 && dirs -c

