#!/bin/bash

set -e

pushd /home/developer/openhospital/openhospital-ui

git clone --depth=1 -b ${OH_CORE_BRANCH} https://github.com/${GITHUB_ORG}/openhospital-ui.git .
npm ci --legacy-peer-deps; npm build

popd