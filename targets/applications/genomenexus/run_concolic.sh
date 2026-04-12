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
-Dconfig.path=../targets/applications/genomenexus/swat.cfg \
-Djava.library.path=$(pwd)/../libs/java-library-path \
-Dspring.data.mongodb.uri=mongodb://localhost:27017/annotator \
-Dspring.cache.type=NONE \
-javaagent:../symbolic-executor/lib/symbolic-executor.jar \
-jar ../targets/applications/genomenexus/build/genomenexus.war

pushd -0 && dirs -c
