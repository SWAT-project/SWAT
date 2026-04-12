#!/bin/bash

set -euo pipefail

pushd "$(dirname "$0")"


# remove old state
rm -rf logs && mkdir logs

# Clean up the database
docker exec -it xss-demo-mysql mysql -uroot -proot -e "DROP DATABASE IF EXISTS xss_demo; CREATE DATABASE xss_demo;"

 # -verbose:class \
java \
-Xmx16g \
-jar build/libs/xss-demo-0.0.1-SNAPSHOT.jar

pushd -0 && dirs -c
