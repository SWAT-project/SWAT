
set -euo pipefail

# Function to display help
show_help() {
    cat << EOF
Symbolic Explorer Wrapper Script
===============================

Runs SymbolicExplorer.py for each target URL in targets.txt with timeout support.

Usage: $0 [OPTIONS]

Options:
  -t, --timeout SECONDS    Set timeout in seconds (default: 300 = 5 minutes)
  -h, --help              Show this help message
  
Environment Variables:
  SYMBOLIC_EXPLORER_HOST        Target host (default: localhost)
  SYMBOLIC_EXPLORER_HTTP_PORT   Target HTTP port (default: 8080)
  SYMBOLIC_EXPLORER_PORT        Explorer port (default: 8078)
  SYMBOLIC_EXPLORER_METHOD      HTTP method (default: GET)
  SYMBOLIC_EXPLORER_TIMEOUT     Timeout in seconds (default: 300)

Examples:
  $0                           # Run with default 5-minute timeout
  $0 -t 600                    # Run with 10-minute timeout
  SYMBOLIC_EXPLORER_TIMEOUT=180 $0  # Run with 3-minute timeout via env var

EOF
}

# Parse command line arguments
while [ $# -gt 0 ]; do
    case $1 in
        -t|--timeout)
            TIMEOUT_OVERRIDE="$2"
            shift 2
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use -h or --help for usage information"
            exit 1
            ;;
    esac
done

echo "Symbolic Explorer Wrapper Script"
echo "==============================="

# Get the directory of this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "Script directory: $SCRIPT_DIR"

# Define paths relative to script location
SYMBOLIC_EXPLORER_DIR="$SCRIPT_DIR/../../../../symbolic-explorer"
TARGETS_FILE="$SCRIPT_DIR/targets.txt"
LOGS_DIR="$SCRIPT_DIR/../../../../logs"

echo "Symbolic Explorer directory: $SYMBOLIC_EXPLORER_DIR"
echo "Targets file: $TARGETS_FILE"
echo "Logs directory: $LOGS_DIR"
echo

# Check if directories and files exist
if [[ ! -d "$SYMBOLIC_EXPLORER_DIR" ]]; then
    echo "Error: Symbolic explorer directory not found: $SYMBOLIC_EXPLORER_DIR"
    exit 1
fi

if [[ ! -f "$TARGETS_FILE" ]]; then
    echo "Error: Targets file not found: $TARGETS_FILE"
    exit 1
fi

if [[ ! -f "$SYMBOLIC_EXPLORER_DIR/SymbolicExplorer.py" ]]; then
    echo "Error: SymbolicExplorer.py not found in: $SYMBOLIC_EXPLORER_DIR"
    exit 1
fi

# Create logs directory if it doesn't exist
mkdir -p "$LOGS_DIR"

# Configuration
HOST=${SYMBOLIC_EXPLORER_HOST:-localhost}
HTTP_PORT=${SYMBOLIC_EXPLORER_HTTP_PORT:-8080}
EXPLORER_PORT=${SYMBOLIC_EXPLORER_PORT:-8078}
HTTP_METHOD=${SYMBOLIC_EXPLORER_METHOD:-GET}
TIMEOUT_SECONDS=${TIMEOUT_OVERRIDE:-${SYMBOLIC_EXPLORER_TIMEOUT:-300}}  # Command line override, then env var, then default 5 minutes

echo "Configuration:"
echo "  Host: $HOST"
echo "  HTTP Port: $HTTP_PORT"
echo "  Explorer Port: $EXPLORER_PORT"
echo "  HTTP Method: $HTTP_METHOD"
echo "  Timeout: ${TIMEOUT_SECONDS}s ($(($TIMEOUT_SECONDS / 60)) minutes)"
echo

# Read targets and process each one
cnt=1
total_targets=$(wc -l < "$TARGETS_FILE" | tr -d ' ')

echo "Found $total_targets target(s) in $TARGETS_FILE"
echo

while IFS= read -r target; do
    # Skip empty lines
    if [[ -z "$target" ]]; then
        continue
    fi

    # Remove leading/trailing whitespace
    target=$(echo "$target" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
    
    echo "----------------------------------------"
    echo "Processing target $cnt/$total_targets: $target"
    echo "----------------------------------------"
    
    # Define log file for this target
    log_file="$LOGS_DIR/symbolic-explorer_$cnt.log"
    
    echo "Log file: $log_file"
    echo "Starting symbolic exploration..."
    echo
    
    # Change to symbolic explorer directory and run the command
    cd "$SYMBOLIC_EXPLORER_DIR"
    
    # Build the command
    cmd="python3 SymbolicExplorer.py --m http --host $HOST --http-port $HTTP_PORT -u \"$target\" --http-method $HTTP_METHOD -p $EXPLORER_PORT"
    
    echo "Command: $cmd"
    echo "Working directory: $(pwd)"
    echo
    
    # Run the command with timeout
    echo "Starting exploration with timeout of ${TIMEOUT_SECONDS}s..."
    
    # Start the python process in background
    python3 SymbolicExplorer.py \
        --m http \
        --host "$HOST" \
        --http-port "$HTTP_PORT" \
        -u "$target" \
        --http-method "$HTTP_METHOD" \
        -p "$EXPLORER_PORT" \
        > "$log_file" 2>&1 &
    
    # Get the PID of the background process
    explorer_pid=$!
    
    # Start timeout timer in background
    (
        sleep $TIMEOUT_SECONDS
        if kill -0 $explorer_pid 2>/dev/null; then
            echo "⚠️  Timeout reached (${TIMEOUT_SECONDS}s), killing process $explorer_pid" >> "$log_file"
            kill -TERM $explorer_pid 2>/dev/null || true
            sleep 2
            kill -KILL $explorer_pid 2>/dev/null || true
        fi
    ) &
    timeout_pid=$!
    
    # Wait for the python process to complete
    if wait $explorer_pid; then
        # Process completed successfully, kill the timeout timer
        kill $timeout_pid 2>/dev/null || true
        echo "✅ Successfully completed exploration for target: $target"
        echo "   Log saved to: $log_file"
    else
        exit_code=$?
        # Kill the timeout timer
        kill $timeout_pid 2>/dev/null || true
        
        if [[ $exit_code -eq 143 || $exit_code -eq 137 ]]; then
            echo "⏰ Exploration timed out after ${TIMEOUT_SECONDS}s for target: $target"
            echo "   Partial log saved to: $log_file"
        else
            echo "❌ Failed to explore target: $target (exit code: $exit_code)"
            echo "   Check log file for details: $log_file"
        fi
        # Continue with next target instead of exiting
    fi
    
    echo
    ((cnt++))
    
done < "$TARGETS_FILE"

echo "========================================="
echo "Symbolic exploration completed!"
echo "========================================="
echo
echo "Summary:"
echo "  Total targets processed: $((cnt-1))"
echo "  Log files location: $LOGS_DIR"
echo "  Log files pattern: symbolic-explorer_*.log"
echo
echo "Log files created:"
ls -la "$LOGS_DIR"/symbolic-explorer_*.log 2>/dev/null || echo "  (No log files found - check for errors above)"

echo
echo "To view a specific log:"
echo "  cat $LOGS_DIR/symbolic-explorer_1.log"
echo
echo "To view all logs:"
echo "  tail -f $LOGS_DIR/symbolic-explorer_*.log"
