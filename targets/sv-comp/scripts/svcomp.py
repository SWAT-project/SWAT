#!/usr/bin/env python3
"""
SV-COMP CLI - Unified command-line interface for SV-COMP verification tasks.

This tool provides commands for setting up, running, and analyzing
SV-COMP verification benchmarks.
"""

import click
import logging
from pathlib import Path

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Get script directory
SCRIPT_DIR = Path(__file__).resolve().parent


@click.group()
@click.option('--verbose', '-v', is_flag=True, help='Enable verbose logging')
@click.option('--config', type=click.Path(exists=True), help='Path to config file')
@click.pass_context
def cli(ctx, verbose, config):
    """SV-COMP CLI - Tools for symbolic verification benchmarks."""
    # Ensure context object exists
    ctx.ensure_object(dict)

    # Set logging level
    if verbose:
        logging.getLogger().setLevel(logging.DEBUG)
        logger.debug("Verbose logging enabled")

    # Store config in context for subcommands to access
    ctx.obj['config'] = config
    ctx.obj['script_dir'] = SCRIPT_DIR


# Import command groups (will be created next)
from commands import setup, test, analyze, util

# Register command groups
cli.add_command(setup.setup)
cli.add_command(test.test)
cli.add_command(analyze.analyze)
cli.add_command(util.util)


if __name__ == '__main__':
    cli(obj={})
