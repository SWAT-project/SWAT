#!/bin/bash

set -euxo pipefail

# change path to root of project
pushd "$(dirname "$0")"

# remove old state
rm -rf logs

# Default mode is local
mode=${1:-"local"}

if [ "$mode" == "explorer" ]; then
    config_file="swat-explorer.cfg"
else
    config_file="swat-local.cfg"
fi

java \
-Xmx32g \
-Dconfig.path=$config_file \
-Djava.library.path=../../../libs/java-library-path \
-Dagent.logging.level=DEBUG \
-javaagent:../../../symbolic-executor/lib/symbolic-executor.jar \
-jar build/libs/spring-1-0.0.1-SNAPSHOT.jar
# > ../targets/examples/basic-2/run.log 2>&1

pushd -0 && dirs -c

