#!/bin/bash

# set -euxo pipefail
set -euo pipefail

# change path to root of project
pushd "$(dirname "$0")"


# remove old state
rm -rf logs && mkdir logs

java \
-Xmx16g \
-Dconfig.path=swat.cfg \
-Djava.library.path=../../../libs/java-library-path \
-javaagent:../../../symbolic-executor/lib/symbolic-executor.jar \
-jar ../../../targets/applications/modelmapper-test-app/build/libs/modelmapper-test-app-0.0.1-SNAPSHOT.jar

# Idea for debugging: -verbose

pushd -0 && dirs -c
