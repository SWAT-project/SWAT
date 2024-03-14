#!/bin/bash

set -euxo pipefail

# change path to root of project
pushd "$(dirname "$0")"/../..

# remove old state
rm -rf logs && mkdir logs
cd logs
pwd
ls ../targets/sv-comp/jdart-regression/addition01/
java \
-Xmx32g \
-Dconfig.path=../targets/sv-comp/swat.cfg \
-javaagent:../symbolic-executor/lib/symbolic-executor.jar \
-jar ../targets/sv-comp/jdart-regression/addition01/build/Main.class > ../targets/sv-comp/jdart-regression/addition01/run.log 2>&1

pushd -0 && dirs -c

