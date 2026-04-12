#!/bin/bash

pushd "$(dirname "$0")"

# build-database importer and server image first
pushd genomenexus-importer

git reset --hard
git apply ../bitnamilegacy.patch

docker build -t local-genome-nexus-importer --build-arg ARG_REF_ENSEMBL_VERSION=grch38_ensembl92 .

popd

docker build -t swat/genomenexus_builder -f docker/Dockerfile .
docker run --name genomenexus-container swat/genomenexus_builder ./build_genomenexus_docker.sh

docker cp genomenexus-container:/home/developer/genomenexus/web/target/web-0-unknown-version-SNAPSHOT.war build/genomenexus.war
# https://stackoverflow.com/questions/33078745/jacoco-maven-multi-module-project-coverage
# ToDo: For the coverage report we will have to aggregate from different modules / src folders
# ToDo: Check whether copy statement is correct
docker cp genomenexus-container:/home/developer/genomenexus/web/target/classes build
docker cp genomenexus-container:/home/developer/genomenexus/service/target/classes build
docker cp genomenexus-container:/home/developer/genomenexus/persistence/target/classes build
docker cp genomenexus-container:/home/developer/genomenexus/model/target/classes build
docker cp genomenexus-container:/home/developer/genomenexus/component/target/classes build

docker stop genomenexus-container
docker rm genomenexus-container

popd
