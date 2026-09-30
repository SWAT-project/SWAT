#!/bin/bash
#
# Checks out the SV-COMP Java benchmarks (with precompiled .class files) from SWAT's
# SV-Benchmarks repository into ../sv-benchmarks: a shallow, sparse checkout of java/ and of
# .last_sv_commit, the upstream sv-benchmarks commit they were built from.
#
# Usage: checkout.sh [SOURCE]
#   SOURCE  where to pull from, default git@github.com:SWAT-project/SV-Benchmarks.git; e.g. a local
#           clone (/path/to/SV-Benchmarks) when GitHub is not reachable over SSH. origin is set to
#           the default URL afterwards either way.
#
# An existing ../sv-benchmarks is never overwritten. To update, move it aside first, e.g. to
# ../sv-benchmarks-<date of its commit>, so old runs can still be rerun against it (--benchmark-dir).

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Define variables
REPO_URL="git@github.com:SWAT-project/SV-Benchmarks.git"
SOURCE="${1:-$REPO_URL}"
TARGET_DIR="$SCRIPT_DIR/../sv-benchmarks"
FOLDER="java"

if [ -e "$TARGET_DIR" ]; then
  echo "Error: $TARGET_DIR already exists. Move it aside first (see the header of this script)."
  exit 1
fi

# A local path is pulled over file:// so that --depth works.
if [ -d "$SOURCE" ]; then
  SOURCE="file://$(cd "$SOURCE" && pwd)"
fi

# Step 1: Create the target directory and initialize an empty Git repository
mkdir -p "$TARGET_DIR"
cd "$TARGET_DIR" || exit
git init

# Step 2: Add the remote repository
git remote add origin "$SOURCE"

# Step 3: Enable sparse checkout
git config core.sparseCheckout true

# Step 4: Specify what to check out
echo "$FOLDER/" >> .git/info/sparse-checkout
echo "/.last_sv_commit" >> .git/info/sparse-checkout

echo "Pulling... This may take some time"
# Step 5: Pull only the specified folder from the main branch
git pull --depth=1 origin main || exit 1

# Later pulls go to the canonical repository, not to wherever this checkout came from.
git remote set-url origin "$REPO_URL"

echo "Checked out '$FOLDER/' folder from $SOURCE into $TARGET_DIR"
echo "Upstream sv-benchmarks commit: $(cat .last_sv_commit 2>/dev/null || echo unknown)"
