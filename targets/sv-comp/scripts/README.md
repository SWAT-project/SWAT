# SV-COMP CLI

Phase 1 restructuring completed - unified CLI interface for SV-COMP verification tasks.

## Quick Start

```bash
# Show all available commands
./svcomp --help

# Setup (first time only)
./svcomp setup all

# Run tests
./svcomp test run --mode parallel --workers 50

# Analyze results
./svcomp analyze stats
./svcomp analyze results
```

## New Structure

```
scripts/
├── svcomp              # Wrapper script (use this!)
├── svcomp.py           # Main CLI entry point
├── lib/                # Library modules (refactored from original scripts)
│   ├── __init__.py
│   ├── analysis.py     # (was analyse_ctx_loss.py)
│   ├── command_gen.py  # (was command_generation.py)
│   ├── comparison.py   # (was compare_results.py)
│   ├── dtypes.py       # (unchanged)
│   ├── execution.py    # (was target_execution.py)
│   ├── selection.py    # (was target_selection.py)
│   ├── utils.py        # (was util.py)
│   └── witness.py      # (was witness_validation.py)
└── commands/           # CLI command modules
    ├── __init__.py
    ├── analyze.py      # Analysis commands
    ├── setup.py        # Setup commands
    ├── test.py         # Test execution commands
    └── util.py         # Utility commands
```

## Available Commands

### Setup Commands

```bash
# Checkout SV-Benchmarks repository
./svcomp setup checkout-benchmarks

# Checkout wit4java witness validator
./svcomp setup checkout-validator

# Install Python dependencies
./svcomp setup install-deps

# Run all setup steps
./svcomp setup all
```

### Test Commands

```bash
# List all available test cases
./svcomp test list

# List with statistics
./svcomp test list --stats

# Run tests in parallel (default)
./svcomp test run --mode parallel --workers 50

# Run tests for specific categories
./svcomp test run --categories valid-assert.prp

# Run single target
./svcomp test run --mode single --target "autostub/Boolean_..."

# Check port availability
./svcomp test validate-ports
```

### Analysis Commands

```bash
# Analyze latest results for context losses
./svcomp analyze results

# Analyze specific results file
./svcomp analyze results path/to/results.json

# Compare two result files
./svcomp analyze compare results/old.json results/new.json

# Show test case statistics
./svcomp analyze stats
```

### Utility Commands

```bash
# Show configuration
./svcomp util config

# Check dependencies
./svcomp util check-deps

# Generate commands (for debugging)
./svcomp util generate-commands --limit 10

# Save commands to file
./svcomp util generate-commands --output commands.json
```

## Global Options

```bash
# Verbose logging
./svcomp -v test run

# Specify config file
./svcomp --config path/to/config.cfg test run
```

## Migration Notes

### Old Scripts (Still Available)

The original Python scripts are still in the `scripts/` directory and can be used directly if needed:
- `target_selection.py`
- `command_generation.py`
- `target_execution.py`
- `witness_validation.py`
- `analyse_ctx_loss.py`
- `compare_results.py`

However, **it's recommended to use the new CLI** for better user experience.

### Key Changes

1. **Unified Interface**: All functionality now accessible through a single `./svcomp` command
2. **Better Help**: Each command has comprehensive help text (`--help`)
3. **Modular Design**: Code is organized into `lib/` (library) and `commands/` (CLI)
4. **Relative Imports**: Library modules use relative imports for better packaging
5. **Wrapper Script**: `svcomp` wrapper automatically uses the venv Python

### Backwards Compatibility

- Old scripts still work as before
- Library functions are exposed through `lib/__init__.py`
- Can import and use functions programmatically:
  ```python
  from lib import extract_testcases, generate_commands, run_parallel
  ```

## Implementation Status

**Phase 1: ✅ Complete**
- ✅ CLI structure created with Click
- ✅ All existing functionality wrapped in CLI commands
- ✅ Library modules reorganized with proper imports
- ✅ Wrapper script for easy execution
- ✅ All commands tested and working

**Phase 2: 🔜 Future**
- [ ] Extract hardcoded configuration to config files
- [ ] Add progress bars for long-running operations
- [ ] Improve error messages and validation
- [ ] Add shell completion support

**Phase 3: 🔜 Future**
- [ ] Add deprecation warnings to old scripts
- [ ] Update documentation
- [ ] Consider removing old scripts

## Examples

### Complete Workflow

```bash
# First time setup
./svcomp setup all

# List available tests
./svcomp test list --stats

# Run tests for specific category
./svcomp test run --categories valid-assert.prp --workers 30

# Analyze results
./svcomp analyze results

# Compare with previous run
./svcomp analyze compare results/old.json results/new.json
```

### Development Workflow

```bash
# Check dependencies
./svcomp util check-deps

# Generate commands to inspect
./svcomp util generate-commands --limit 5

# Run single test for debugging
./svcomp test run --mode single --target "path/to/test"

# View configuration
./svcomp util config
```

## Troubleshooting

### "Virtual environment not found"

Run setup first:
```bash
python3 svcomp.py setup install-deps
```

### "Module not found" errors

Make sure you're using the wrapper script `./svcomp` which automatically uses the venv Python, not calling `svcomp.py` directly.

### Import errors

The CLI should be run from the `scripts/` directory. If running from elsewhere, you may need to adjust paths.

## Development

To add new commands:

1. Create or modify a file in `commands/`
2. Add a new `@group.command()` decorated function
3. Import necessary functions from `lib/`
4. The command will automatically be available in the CLI

Example:
```python
# In commands/test.py

@test.command(name='my-new-command')
@click.option('--my-option', help='Description')
@click.pass_context
def my_new_command(ctx, my_option):
    """Help text for the command."""
    from lib import some_function

    # Implementation
    some_function(my_option)
    click.secho("✓ Done!", fg='green')
```

Then use it:
```bash
./svcomp test my-new-command --my-option value
```
