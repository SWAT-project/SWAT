#!/bin/bash

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Define variables
REPO_URL="git@github.com:SWAT-project/SV-Benchmarks.git"
TARGET_DIR="$SCRIPT_DIR/../sv-benchmarks"
FOLDER="java"

# Step 1: Create the target directory and initialize an empty Git repository
mkdir -p "$TARGET_DIR"
cd "$TARGET_DIR" || exit
git init

# Step 2: Add the remote repository
git remote add origin "$REPO_URL"

# Step 3: Enable sparse checkout
git config core.sparseCheckout true

# Step 4: Specify the folder to checkout
echo "$FOLDER/" >> .git/info/sparse-checkout

echo "Pulling... This may take some time"
# Step 5: Pull only the specified folder from the main branch
git pull --depth=1 origin main

echo "Checked out '$FOLDER/' folder from $REPO_URL into $TARGET_DIR"
