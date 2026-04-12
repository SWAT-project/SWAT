#!/bin/bash

set -euxo pipefail
cd logs

java \
-Xmx16g \
-Dswat.cfg=../config/swat.cfg \
-javaagent:../symbolic/tmp/tmp/symbolic-executor.jar \
-jar ../target/target.jar

pushd -0 && dirs -c
