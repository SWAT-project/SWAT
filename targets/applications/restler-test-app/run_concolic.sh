#!/bin/bash

# set -euxo pipefail
set -euo pipefail

# change path to root of project
pushd "$(dirname "$0")"/../../..


# remove old state
rm -rf logs && mkdir logs
pushd logs

java \
-Xmx16g \
-Dconfig.path=../targets/applications/restler-test-app/swat.cfg \
-Djava.library.path=$(pwd)/../libs/java-library-path \
-Dagent.logging.level=INFO \
-javaagent:../symbolic-executor/lib/symbolic-executor.jar \
-jar ../targets/applications/restler-test-app/build/libs/restler-test-app-0.0.1-SNAPSHOT.jar

# Idea for debugging: -verbose

pushd -0 && dirs -c
