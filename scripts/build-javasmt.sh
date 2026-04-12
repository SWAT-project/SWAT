#!/bin/bash

# Script to build JavaSMT from source and copy to libs directory
set -e  # Exit on any error

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Configuration
REPO_URL="https://github.com/SWAT-project/java-smt.git"
BRANCH_NAME="feature/z3-native-options"
TEMP_DIR="$SCRIPT_DIR/temp-javasmt-build"
TARGET_DIR="$SCRIPT_DIR/../libs/java-library-path"
JAR_NAME="java-smt-latest.jar"

echo "🔨 Building JavaSMT from source..."


# Check if Ant is installed
if ! command -v ant &> /dev/null
then
    echo "Ant is not installed. Please install it to continue (e.g., brew install ant)."
    exit 1
fi

# Create target directory if it doesn't exist
mkdir -p "$TARGET_DIR"

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

# Build the project
echo "🔧 Building JavaSMT..."
ant jar

# Find the generated JAR file (it has a version-specific name)
JAR_FILE=$(find . -name "java-smt-*.jar" -maxdepth 1 | head -n 1)

if [ -z "$JAR_FILE" ]; then
    echo "❌ Error: No java-smt JAR file found after build!"
    cd "$SCRIPT_DIR"
    rm -rf "$TEMP_DIR"
    exit 1
fi

echo "📦 Found JAR: $JAR_FILE"
echo "📦 Copying to $TARGET_DIR/$JAR_NAME..."
cp "$JAR_FILE" "$TARGET_DIR/$JAR_NAME"

# Navigate back to script directory
cd "$SCRIPT_DIR"

# Clean up temp directory
echo "🧹 Cleaning up temporary build directory..."
rm -rf "$TEMP_DIR"

echo "✅ JavaSMT build complete! JAR available at: $TARGET_DIR/$JAR_NAME"
echo ""
echo "💡 To use this JAR, update your build.gradle dependencies:"
echo "   implementation files('$TARGET_DIR/$JAR_NAME')"
echo "   // and remove or comment out: implementation libs.java.smt"