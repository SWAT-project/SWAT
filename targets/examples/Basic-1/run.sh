#!/bin/bash

set -euxo pipefail

# change path to root of project
pushd "$(dirname "$0")"/../../..

# remove old state
rm -rf logs && mkdir logs
cd logs

java \
-Xmx32g \
-Dconfig.path=../targets/examples/Basic-1/swat.cfg \
-Djava.library.path=../libs/java-library-path \
-Dlogging.level=DEBUG \
-javaagent:../symbolic-executor/lib/symbolic-executor.jar \
-jar ../targets/examples/Basic-1/build/libs/Basic-1.jar > ../targets/examples/Basic-1/run.log 2>&1

pushd -0 && dirs -c

