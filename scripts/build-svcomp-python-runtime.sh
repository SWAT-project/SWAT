#!/usr/bin/env bash
set -euo pipefail

# Builds the Python dependency directory that ships in the SV-COMP archive as
# libs/python/. The archive runs SymbolicExplorer with the competition
# machine's system python3 and puts this directory first on PYTHONPATH, so no
# virtual environment (and no absolute interpreter path) is baked into the ZIP.
#
# Wheels are resolved for the *target* interpreter, not the one running pip,
# so this works on any build host (e.g. an ubuntu-24.04 CI runner building for
# the Ubuntu 26.04 / Python 3.14 competition image).
#
# Usage: scripts/build-svcomp-python-runtime.sh <output-dir>
#
# Environment:
#   SWAT_SVCOMP_PYTHON_VERSION  target CPython "major.minor" (default: 3.14,
#                               the python3 of Ubuntu 26.04 / SV-COMP 2027)
#   SWAT_SVCOMP_PIP             pip command to use (default: "python3 -m pip")
#   SWAT_SVCOMP_WITNESSCREATOR  replaces the pinned witnesscreator requirement
#                               (git tag) with a local source directory or
#                               wheel, for builds against an unreleased version

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REQUIREMENTS="${ROOT_DIR}/scripts/svcomp-package/requirements.txt"
PY_VERSION="${SWAT_SVCOMP_PYTHON_VERSION:-3.14}"
PIP_CMD="${SWAT_SVCOMP_PIP:-python3 -m pip}"
OUT_DIR="${1:?usage: $0 <output-dir>}"

[[ "$PY_VERSION" =~ ^3\.[0-9]+$ ]] || { echo "error: invalid SWAT_SVCOMP_PYTHON_VERSION: ${PY_VERSION}" >&2; exit 1; }
PY_TAG="cp${PY_VERSION/./}"

if [[ -e "$OUT_DIR" ]] && [[ -n "$(ls -A "$OUT_DIR")" ]]; then
  echo "error: output directory is not empty: ${OUT_DIR}" >&2
  exit 1
fi
mkdir -p "$OUT_DIR"

# witnesscreator (pure Python, stdlib only) is pinned to a git tag and built from
# source below; everything else comes as wheels for the target interpreter.
WHEEL_REQUIREMENTS="$(mktemp "${TMPDIR:-/tmp}/swat-svcomp-requirements.XXXXXX")"
trap 'rm -f "$WHEEL_REQUIREMENTS"' EXIT
grep -v '^witnesscreator' "$REQUIREMENTS" > "$WHEEL_REQUIREMENTS"
WITNESSCREATOR_REQ="${SWAT_SVCOMP_WITNESSCREATOR:-$(grep '^witnesscreator' "$REQUIREMENTS")}"
[[ -n "$WITNESSCREATOR_REQ" ]] || { echo "error: no witnesscreator requirement in ${REQUIREMENTS}" >&2; exit 1; }

# --no-deps: requirements.txt is a complete, pinned closure.
# --only-binary: never compile on the build host (would target the wrong ABI).
# shellcheck disable=SC2086
$PIP_CMD install \
  --disable-pip-version-check \
  --no-cache-dir \
  --no-compile \
  --no-deps \
  --only-binary=:all: \
  --implementation cp \
  --python-version "$PY_VERSION" \
  --abi "$PY_TAG" \
  --abi abi3 \
  --abi none \
  --platform manylinux_2_17_x86_64 \
  --platform manylinux2014_x86_64 \
  --platform manylinux_2_27_x86_64 \
  --platform manylinux_2_28_x86_64 \
  --platform manylinux_2_34_x86_64 \
  --platform any \
  --target "$OUT_DIR" \
  -r "$WHEEL_REQUIREMENTS"

# Pure-Python package: built on the host, valid for any CPython >= 3.10.
# shellcheck disable=SC2086
$PIP_CMD install \
  --disable-pip-version-check \
  --no-cache-dir \
  --no-compile \
  --no-deps \
  --target "$OUT_DIR" \
  "$WITNESSCREATOR_REQ"

# Console-script wrappers carry the build host's interpreter path; unused.
rm -rf "${OUT_DIR}/bin"
find "$OUT_DIR" -name '__pycache__' -type d -prune -exec rm -rf {} +

# Non-hidden marker (BenchExec skips hidden files when collecting tool files).
echo "$PY_VERSION" > "${OUT_DIR}/PYTHON_VERSION"
cp "$REQUIREMENTS" "${OUT_DIR}/requirements.txt"

echo "Python runtime for CPython ${PY_VERSION} written to: ${OUT_DIR}"
