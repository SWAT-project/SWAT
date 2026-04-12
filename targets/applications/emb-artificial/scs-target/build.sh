#!/bin/bash

# set -euxo pipefail
set -euo pipefail

# change path to root of project
pushd "$(dirname "$0")"/scs

mvn -DskipTests install

pushd -0 && dirs -c