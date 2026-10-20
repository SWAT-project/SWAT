#!/usr/bin/env bash
set -euo pipefail

# This script packages artifacts that were already produced by CI. It must not
# invoke Gradle, create virtual environments, or download dependencies.
#
# Generated files are expected in the repository checkout after the CI build.
# Set SWAT_SVCOMP_ARTIFACT_DIR only if those generated files live somewhere
# else. The artifact root must contain:
#
# - symbolic-executor/lib/symbolic-executor.jar, or symbolic-executor.jar at the
#   root (actions/upload-artifact drops the directories of a single-file upload)
#
# Set SWAT_SVCOMP_PYTHON_DIR to the Python dependency directory produced by
# scripts/build-svcomp-python-runtime.sh (it includes the witnesscreator
# package that writes the witnesses). It is shipped as libs/python/ and used
# with the competition machine's system python3 (no bundled venv).
#
# Z3 and JavaSMT are taken from the vendored files in this repository (libs/).
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
PYTHON_DIR="${SWAT_SVCOMP_PYTHON_DIR:-}"
LINUX_Z3_DIST="z3-5.3.1-x64-glibc-2.39"
LINUX_Z3_ZIP="${ROOT_DIR}/libs/${LINUX_Z3_DIST}.zip"
JAVA_SMT_DIR="${ROOT_DIR}/libs/maven/org/sosy-lab/java-smt"

if [[ -n "$PYTHON_DIR" ]]; then
  PYTHON_DIR="$(cd "$PYTHON_DIR" && pwd)"
fi

fail() {
  echo "error: $*" >&2
  exit 1
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

# Installs <path> of the vendored Linux Z3 distribution (relative to its root) into
# libs/java-library-path/ under its file name.
install_z3_runtime_file() {
  local path="$1"
  local mode="${2:-0644}"
  local dest="${PACKAGE_DIR}/libs/java-library-path/$(basename "$path")"

  [[ -f "$LINUX_Z3_ZIP" ]] || fail "missing vendored Linux Z3 distribution: ${LINUX_Z3_ZIP}"
  mkdir -p "$(dirname "$dest")"
  unzip -p "$LINUX_Z3_ZIP" "${LINUX_Z3_DIST}/${path}" > "$dest"
  [[ -s "$dest" ]] || fail "missing ${path} in ${LINUX_Z3_ZIP}"
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
install -m 0644 "$SUPPORT_DIR/requirements.txt" "$PACKAGE_DIR/requirements.txt"
install -m 0644 targets/sv-comp/sv-comp.cfg "$PACKAGE_DIR/sv-comp.cfg"

EXECUTOR_JAR="$(first_artifact_file \
  symbolic-executor/lib/symbolic-executor.jar \
  symbolic-executor.jar \
  symbolic-executor/build/libs/symbolic-executor-all.jar)"
copy_artifact_file "$EXECUTOR_JAR" "$PACKAGE_DIR/symbolic-executor/lib/symbolic-executor.jar"

copy_tree_files symbolic-explorer "$PACKAGE_DIR/symbolic-explorer"

install_z3_runtime_file bin/z3 0755
install_z3_runtime_file bin/libz3.so
install_z3_runtime_file bin/libz3java.so
install_z3_runtime_file java/com.microsoft.z3.jar
# The JavaSMT build the executor uses (scripts/build-javasmt.sh); it is shaded into the executor JAR
# and shipped next to it for reference.
JAVA_SMT_JARS=("$JAVA_SMT_DIR"/*/java-smt-*.jar)
[[ ${#JAVA_SMT_JARS[@]} -eq 1 && -f "${JAVA_SMT_JARS[0]}" ]] || fail "expected one JavaSMT JAR in ${JAVA_SMT_DIR}, found: ${JAVA_SMT_JARS[*]}"
JAVA_SMT_JAR="${JAVA_SMT_JARS[0]}"
copy_artifact_file "$JAVA_SMT_JAR" "$PACKAGE_DIR/libs/java-library-path/java-smt-latest.jar"

copy_tree_files "$SUPPORT_DIR/smoketest" "$PACKAGE_DIR/smoketest"

[[ -n "$PYTHON_DIR" ]] || fail "SWAT_SVCOMP_PYTHON_DIR must point to the output of scripts/build-svcomp-python-runtime.sh"
[[ -f "$PYTHON_DIR/PYTHON_VERSION" ]] || fail "missing ${PYTHON_DIR}/PYTHON_VERSION; build it with scripts/build-svcomp-python-runtime.sh"
[[ -d "$PYTHON_DIR/witnesscreator" ]] || fail "missing witnesscreator package in ${PYTHON_DIR}; build it with scripts/build-svcomp-python-runtime.sh"
copy_artifact_tree "$PYTHON_DIR" "${PACKAGE_DIR}/libs/python"

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
