#!/bin/bash

cd "$(dirname "$0")"
pushd ../../../../../../david-project-fontus/

./gradlew tools:sql-tainter:jar

popd

cp ../../../../../../david-project-fontus/tools/sql-tainter/build/libs/sql-tainter.jar ../../tools/sql-tainter.jar
