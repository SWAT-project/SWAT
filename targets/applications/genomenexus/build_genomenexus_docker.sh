#!/bin/bash

pushd genomenexus

mvn -DskipTests clean install

popd
