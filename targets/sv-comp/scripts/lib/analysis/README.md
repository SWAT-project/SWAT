# Analysis Submodule

Comprehensive analysis utilities for SV-COMP test execution results.

## Structure

```
analysis/
├── __init__.py          # Module exports and run_all_analyses()
├── timing.py            # Timing analysis and per-testcase averages
├── failures.py          # Failure root cause analysis
├── performance.py       # Performance bottleneck identification
├── context_loss.py      # Context loss method extraction and analysis
└── README.md           # This file
```

## Modules

### `timing.py` - TimingAnalysis
Aggregates timing data from all `timing_data.json` files and computes:
- **Average time per testcase** for each module (executor, solver, explorer, witness)
- Min/max/total statistics across all testcases
- Percentage breakdown

**Usage:**
```python
from lib.analysis.timing import TimingAnalysis

# Print timing statistics for all testcases
TimingAnalysis.print_timing_statistics()

# Or with custom log directory
TimingAnalysis.print_timing_statistics(Path('/path/to/logs'))

# Get raw statistics dictionary
stats = TimingAnalysis.aggregate_timing_from_files()
```

**Output Example:**
```
======================================================================
TIMING ANALYSIS - AGGREGATE STATISTICS
======================================================================
Total testcases analyzed: 150

Average time per testcase (seconds):
  Symbolic Executor        42.15s ( 78.5%)
  SMT Solver                8.32s ( 15.5%)
  Symbolic Explorer         2.14s (  4.0%)
  Witness Generation        0.85s (  1.6%)
  Witness Validation        0.21s (  0.4%)
  Total per testcase:      53.67s
...
```

### `failures.py` - FailureAnalysis
Analyzes log files to identify root causes of failures:
- Symbolic context loss
- Symbolic precision loss
- Uncaught exceptions
- Reference semantic changes
- Internal SWAT errors

**Usage:**
```python
from lib.analysis.failures import FailureAnalysis

# Print failure statistics
FailureAnalysis.print_failure_statistics()

# Get raw failure data
stats = FailureAnalysis.analyze_failures()
```

**Output Example:**
```
======================================================================
FAILURE ANALYSIS - ROOT CAUSE BREAKDOWN
======================================================================
Total testcases analyzed: 150

Symbolic Context Loss            45 ( 30.0%)
Symbolic Precision Loss          12 (  8.0%)
Uncaught Exceptions              23 ( 15.3%)
Reference Semantic Change         8 (  5.3%)
Internal SWAT Errors              2 (  1.3%)
...
```

### `performance.py` - PerformanceAnalysis
Identifies performance bottlenecks and optimization targets:
- Find slowest testcases
- Detailed timing breakdown for each

**Usage:**
```python
from lib.analysis.performance import PerformanceAnalysis

# Print top 10 slowest testcases
PerformanceAnalysis.print_slowest_testcases(top_n=10)

# Get slowest testcases data
slowest = PerformanceAnalysis.find_slowest_testcases(top_n=20)
```

**Output Example:**
```
======================================================================
TOP 10 SLOWEST TESTCASES
======================================================================

1. algorithms/sorting/bubble_sort_valid-assert
   Total time: 245.32s
   Breakdown:
     - Symbolic Executor:   210.45s
     - SMT Solver:           28.12s
     - Symbolic Explorer:     4.35s
     - Witness Generation:    1.95s
     - Witness Validation:    0.45s
...
```

## Running All Analyses

For a comprehensive report, use the convenience function:

```python
from lib.analysis import run_all_analyses

# Run all analyses and print comprehensive report
run_all_analyses()

# Or with custom log directory
run_all_analyses(Path('/path/to/logs'))
```

This will run timing, performance, and failure analyses in sequence.

## Integration

The analysis module is automatically integrated into the test execution pipeline:
- `TimingAnalysis.print_timing_statistics()` is called after parallel test execution
- Results are logged to the standard logger

## Extensibility

To add new analysis capabilities:

1. Create a new module (e.g., `coverage.py`)
2. Create an analysis class with static methods
3. Follow the pattern:
   - `collect_*()` methods to gather data
   - `analyze_*()` methods to process data
   - `print_*()` methods to display results
4. Export from `__init__.py`
5. Add to `run_all_analyses()` if appropriate

**Example:**
```python
# lib/analysis/memory.py
class MemoryAnalysis:
    @staticmethod
    def collect_memory_stats(log_base_dir=None):
        # Collect memory usage data
        pass

    @staticmethod
    def analyze_memory_usage(log_base_dir=None):
        # Analyze memory patterns
        pass

    @staticmethod
    def print_memory_statistics(log_base_dir=None):
        # Print analysis results
        pass
```

## Notes

- All analysis modules use the same log directory structure
- Timing data must be in `timing_data.json` format (created by TimingManager)
- Log files are searched recursively from the log base directory
- Analysis is read-only and does not modify any files
