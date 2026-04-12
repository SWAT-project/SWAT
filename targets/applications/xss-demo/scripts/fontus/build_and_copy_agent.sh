#!/bin/bash

cd "$(dirname "$0")"

pushd ../../../../../../david-project-fontus/
./gradlew fontus:shadowJar
popd

cp ../../../../../../david-project-fontus/fontus/build/libs/fontus-1.0.0.jar ../../libs/fontus-1.0.0.jar