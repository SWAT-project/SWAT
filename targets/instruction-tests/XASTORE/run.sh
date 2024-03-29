#!/bin/bash

set -euxo pipefail

# change path to root of project
pushd "$(dirname "$0")"/../../..

# remove old state
rm -rf logs && mkdir logs
cd logs

java \
-Xmx32g \
-Dconfig.path=../targets/instruction-tests/XASTORE/swat.cfg \
-Djava.library.path=../libs/java-library-path \
-Dlogging.level=DEBUG \
-javaagent:../symbolic-executor/lib/symbolic-executor.jar \
-jar ../targets/instruction-tests/XASTORE/build/libs/XASTORE.jar > ../targets/instruction-tests/XASTORE/run.log 2>&1

pushd -0 && dirs -c

