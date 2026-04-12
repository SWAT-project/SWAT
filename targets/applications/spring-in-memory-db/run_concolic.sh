#!/bin/bash

set -euo pipefail

# change path to root of project
pushd "$(dirname "$0")"/../../..


# remove old state
rm -rf logs && mkdir logs
pushd logs

java \
-Dconfig.path=../targets/applications/spring-in-memory-db/swat.cfg \
-Djava.library.path=$(pwd)/../libs/java-library-path \
-javaagent:../symbolic-executor/lib/symbolic-executor.jar \
-jar ../targets/applications/spring-in-memory-db/build/libs/spring-in-memory-db-0.0.1-SNAPSHOT.jar

pushd -0 && dirs -c
