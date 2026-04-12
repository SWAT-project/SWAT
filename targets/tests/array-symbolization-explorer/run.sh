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
echo "Array Symbolization Explorer Integration Test"
echo "============================================================"
echo ""

# Step 1: Build the project
echo -e "${YELLOW}[1/5] Building project...${NC}"
cd "$ROOT_DIR"
./gradlew :targets:tests:array-symbolization-explorer:build :symbolic-executor:assemble -q
if [ $? -ne 0 ]; then
    echo -e "${RED}✗ Build failed${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Build successful${NC}"
echo ""

# Step 2: Clean logs
echo -e "${YELLOW}[2/5] Cleaning logs...${NC}"
rm -rf "$SCRIPT_DIR/logs"
mkdir -p "$SCRIPT_DIR/logs"
echo -e "${GREEN}✓ Logs cleaned${NC}"
echo ""

# Step 3: Start the symbolic explorer in the background
echo -e "${YELLOW}[3/5] Starting symbolic explorer...${NC}"
cd "$ROOT_DIR/symbolic-explorer"

# Activate virtual environment
if [ ! -d ".venv" ]; then
    echo -e "${RED}✗ Virtual environment not found at symbolic-explorer/.venv${NC}"
    echo -e "${YELLOW}  Please create it first:${NC}"
    echo "  cd symbolic-explorer && python3 -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt"
    exit 1
fi

source .venv/bin/activate
EXPLORER_LOG="$SCRIPT_DIR/logs/explorer.log"

# Start explorer in passive mode (just as a server)
python3 SymbolicExplorer.py --mode passive --port 8078 > "$EXPLORER_LOG" 2>&1 &
EXPLORER_PID=$!
echo -e "${GREEN}✓ Explorer started (PID: $EXPLORER_PID)${NC}"
echo ""

# Wait for explorer to start
sleep 3

# Step 4: Run Java once to test array symbolization
echo -e "${YELLOW}[4/5] Running Java with array symbolization...${NC}"

JAVA_LOG="$SCRIPT_DIR/logs/java-output.log"
cd "$ROOT_DIR"

java -Xmx4g \
  -Dconfig.path=targets/tests/array-symbolization-explorer/swat.cfg \
  -javaagent:symbolic-executor/lib/symbolic-executor.jar \
  -Djava.library.path=libs/java-library-path \
  -cp targets/tests/array-symbolization-explorer/build/libs/array-symbolization-explorer.jar \
  de.uzl.its.tests.arrays.SimpleArrayTest > "$JAVA_LOG" 2>&1

JAVA_EXIT=$?
if [ $JAVA_EXIT -ne 0 ]; then
    echo -e "${RED}✗ Java execution failed${NC}"
    cat "$JAVA_LOG"
    kill $EXPLORER_PID 2>/dev/null
    exit 1
fi

echo -e "${GREEN}✓ Java execution completed${NC}"

# Give explorer a moment to process
sleep 2

# Stop explorer
echo -e "${YELLOW}Stopping explorer...${NC}"
kill $EXPLORER_PID 2>/dev/null
wait $EXPLORER_PID 2>/dev/null
echo ""

# Step 5: Validate results
echo -e "${YELLOW}[5/5] Validating results...${NC}"

# Check for array symbolization evidence in all logs
COMBINED_OUTPUT=$(cat "$EXPLORER_LOG" "$SCRIPT_DIR/logs"/*.log 2>/dev/null)

# Check for symbolic array initialization
ARRAY_INIT=$(echo "$COMBINED_OUTPUT" | grep -c "Initializing symbolic tracking for int\[\]")

# Check for array registration
ARRAY_REGISTER=$(echo "$COMBINED_OUTPUT" | grep -c "Input with name: \[I")

# Check for constraints sent to explorer
CONSTRAINTS_SENT=$(echo "$COMBINED_OUTPUT" | grep -c "Accepted\|constraints")

# Check that Java test actually ran
TEST_RAN=$(echo "$COMBINED_OUTPUT" | grep -c "Testing int array symbolization")

echo ""
echo "Results:"
echo "  Array initialization detected:  $ARRAY_INIT"
echo "  Array registration detected:    $ARRAY_REGISTER"
echo "  Constraints communication:       $CONSTRAINTS_SENT"
echo "  Test execution verified:        $TEST_RAN"
echo ""

# Validation
FAILED=0

if [ $TEST_RAN -eq 0 ]; then
    echo -e "${RED}✗ Test did not run${NC}"
    FAILED=1
else
    echo -e "${GREEN}✓ Test executed${NC}"
fi

if [ $ARRAY_INIT -eq 0 ]; then
    echo -e "${RED}✗ Array symbolic initialization not detected${NC}"
    FAILED=1
else
    echo -e "${GREEN}✓ Array symbolic initialization working${NC}"
fi

if [ $ARRAY_REGISTER -eq 0 ]; then
    echo -e "${YELLOW}⚠ Warning: Array registration not detected in logs${NC}"
else
    echo -e "${GREEN}✓ Array registration detected${NC}"
fi

echo ""
echo "============================================================"
if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ TEST PASSED - Array symbolization working!${NC}"
    echo "============================================================"
    echo ""
    echo "NOTE: This test verifies that arrays can be made symbolic"
    echo "and work with the explorer. Full path exploration requires"
    echo "implementing the complete executor-explorer loop."
    exit 0
else
    echo -e "${RED}✗ TEST FAILED - Array symbolization not working${NC}"
    echo "============================================================"
    echo ""
    echo "Check logs at: $SCRIPT_DIR/logs/"
    exit 1
fi
