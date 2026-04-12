#!/bin/bash

set -euxo pipefail

# change path to root of project
pushd "$(dirname "$0")"/../../..

# remove old state
rm -rf logs && mkdir logs
cd logs

java \
-javaagent:../symbolic-executor/lib/symbolic-executor.jar \
-Dconfig.path=../targets/tests/database-mock-test/swat.cfg \
-Djava.library.path=../libs/java-library-path \
-Dagent.logging.level=DEBUG \
-jar ../targets/tests/database-mock-test/build/libs/database-mock-test.jar

pushd -0 && dirs -c

