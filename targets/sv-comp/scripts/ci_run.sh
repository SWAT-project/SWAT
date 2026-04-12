#!/bin/bash

# Name of the virtual environment directory
VENV_DIR="venv"
echo "::group::Prepare dependencies..."
# Check if the virtual environment directory exists
if [ ! -d "$VENV_DIR" ]; then
  echo "Creating virtual environment..."
  python3 -m venv $VENV_DIR
fi

# Activate the virtual environment (for Bash)
source $VENV_DIR/bin/activate

# Now we're inside the virtual environment – run Python commands or scripts here

echo "Installing dependencies..."
pip3 install -r requirements.txt


echo "remove .github directory"
rm -rf ../sv-benchmarks/.github
echo "::endgroup::"

echo "Running Tests..."
python3 target_execution.py

# Deactivate the virtual environment (optional)
deactivate
echo "Virtual environment deactivated."