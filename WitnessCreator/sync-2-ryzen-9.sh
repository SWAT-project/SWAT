#!/bin/bash

cd "$(dirname "$0")"

# Remote server details
USERNAME="felix"
IP="141.83.162.115"

# Files to be uploaded
# Source directory
SRC_DIR="./"

# Destination directory in remote server
DEST_DIR="/mnt/ssd1/sv-comp/submission-container/WitnessCreator/"

# Upload all files and subdirectories to remote server
rsync -avz -e "ssh" --progress --recursive --include='**.gitignore' --filter=':- .gitignore' --exclude='.git' --delete-after "${SRC_DIR}" "${USERNAME}@${IP}:${DEST_DIR}"
