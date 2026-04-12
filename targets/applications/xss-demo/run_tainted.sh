#!/bin/bash

set -euo pipefail

pushd "$(dirname "$0")"

# remove old state
rm -rf logs && mkdir logs

# Clean up the database
docker exec -it xss-demo-mysql mysql -uroot -proot -e "DROP DATABASE IF EXISTS xss_demo; CREATE DATABASE xss_demo;"

# Run with tainted profile using parent project's Gradle
cd ../../..  # Go to the root project directory
./gradlew :targets:applications:xss-demo:bootRunTainted

popd 