#!/bin/bash

set -e

python3 y2j.py
docker run --rm -v $(pwd)/oh_build/api:/home/developer/target-spec-dir -v $(pwd)/oh_build/restler_working_dir:/home/developer/restler-working-dir \
    swat/restler-builder sh -c 'cd /home/developer/restler-working-dir && /home/developer/restler-repo/RESTler/restler/Restler compile --api_spec /home/developer/target-spec-dir/oh_api_from_gitea_tub.json'