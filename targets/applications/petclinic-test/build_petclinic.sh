#!/bin/bash

pushd "$(dirname "$0")"

docker build -t swat/petclinic_builder -f docker/Dockerfile .
docker run --name petclinic-container swat/petclinic_builder ./build_petclinic_docker.sh
docker cp petclinic-container:/home/developer/petclinic/build/libs build
docker stop petclinic-container
docker rm petclinic-container