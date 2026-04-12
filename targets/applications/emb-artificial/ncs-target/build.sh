#!/bin/bash

# set -euxo pipefail
set -euo pipefail

# change path to root of project
pushd "$(dirname "$0")"/ncs

mvn -DskipTests install

pushd -0 && dirs -c