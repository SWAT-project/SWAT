#!/bin/bash

set -e

docker build -f docker/backend/Dockerfile -t swat/openhospital-backend-builder .
docker run --name obp-container swat/openhospital-backend-builder ./build_openhospital_backend.sh
docker cp obp-container:/home/developer/openhospital/openhospital-api/target/openhospital-api-0.0.2.jar oh_build
docker cp obp-container:/home/developer/openhospital/openhospital-api/rsc oh_build
docker stop obp-container
docker rm obp-container


docker build -f docker/frontend/Dockerfile -t swat/openhospital-ui-builder .
docker run --name oup-container swat/openhospital-ui-builder ./clone_openhospital_ui.sh
docker cp oup-container:/home/developer/openhospital/openhospital-ui/api oh_build
docker stop oup-container
docker rm oup-container