#!/bin/bash
set -x
cd "$(dirname $0)"
echo "$(dirname $0)"
cp ../../../../symbolic-executor/lib/symbolic-executor.jar cache/symbolic-executor.jar
cp ../swat.cfg cache/swat.cfg
cp ../build/libs/restler-test-app-0.0.1-SNAPSHOT.jar cache/target.jar

SERVER_PORT=$(grep "server.port" ../src/main/resources/application.properties | cut -d'=' -f2)
echo "SERVER_PORT=$SERVER_PORT"
docker build --build-arg SERVER_PORT=$SERVER_PORT -t its/restler-target:${VERSION:-latest} .
retVal=$?
if [ $retVal -ne 0 ]; then
    echo "Error building restler-target image"
    exit $retVal
fi