#!/bin/bash

pushd openolat

mvn clean
mvn clean package compile war:war -Pcompressjs,tomcat -DskipTests

popd