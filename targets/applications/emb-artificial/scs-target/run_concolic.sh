#!/bin/bash

set -euo pipefail

# change path to root of project
pushd "$(dirname "$0")"/../../../..


# remove old state
rm -rf logs && mkdir logs
pushd logs

 # -verbose:class \
java \
-Xmx16g \
-Dconfig.path=../targets/applications/emb-artificial/scs-target/swat.cfg \
-Djava.library.path=$(pwd)/../libs/java-library-path \
-javaagent:../symbolic-executor/lib/symbolic-executor.jar \
-jar ../targets/applications/emb-artificial/scs-target/scs/target/rest-scs-sut.jar

pushd -0 && dirs -c
