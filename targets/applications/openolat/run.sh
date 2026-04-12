./z3_extract_helper.sh

SWAT_BASE_DIR="$(dirname "$0")"/../../..

docker build -t swat/openolat_runner -f docker/run/Dockerfile .

mkdir -p openolat-mounts/logs openolat-mounts/data | true

docker run --network=host \
  --name swat_openolat_runner_c \
  -v ${SWAT_BASE_DIR}/symbolic-executor/build/libs:/swat/executor \
  -v ./openolat-mounts/logs:/home/openolat/logs \
  -v ./openolat-mounts/data:/home/openolat/olatdata \
  --rm swat/openolat_runner
# - olatdata-regular:/home/openolat/olatdata