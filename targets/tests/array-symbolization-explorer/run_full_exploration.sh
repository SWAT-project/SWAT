#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get the root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"

echo "============================================================"
echo "Array Symbolization - Full Path Exploration Test"
echo "============================================================"
echo ""

# Step 1: Build the project
echo -e "${YELLOW}[1/3] Building project...${NC}"
cd "$ROOT_DIR"
./gradlew :targets:tests:array-symbolization-explorer:build :symbolic-executor:assemble -q
if [ $? -ne 0 ]; then
    echo -e "${RED}✗ Build failed${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Build successful${NC}"
echo ""

# Step 2: Clean logs
echo -e "${YELLOW}[2/3] Cleaning logs...${NC}"
rm -rf "$SCRIPT_DIR/logs"
mkdir -p "$SCRIPT_DIR/logs"
echo -e "${GREEN}✓ Logs cleaned${NC}"
echo ""

# Step 3: Run with SimpleDriver (explorer drives the exploration)
echo -e "${YELLOW}[3/3] Running symbolic exploration...${NC}"
cd "$ROOT_DIR/symbolic-explorer"

# Activate virtual environment
if [ ! -d ".venv" ]; then
    echo -e "${RED}✗ Virtual environment not found at symbolic-explorer/.venv${NC}"
    echo -e "${YELLOW}  Please create it first:${NC}"
    echo "  cd symbolic-explorer && python3 -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt"
    exit 1
fi

source .venv/bin/activate

# Run with SimpleDriver mode
python3 SymbolicExplorer.py \
    --mode simple \
    --target de.uzl.its.tests.arrays.SimpleArrayTest \
    --classpath "$ROOT_DIR/targets/tests/array-symbolization-explorer/build/libs/array-symbolization-explorer.jar" \
    --config "$ROOT_DIR/targets/tests/array-symbolization-explorer/swat.cfg" \
    --agent "$ROOT_DIR/symbolic-executor/lib/symbolic-executor.jar" \
    --z3dir "$ROOT_DIR/libs/java-library-path" \
    --logdir "$SCRIPT_DIR/logs" \
    --log-smt-formulas \
    --port 8078 \
    > "$SCRIPT_DIR/logs/explorer-output.log" 2>&1

EXPLORE_EXIT=$?

echo ""
echo "============================================================"
echo "Validating results..."
echo "============================================================"
echo ""

# Combine all logs for analysis
COMBINED_OUTPUT=$(cat "$SCRIPT_DIR/logs"/*.log 2>/dev/null)

# Check for all 3 paths being explored
PATH1_COUNT=$(echo "$COMBINED_OUTPUT" | grep -c "Found path 1: arr\[0\]==42 && arr\[1\]==10")
PATH2_COUNT=$(echo "$COMBINED_OUTPUT" | grep -c "Found path 2: arr\[0\]>100")
PATH3_COUNT=$(echo "$COMBINED_OUTPUT" | grep -c "Found path 3: default")

# Check exploration completed
EXPLORATION_COMPLETE=$(echo "$COMBINED_OUTPUT" | grep -c "All reachable paths explored\|EXPLORATION COMPLETE")

echo "Results:"
echo "  Path 1 (arr[0]==42 && arr[1]==10):  $    occurrence(s)"
echo "  Path 2 (arr[0]>100):                $PATH2_COUNT occurrence(s)"
echo "  Path 3 (default):                   $PATH3_COUNT occurrence(s)"
echo "  Exploration completed:              $EXPLORATION_COMPLETE"
echo ""

# Validation
FAILED=0

if [ $PATH1_COUNT -eq 0 ]; then
    echo -e "${RED}✗ Path 1 not explored${NC}"
    FAILED=1
else
    echo -e "${GREEN}✓ Path 1 explored${NC}"
fi

if [ $PATH2_COUNT -eq 0 ]; then
    echo -e "${RED}✗ Path 2 not explored${NC}"
    FAILED=1
else
    echo -e "${GREEN}✓ Path 2 explored${NC}"
fi

if [ $PATH3_COUNT -eq 0 ]; then
    echo -e "${RED}✗ Path 3 not explored${NC}"
    FAILED=1
else
    echo -e "${GREEN}✓ Path 3 explored${NC}"
fi

if [ $EXPLORATION_COMPLETE -eq 0 ]; then
    echo -e "${YELLOW}⚠ Warning: Exploration may not have completed normally${NC}"
fi

echo ""
echo "============================================================"
if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ TEST PASSED - All paths explored!${NC}"
    echo "============================================================"
    exit 0
else
    echo -e "${RED}✗ TEST FAILED - Not all paths explored${NC}"
    echo "============================================================"
    echo ""
    echo "Check logs at: $SCRIPT_DIR/logs/"
    echo ""
    echo "Recent log output:"
    tail -50 "$SCRIPT_DIR/logs/explorer-output.log"
    exit 1
fi
