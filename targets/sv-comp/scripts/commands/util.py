"""Utility commands for SV-COMP."""

import click
import logging
import json
from pathlib import Path

logger = logging.getLogger(__name__)


@click.group(name='util')
def util():
    """Utility commands."""
    pass


@util.command(name='generate-commands')
@click.option('--benchmark-dir', type=click.Path(exists=True), help='Path to sv-benchmarks directory')
@click.option('--config-file', default='sv-comp.cfg', help='Configuration file name')
@click.option('--output', type=click.Path(), help='Output file for commands (JSON)')
@click.option('--limit', type=int, help='Limit number of commands to generate')
@click.pass_context
def generate_commands_cmd(ctx, benchmark_dir, config_file, output, limit):
    """Generate commands without running them.

    Useful for debugging or inspecting what would be executed.
    """
    from lib import extract_testcases, generate_commands

    script_dir = ctx.obj['script_dir']

    # Determine benchmark directory
    if benchmark_dir:
        test_dir = Path(benchmark_dir)
    else:
        test_dir = script_dir.parent / 'sv-benchmarks'

    if not test_dir.exists():
        click.echo(f"Error: Benchmark directory not found at {test_dir}", err=True)
        ctx.exit(1)

    try:
        ver_tasks = extract_testcases(test_dir)
        click.echo(f"Extracted {len(ver_tasks)} test cases")

        if limit:
            ver_tasks = ver_tasks[:limit]
            click.echo(f"Limited to {len(ver_tasks)} test cases")

        ver_tasks_with_commands = generate_commands(ver_tasks, config_file)
        click.echo(f"Generated {len(ver_tasks_with_commands)} commands")

        if output:
            # Save to file
            output_path = Path(output)
            commands_data = []
            for task in ver_tasks_with_commands:
                commands_data.append({
                    'target': str(task['command']['target']),
                    'command': task['command']['command'],
                    'log_dir': str(task['command']['log_dir']),
                    'category': task['category'].value,
                    'verdict': task['verdict'].value,
                })

            with open(output_path, 'w') as f:
                json.dump(commands_data, f, indent=2)

            click.secho(f"✓ Commands saved to {output_path}", fg='green')
        else:
            # Print first 3 commands
            click.echo("\nFirst 3 commands:")
            for i, task in enumerate(ver_tasks_with_commands[:3], 1):
                click.echo(f"\nCommand {i}:")
                click.echo(f"  Target: {task['command']['target']}")
                click.echo(f"  Category: {task['category'].value}")
                click.echo(f"  Log dir: {task['command']['log_dir']}")
                click.echo(f"  Command: {' '.join(task['command']['command'])}")

    except click.exceptions.Exit:
        # Re-raise Exit exceptions (from ctx.exit calls) without logging
        raise
    except Exception as e:
        click.echo(f"Error generating commands: {e}", err=True)
        logger.exception("Command generation failed")
        ctx.exit(1)


@util.command(name='config')
@click.pass_context
def show_config(ctx):
    """Show current configuration."""
    script_dir = ctx.obj['script_dir']
    config = ctx.obj.get('config')

    click.echo("Current Configuration:")
    click.echo(f"  Script directory: {script_dir}")
    click.echo(f"  Config file: {config or '(none specified)'}")

    # Show available config files
    config_files = list(script_dir.parent.glob('*.cfg'))
    if config_files:
        click.echo("\nAvailable config files:")
        for cfg in config_files:
            click.echo(f"  - {cfg.name}")

    # Show benchmark directory
    benchmark_dir = script_dir.parent / 'sv-benchmarks'
    click.echo(f"\nBenchmark directory: {benchmark_dir}")
    click.echo(f"  Exists: {benchmark_dir.exists()}")

    # Show results directory
    results_dir = script_dir.parent / 'results'
    click.echo(f"\nResults directory: {results_dir}")
    click.echo(f"  Exists: {results_dir.exists()}")
    if results_dir.exists():
        result_files = list(results_dir.glob('results_*.json'))
        click.echo(f"  Result files: {len(result_files)}")

    # Show logs directory
    logs_dir = script_dir.parent / 'logs'
    click.echo(f"\nLogs directory: {logs_dir}")
    click.echo(f"  Exists: {logs_dir.exists()}")


@util.command(name='check-deps')
@click.pass_context
def check_deps(ctx):
    """Check if dependencies are installed."""
    script_dir = ctx.obj['script_dir']

    click.echo("Checking dependencies...\n")

    # Check Python version
    import sys
    click.echo(f"Python version: {sys.version}")

    # Check required packages
    required_packages = [
        'click',
        'yaml',
        'pathlib',
    ]

    optional_packages = [
        'matplotlib',
        'numpy',
    ]

    all_ok = True

    click.echo("\nRequired packages:")
    for pkg in required_packages:
        try:
            __import__(pkg)
            click.secho(f"  ✓ {pkg}", fg='green')
        except ImportError:
            click.secho(f"  ✗ {pkg}", fg='red')
            all_ok = False

    click.echo("\nOptional packages:")
    for pkg in optional_packages:
        try:
            __import__(pkg)
            click.secho(f"  ✓ {pkg}", fg='green')
        except ImportError:
            click.secho(f"  - {pkg} (not installed)", fg='yellow')

    # Check venv
    venv_path = script_dir / '.venv'
    click.echo(f"\nVirtual environment ({venv_path}):")
    click.echo(f"  Exists: {venv_path.exists()}")

    if all_ok:
        click.secho("\n✓ All required dependencies are installed", fg='green')
    else:
        click.secho("\n✗ Some dependencies are missing. Run 'svcomp setup install-deps'", fg='red', err=True)
        ctx.exit(1)
