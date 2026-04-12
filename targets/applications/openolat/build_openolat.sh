#!/bin/bash

pushd "$(dirname "$0")"

mkdir tmp || true
ls ../../../.git/modules/targets/applications/openolat/openolat
cp -r ../../../.git/modules/targets/applications/openolat/openolat tmp
pushd tmp
mv openolat .git
popd

docker build -t swat/openolat_builder -f docker/build/Dockerfile .
docker run --name openolat-container swat/openolat_builder ./build_openolat_docker.sh
docker cp openolat-container:/home/developer/openolat/target/openolat-lms-20.0-SNAPSHOT.war build
docker stop openolat-container
docker rm openolat-container

rm -rf tmp

popd