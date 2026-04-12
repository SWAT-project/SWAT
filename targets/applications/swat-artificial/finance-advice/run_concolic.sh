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
-Dconfig.path=../targets/applications/swat-artificial/finance-advice/swat.cfg \
-Djava.library.path=$(pwd)/../libs/java-library-path \
-Ddatabase.port=9090 \
-javaagent:../symbolic-executor/lib/symbolic-executor.jar \
-jar ../targets/applications/swat-artificial/finance-advice/build/libs/finance-advice-0.0.1-SNAPSHOT.jar

pushd -0 && dirs -c
