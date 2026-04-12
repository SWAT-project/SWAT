#!/bin/bash

cd "$(dirname "$0")"

pushd ../../.data/

java -jar ../tools/sql-tainter.jar db_dump.sql

popd