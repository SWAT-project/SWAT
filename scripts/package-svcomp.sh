#!/usr/bin/env bash
set -euo pipefail

# This script packages artifacts that were already produced by CI. It must not
# invoke Gradle, create virtual environments, or download dependencies.
#
# Generated files are expected in the repository checkout after the CI build.
# Set SWAT_SVCOMP_ARTIFACT_DIR only if those generated files live somewhere
# else. The artifact root must contain:
#
# - symbolic-executor/lib/symbolic-executor.jar
# - targets/sv-comp/WitnessCreator/build/libs/WitnessCreator.jar
#
# Set SWAT_SVCOMP_REFERENCE_DIR to the extracted Zenodo reference package root
# from https://zenodo.org/records/17748741. The reference root is used for the
# pinned SV-COMP Python environment and JavaSMT compatibility JAR:
#
# - .venv_ubuntu_24_04_1__x86_64/
#
# Z3 is taken from the vendored Linux distribution ZIP in this repository.
#
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

VERSION="${SWAT_SVCOMP_VERSION:-$(git rev-parse --short HEAD)}"
COMMIT="${SWAT_SVCOMP_COMMIT:-$(git rev-parse HEAD 2>/dev/null || echo unknown)}"
REF="${SWAT_SVCOMP_REF:-$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo unknown)}"
CHANNEL="${SWAT_SVCOMP_CHANNEL:-}"
PACKAGE_NAME="swat-svcomp-${VERSION}"
WORK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/swat-svcomp-package.XXXXXX")"
PACKAGE_DIR="${WORK_DIR}/${PACKAGE_NAME}"
DIST_DIR="${ROOT_DIR}/build/distributions"
SUPPORT_DIR="${ROOT_DIR}/scripts/svcomp-package"
ARTIFACT_DIR="$(cd "${SWAT_SVCOMP_ARTIFACT_DIR:-$ROOT_DIR}" && pwd)"
REFERENCE_DIR="${SWAT_SVCOMP_REFERENCE_DIR:-}"
VENV_DIR_NAME="${SWAT_SVCOMP_VENV_DIR_NAME:-.venv_ubuntu_24_04_1__x86_64}"
LINUX_Z3_DIST="z3-4.15.4-x64-glibc-2.39"
LINUX_Z3_ZIP="${ROOT_DIR}/libs/${LINUX_Z3_DIST}.zip"

if [[ -n "$REFERENCE_DIR" ]]; then
  REFERENCE_DIR="$(cd "$REFERENCE_DIR" && pwd)"
fi

fail() {
  echo "error: $*" >&2
  exit 1
}

artifact_file() {
  local rel="$1"
  local path="${ARTIFACT_DIR}/${rel}"
  [[ -f "$path" ]] || fail "missing built artifact file: ${path}"
  printf '%s\n' "$path"
}

reference_file() {
  local rel="$1"
  [[ -n "$REFERENCE_DIR" ]] || fail "SWAT_SVCOMP_REFERENCE_DIR is required for ${rel}"

  local path="${REFERENCE_DIR}/${rel}"
  [[ -f "$path" ]] || fail "missing Zenodo reference file: ${path}"
  printf '%s\n' "$path"
}

first_artifact_file() {
  local path
  for rel in "$@"; do
    path="${ARTIFACT_DIR}/${rel}"
    if [[ -f "$path" ]]; then
      printf '%s\n' "$path"
      return 0
    fi
  done
  fail "missing built artifact file; checked: $*"
}

copy_tree_files() {
  local src="$1"
  local dest="$2"
  mkdir -p "$dest"
  find "$src" -type f \
    ! -path '*/__pycache__/*' \
    ! -path '*/.venv/*' \
    ! -name '*.pyc' \
    ! -name '*.pyo' \
    ! -name '*.iml' \
    -print0 | while IFS= read -r -d '' file; do
      local rel="${file#"$src"/}"
      mkdir -p "$dest/$(dirname "$rel")"
      cp "$file" "$dest/$rel"
    done
}

copy_artifact_file() {
  local src="$1"
  local dest="$2"
  local mode="${3:-0644}"
  mkdir -p "$(dirname "$dest")"
  install -m "$mode" "$src" "$dest"
}

copy_artifact_tree() {
  local src="$1"
  local dest="$2"
  [[ -d "$src" ]] || fail "missing artifact directory: ${src}"
  mkdir -p "$(dirname "$dest")"
  cp -a "$src" "$dest"
}

