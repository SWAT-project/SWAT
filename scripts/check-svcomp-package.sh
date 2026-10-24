#!/usr/bin/env bash
set -euo pipefail

# Checks an SV-COMP ZIP built by scripts/package-svcomp.sh against the archive
# requirements (single top-level directory, LICENSE, THIRD-PARTY-LICENSES.md,
# README, smoketest.sh, one-line --version) and runs it in a bare competition base image with only
# the fm-tools required_ubuntu_packages installed, like `fm-weck smoke-test`.
#
# Usage: scripts/check-svcomp-package.sh <zip> [base-image]
# Requires docker. Default base image: docker.io/ubuntu:26.04 (SV-COMP 2027).

ZIP_PATH="$(realpath "${1:?usage: $0 <zip> [base-image]}")"
BASE_IMAGE="${2:-docker.io/ubuntu:26.04}"
# Keep in sync with required_ubuntu_packages in fm-tools data/swat.yml.
UBUNTU_PACKAGES="python3 openjdk-17-jdk-headless"

fail() {
  echo "error: $*" >&2
  exit 1
}

echo "ZIP: ${ZIP_PATH} ($(stat -c %s "$ZIP_PATH") bytes)"

top_dirs="$(unzip -Z1 "$ZIP_PATH" | cut -d/ -f1 | sort -u)"
[[ "$(wc -l <<< "$top_dirs")" -eq 1 ]] || fail "ZIP must have a single top-level directory, found: ${top_dirs//$'\n'/ }"

check_dir="$(mktemp -d "${TMPDIR:-/tmp}/swat-svcomp-check.XXXXXX")"
unzip -q "$ZIP_PATH" -d "$check_dir"
tool_dir="${check_dir}/${top_dirs}"
for f in LICENSE README.md smoketest.sh run-swat.sh libs/python/PYTHON_VERSION cfg-extraction/cfg-extractor-precheck.jar; do
  [[ -f "${tool_dir}/${f}" ]] || fail "missing ${f} in archive root ${top_dirs}/"
done
[[ -s "${tool_dir}/THIRD-PARTY-LICENSES.md" ]] || fail "missing or empty THIRD-PARTY-LICENSES.md in archive root ${top_dirs}/"
[[ -x "${tool_dir}/run-swat.sh" && -x "${tool_dir}/smoketest.sh" ]] || fail "run-swat.sh and smoketest.sh must be executable"

echo "Running checks in ${BASE_IMAGE} (packages: ${UBUNTU_PACKAGES})"
docker run --rm -v "${tool_dir}:/tool" -w /tool -e UBUNTU_PACKAGES="$UBUNTU_PACKAGES" "$BASE_IMAGE" bash -euc '
  apt-get update -qq
  DEBIAN_FRONTEND=noninteractive apt-get install -y -qq --no-install-recommends $UBUNTU_PACKAGES > /dev/null
  echo "python3: $(python3 --version)"
  echo "java: $(java -version 2>&1 | head -n 1)"
  PYTHONPATH=libs/python python3 -c "import fastapi, pydantic, uvicorn, requests, yaml, z3, witnesscreator; print(\"z3:\", z3.get_version_string())"
  version="$(./run-swat.sh --version)"
  echo "version: ${version}"
  [ -n "$version" ] && [ "$(printf "%s\n" "$version" | wc -l)" -eq 1 ] || { echo "error: --version must print one non-empty line" >&2; exit 1; }
  [ "$(./run-swat.sh -v)" = "$version" ] || { echo "error: -v and --version differ" >&2; exit 1; }
  precheck="$(java -cp cfg-extraction/cfg-extractor-precheck.jar de.uzl.its.cfg.staticcheck.ExceptionFreedomCheck smoketest/common smoketest | tail -n 1)"
  echo "property pre-check: ${precheck}"
  case "$precheck" in *"\"tool\": \"cfg-extraction\""*) ;; *) echo "error: the property pre-check did not run" >&2; exit 1;; esac
  ./smoketest.sh > smoketest.out 2>&1 || { cat smoketest.out; exit 1; }
  echo "smoketest stdout: $(wc -c < smoketest.out) bytes"
  [ -f witness/witness.yml ] && [ -f witness/witness.graphml ] || { echo "error: smoketest left no witness/witness.yml + witness.graphml" >&2; exit 1; }
  tail -n 1 smoketest.out
'
echo "SV-COMP package check passed."
