#!/bin/bash

set -euo pipefail

echo "Starting Symbolic Execution with JaCoCo Coverage (File Mode)"
echo "==========================================================="

# change path to root of project
pushd "$(dirname "$0")"/../../../..

# Configuration
JACOCO_EXEC_FILE=${JACOCO_EXEC_FILE:-jacoco.exec}
JACOCO_SESSION=${JACOCO_SESSION:-symbolic-execution-$(date +%Y%m%d-%H%M%S)}

echo "Coverage data file: $JACOCO_EXEC_FILE"
echo "Session ID: $JACOCO_SESSION"
echo

# remove old state
rm -rf logs && mkdir logs
pushd logs

# Remove old coverage data
rm -f $JACOCO_EXEC_FILE

echo "Starting Java application with both agents..."
echo

java \
-Xmx16g \
-Dconfig.path=../targets/applications/swat-artificial/personal-finance-engine-v2/swat.cfg \
-Djava.library.path=$(pwd)/../libs/java-library-path \
"-javaagent:../libs/jacoco/jacocoagent.jar=destfile=$JACOCO_EXEC_FILE,output=file,sessionid=$JACOCO_SESSION,includes=de.uzl.its.targets.*,dumponexit=true" \
-javaagent:../symbolic-executor/lib/symbolic-executor.jar \
-jar ../targets/applications/swat-artificial/personal-finance-engine-v2/build/libs/personal-finance-engine-v2-0.0.1-SNAPSHOT.jar

popd -0 && dirs -c
