#!/bin/bash

# Builds JavaSMT from source and installs it into the local Maven repository
# libs/maven, from which symbolic-executor resolves org.sosy-lab:java-smt
# (version in settings.gradle).
#
# The branch is the JavaSMT release SWAT uses plus the changes needed for the
# vendored Z3 version. It is compiled against the Java bindings of that Z3
# (com.microsoft.z3.jar from the vendored Linux Z3 distribution in libs/), so
# the bytecode matches the libz3java the executor loads at run time.
set -e  # Exit on any error

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$SCRIPT_DIR/.."

# Configuration (JAVASMT_REPO_URL: build from another clone, e.g. a local one)
REPO_URL="${JAVASMT_REPO_URL:-https://github.com/SWAT-project/java-smt.git}"
BRANCH_NAME="swat/6.0.0-z3-5"
BASE_RELEASE="6.0.0" # the JavaSMT release the branch is based on; its POM lists the dependencies
Z3_DIST="z3-5.3.1-x64-glibc-2.39"
TEMP_DIR="$SCRIPT_DIR/temp-javasmt-build"
MAVEN_DIR="$ROOT_DIR/libs/maven/org/sosy-lab/java-smt"

echo "🔨 Building JavaSMT from source..."

# Check if Ant is installed
if ! command -v ant &> /dev/null
then
    echo "Ant is not installed. Please install it to continue (e.g., brew install ant)."
    exit 1
fi

Z3_ZIP="$ROOT_DIR/libs/${Z3_DIST}.zip"
if [ ! -f "$Z3_ZIP" ]; then
    echo "❌ Error: Z3 distribution not found at $Z3_ZIP"
    exit 1
fi

# Clean up any existing temp directory
if [ -d "$TEMP_DIR" ]; then
    echo "🧹 Cleaning up existing temp directory..."
    rm -rf "$TEMP_DIR"
fi

# Clone the repository
echo "📥 Cloning JavaSMT repository..."
git clone "$REPO_URL" "$TEMP_DIR"

# Navigate to the cloned directory
cd "$TEMP_DIR"

# Checkout the correct branch
echo "Checking out $BRANCH_NAME..."
git checkout "$BRANCH_NAME"
VERSION="$(git describe --tags)"

# Fetch the dependencies, then compile against the vendored Z3 Java bindings
# instead of the ones Ivy fetched (ivy.disable keeps Ivy from restoring them)
echo "🔧 Building JavaSMT $VERSION..."
ant resolve-dependencies
unzip -p "$Z3_ZIP" "${Z3_DIST}/java/com.microsoft.z3.jar" > lib/java/runtime-z3/com.microsoft.z3.jar
ant -Divy.disable=true -Divy.module=java-smt jar

JAR_FILE="java-smt-${VERSION}.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ Error: $JAR_FILE not found after build!"
    cd "$SCRIPT_DIR"
    rm -rf "$TEMP_DIR"
    exit 1
fi

TARGET_DIR="$MAVEN_DIR/$VERSION"
echo "📦 Installing $JAR_FILE into $TARGET_DIR..."
mkdir -p "$TARGET_DIR"
cp "$JAR_FILE" "$TARGET_DIR/"
curl -fsSL "https://repo1.maven.org/maven2/org/sosy-lab/java-smt/${BASE_RELEASE}/java-smt-${BASE_RELEASE}.pom" \
    | sed "0,/<version>${BASE_RELEASE}<\/version>/s//<version>${VERSION}<\/version>/" \
    > "$TARGET_DIR/java-smt-${VERSION}.pom"

# Navigate back to script directory
cd "$SCRIPT_DIR"

# Clean up temp directory
echo "🧹 Cleaning up temporary build directory..."
rm -rf "$TEMP_DIR"

echo "✅ JavaSMT build complete: $TARGET_DIR"
echo ""
echo "💡 To use it, set the java-smt version in settings.gradle to $VERSION"
