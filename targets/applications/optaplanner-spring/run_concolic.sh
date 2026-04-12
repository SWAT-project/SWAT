#!/bin/bash

set -euo pipefail

# change path to root of project
pushd "$(dirname "$0")"/../../..


# remove old state
rm -rf logs && mkdir logs
pushd logs

 # -verbose:class \
java \
-Xmx16g \
-Dconfig.path=../targets/applications/optaplanner-spring/swat.cfg \
-Djava.library.path=$(pwd)/../libs/java-library-path \
-javaagent:../symbolic-executor/lib/symbolic-executor.jar \
-jar ../targets/applications/optaplanner-spring/build/libs/optaplanner-spring-0.0.1-SNAPSHOT.jar



pushd -0 && dirs -c
