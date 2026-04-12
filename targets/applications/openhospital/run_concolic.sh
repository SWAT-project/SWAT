#!/bin/bash

set -euo pipefail

pushd "$(dirname "$0")"


# remove old state
rm -rf logs && mkdir logs

 # -verbose:class \
java \
-Xmx16g \
-Dconfig.path=swat.cfg \
-Djava.library.path=../../../libs/java-library-path \
-javaagent:../../../symbolic-executor/lib/symbolic-executor.jar \
-jar oh_build/openhospital-api-0.0.2.jar


# -cp "../targets/web-apps/openhospital/oh_build/openhospital-api-0.0.2.jar:../targets/web-apps/openhospital/oh_build/rsc/:../targets/web-apps/openhospital/oh_build/static/" \
# --debug
#-jar ../targets/web-apps/openhospital/oh_build/openhospital-api-0.0.2.jar
# /home/florian/Desktop/oh_temp/test.pom 

# Idea for debugging: -verbose

pushd -0 && dirs -c
