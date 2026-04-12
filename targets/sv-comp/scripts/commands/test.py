"""Test execution commands for SV-COMP."""

import click
import logging
from pathlib import Path
import sys
from lib.execution import target_execution

logger = logging.getLogger(__name__)


@click.group(name='test')
def test():
    """Test execution commands."""
    pass


@test.command(name='list')
@click.option('--benchmark-dir', type=click.Path(exists=True), help='Path to sv-benchmarks directory')
@click.option('--stats', is_flag=True, help='Show detailed statistics')
@click.pass_context
def list_tests(ctx, benchmark_dir, stats):
    """List available test cases."""
    from lib import extract_testcases, print_testcase_statistics

    script_dir = ctx.obj['script_dir']

    # Determine benchmark directory
    if benchmark_dir:
        test_dir = Path(benchmark_dir)
    else:
        test_dir = script_dir.parent / 'sv-benchmarks'

    if not test_dir.exists():
        click.echo(f"Error: Benchmark directory not found at {test_dir}", err=True)
        click.echo("Run 'svcomp setup checkout-benchmarks' first", err=True)
        ctx.exit(1)

    click.echo(f"Scanning benchmark directory: {test_dir}")

    try:
        ver_tasks = extract_testcases(test_dir)
        click.echo(f"\nFound {len(ver_tasks)} test cases")

        if stats:
            print_testcase_statistics(ver_tasks)
    except click.exceptions.Exit:
        # Re-raise Exit exceptions (from ctx.exit calls) without logging
        raise
    except Exception as e:
        click.echo(f"Error extracting test cases: {e}", err=True)
        logger.exception("Failed to extract test cases")
        ctx.exit(1)


@test.command(name='run')
@click.option('--mode', type=click.Choice(['single', 'parallel']), default='parallel', help='Execution mode')
@click.option('--workers', type=int, default=50, help='Number of parallel workers')
@click.option('--benchmark-dir', type=click.Path(exists=True), help='Path to sv-benchmarks directory')
@click.option('--config-file', default='sv-comp.cfg', help='Configuration file name')
@click.option('--categories', multiple=True, help='Verification categories to run (e.g., valid-assert.prp)')
@click.option('--target', help='Single target to run (only for single mode)')
@click.pass_context
def run_tests(ctx, mode, workers, benchmark_dir, config_file, categories, target):
    """Run verification tests."""
    from lib import (
        extract_testcases,
        generate_commands,
        run_parallel,
        run_single_target,
        check_port_availability,
        VerificationCategory,
    )
    script_dir = ctx.obj['script_dir']

    # Determine benchmark directory
    if benchmark_dir:
        test_dir = Path(benchmark_dir)
    else:
        test_dir = script_dir.parent / 'sv-benchmarks'

    if not test_dir.exists():
        click.echo(f"Error: Benchmark directory not found at {test_dir}", err=True)
        click.echo("Run 'svcomp setup checkout-benchmarks' first", err=True)
        ctx.exit(1)

    click.echo(f"Running tests in {mode} mode...")
    click.echo(f"Benchmark directory: {test_dir}")

    try:
        # Extract test cases
        ver_tasks = extract_testcases(test_dir)
        click.echo(f"Extracted {len(ver_tasks)} test cases")

        # Filter by categories if specified
        if categories:
            selected_categories = []
            for cat in categories:
                try:
                    selected_categories.append(VerificationCategory(cat))
                except ValueError:
                    click.echo(f"Warning: Unknown category '{cat}', skipping", err=True)

            if selected_categories:
                ver_tasks = [task for task in ver_tasks if task['category'] in selected_categories]
                click.echo(f"Filtered to {len(ver_tasks)} test cases for categories: {[c.value for c in selected_categories]}")

        # Generate commands
        if mode == 'single' and target is None:
            # Use default target from original script
            config = 'swat-debug.cfg'
        else:
            config = config_file

        ver_tasks_with_commands = generate_commands(ver_tasks, config)
        click.echo(f"Generated {len(ver_tasks_with_commands)} commands")

        # Check port availability
        ports_available, occupied_ports = check_port_availability(ver_tasks_with_commands)
        if not ports_available:
            click.echo(f"Warning: {len(occupied_ports)} ports are occupied: {occupied_ports[:10]}", err=True)
            if not click.confirm("Continue anyway?"):
                ctx.exit(1)

        # Run tests
        if mode == 'single':
            if target:
                # Find and run specific target
                ver_task = None
                for task in ver_tasks_with_commands:
                    if str(task['command']['target']) == target:
                        ver_task = task
                        break

                if ver_task is None:
                    click.echo(f"Error: Target '{target}' not found", err=True)
                    ctx.exit(1)

                click.echo(f"Running single target: {target}")
                target_execution(ver_task)
            else:
                # Run default single target
                run_single_target(ver_tasks_with_commands)
        else:
            run_parallel(ver_tasks_with_commands, max_workers=workers)

        click.secho("✓ Test execution complete", fg='green')

    except click.exceptions.Exit:
        # Re-raise Exit exceptions (from ctx.exit calls) without logging
        raise
    except Exception as e:
        click.echo(f"Error running tests: {e}", err=True)
        logger.exception("Test execution failed")
        ctx.exit(1)


@test.command(name='validate-ports')
@click.option('--benchmark-dir', type=click.Path(exists=True), help='Path to sv-benchmarks directory')
@click.option('--config-file', default='sv-comp.cfg', help='Configuration file name')
@click.pass_context
def validate_ports(ctx, benchmark_dir, config_file):
    """Check if required ports are available."""
    from lib import extract_testcases, generate_commands, check_port_availability

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
        ver_tasks_with_commands = generate_commands(ver_tasks, config_file)

        ports_available, occupied_ports = check_port_availability(ver_tasks_with_commands)

        if ports_available:
            click.secho("✓ All required ports are available", fg='green')
        else:
            click.secho(f"✗ {len(occupied_ports)} ports are occupied:", fg='red', err=True)
            for port in occupied_ports[:20]:  # Show first 20
                click.echo(f"  - {port}")
            ctx.exit(1)

    except click.exceptions.Exit:
        # Re-raise Exit exceptions (from ctx.exit calls) without logging
        raise
    except Exception as e:
        click.echo(f"Error checking ports: {e}", err=True)
        logger.exception("Port validation failed")
        ctx.exit(1)