install_z3_runtime_file() {
  local name="$1"
  local mode="${2:-0644}"
  local dest="${PACKAGE_DIR}/libs/java-library-path/${name}"

  [[ -f "$LINUX_Z3_ZIP" ]] || fail "missing vendored Linux Z3 distribution: ${LINUX_Z3_ZIP}"
  mkdir -p "$(dirname "$dest")"
  unzip -p "$LINUX_Z3_ZIP" "${LINUX_Z3_DIST}/bin/${name}" > "$dest"
  chmod "$mode" "$dest"
}

echo "Packaging repository files from: ${ROOT_DIR}"
echo "Packaging built artifacts from: ${ARTIFACT_DIR}"

echo "Creating package staging directory: ${PACKAGE_DIR}"
mkdir -p "$PACKAGE_DIR"
{
  echo "version=${VERSION}"
  echo "channel=${CHANNEL}"
  echo "ref=${REF}"
  echo "commit=${COMMIT}"
} > "$PACKAGE_DIR/BUILD_INFO.txt"

install -m 0644 LICENSE "$PACKAGE_DIR/LICENSE"
install -m 0644 Third-Party-Licenses.html "$PACKAGE_DIR/Third-Party-Licenses.html"
install -m 0644 "$SUPPORT_DIR/README.md" "$PACKAGE_DIR/README.md"
install -m 0755 "$SUPPORT_DIR/run-swat.sh" "$PACKAGE_DIR/run-swat.sh"
install -m 0755 "$SUPPORT_DIR/compile-target.sh" "$PACKAGE_DIR/compile-target.sh"
install -m 0755 "$SUPPORT_DIR/smoketest.sh" "$PACKAGE_DIR/smoketest.sh"
install -m 0755 "$SUPPORT_DIR/run_swat.py" "$PACKAGE_DIR/run_swat.py"
install -m 0644 targets/sv-comp/sv-comp.cfg "$PACKAGE_DIR/sv-comp.cfg"

EXECUTOR_JAR="$(first_artifact_file \
  symbolic-executor/lib/symbolic-executor.jar \
  symbolic-executor/build/libs/symbolic-executor-all.jar)"
copy_artifact_file "$EXECUTOR_JAR" "$PACKAGE_DIR/symbolic-executor/lib/symbolic-executor.jar"

copy_tree_files symbolic-explorer "$PACKAGE_DIR/symbolic-explorer"

WITNESS_CREATOR_JAR="$(artifact_file targets/sv-comp/WitnessCreator/build/libs/WitnessCreator.jar)"
copy_artifact_file "$WITNESS_CREATOR_JAR" "$PACKAGE_DIR/WitnessCreator/build/libs/WitnessCreator.jar"
mkdir -p "$PACKAGE_DIR/WitnessCreator/witnesses"
install -m 0644 targets/sv-comp/WitnessCreator/witnesses/default_violation.st "$PACKAGE_DIR/WitnessCreator/witnesses/default_violation.st"
install -m 0644 targets/sv-comp/WitnessCreator/witnesses/witness.st "$PACKAGE_DIR/WitnessCreator/witnesses/witness.st"

install_z3_runtime_file z3 0755
install_z3_runtime_file libz3.so
install_z3_runtime_file libz3java.so
install_z3_runtime_file com.microsoft.z3.jar
install_z3_runtime_file libz3.a
JAVA_SMT_JAR="$(reference_file libs/java-library-path/java-smt-latest.jar)"
copy_artifact_file "$JAVA_SMT_JAR" "$PACKAGE_DIR/libs/java-library-path/java-smt-latest.jar"

copy_tree_files "$SUPPORT_DIR/smoketest" "$PACKAGE_DIR/smoketest"

[[ -n "$REFERENCE_DIR" ]] || fail "SWAT_SVCOMP_REFERENCE_DIR must point to the extracted Zenodo package root for ${VENV_DIR_NAME}"
copy_artifact_tree "${REFERENCE_DIR}/${VENV_DIR_NAME}" "${PACKAGE_DIR}/${VENV_DIR_NAME}"

mkdir -p "$DIST_DIR"
ZIP_PATH="${DIST_DIR}/${PACKAGE_NAME}.zip"
ZIP_TMP="${WORK_DIR}/${PACKAGE_NAME}.zip"

echo "Creating ZIP: ${ZIP_PATH}"
(
  cd "$WORK_DIR"
  zip -qr "$ZIP_TMP" "$PACKAGE_NAME"
)
cp "$ZIP_TMP" "$ZIP_PATH"

echo "Package created at: ${ZIP_PATH}"
echo "Staging directory left at: ${PACKAGE_DIR}"
