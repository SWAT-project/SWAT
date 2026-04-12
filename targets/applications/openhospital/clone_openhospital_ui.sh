#!/bin/bash

set -e

GITHUB_ORG=informatici
OH_CORE_BRANCH=v1.12.0
OH_API_BRANCH=v1.12.0

pushd /home/developer/openhospital/openhospital-ui

git clone --depth=1 -b ${OH_CORE_BRANCH} https://github.com/${GITHUB_ORG}/openhospital-ui.git .

popd