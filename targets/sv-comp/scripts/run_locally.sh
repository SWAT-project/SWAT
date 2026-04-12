#!/bin/bash

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Name of the virtual environment directory
VENV_DIR="$SCRIPT_DIR/.venv"

# Check if the virtual environment directory exists
if [ ! -d "$VENV_DIR" ]; then
  echo "Creating virtual environment..."
  python3 -m venv $VENV_DIR
fi

# Activate the virtual environment (for Bash)
source $VENV_DIR/bin/activate

# Now we're inside the virtual environment – run Python commands or scripts here
echo "Virtual environment activated."
echo "Installing dependencies..."
pip3 install -r "$SCRIPT_DIR/requirements.txt"

echo "Clone target Repository"
"$SCRIPT_DIR/checkout.sh"

echo "Running Tests..."
python3 "$SCRIPT_DIR/target_execution.py"

# Deactivate the virtual environment (optional)
deactivate
echo "Virtual environment deactivated."