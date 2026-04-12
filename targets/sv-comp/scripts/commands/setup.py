"""Setup commands for SV-COMP."""

import click
import subprocess
import logging
from pathlib import Path

logger = logging.getLogger(__name__)


@click.group(name='setup')
def setup():
    """Setup commands for benchmarks and dependencies."""
    pass


@setup.command(name='checkout-benchmarks')
@click.pass_context
def checkout_benchmarks(ctx):
    """Checkout SV-Benchmarks repository (sparse checkout of java folder)."""
    script_dir = ctx.obj['script_dir']
    checkout_script = script_dir / 'checkout.sh'

    if not checkout_script.exists():
        click.echo(f"Error: checkout.sh not found at {checkout_script}", err=True)
        ctx.exit(1)

    click.echo("Checking out SV-Benchmarks repository...")
    click.echo("This may take some time...")

    try:
        result = subprocess.run(
            ['bash', str(checkout_script)],
            cwd=script_dir,
            check=True,
            capture_output=True,
            text=True
        )
        click.echo(result.stdout)
        click.secho("✓ Successfully checked out SV-Benchmarks", fg='green')
    except subprocess.CalledProcessError as e:
        click.echo(f"Error running checkout script: {e}", err=True)
        click.echo(e.stderr, err=True)
        ctx.exit(1)


@setup.command(name='checkout-validator')
@click.pass_context
def checkout_validator(ctx):
    """Checkout wit4java witness validator."""
    script_dir = ctx.obj['script_dir']
    checkout_script = script_dir / 'checkout_wit4java.sh'

    if not checkout_script.exists():
        click.echo(f"Error: checkout_wit4java.sh not found at {checkout_script}", err=True)
        ctx.exit(1)

    click.echo("Checking out wit4java validator...")

    try:
        result = subprocess.run(
            ['bash', str(checkout_script)],
            cwd=script_dir,
            check=True,
            capture_output=True,
            text=True
        )
        click.echo(result.stdout)
        click.secho("✓ Successfully checked out wit4java validator", fg='green')
    except subprocess.CalledProcessError as e:
        click.echo(f"Error running checkout script: {e}", err=True)
        click.echo(e.stderr, err=True)
        ctx.exit(1)


@setup.command(name='install-deps')
@click.option('--venv-dir', default='.venv', help='Virtual environment directory')
@click.pass_context
def install_deps(ctx, venv_dir):
    """Install Python dependencies in a virtual environment."""
    script_dir = ctx.obj['script_dir']
    venv_path = script_dir / venv_dir
    requirements_file = script_dir / 'requirements.txt'

    if not requirements_file.exists():
        click.echo(f"Error: requirements.txt not found at {requirements_file}", err=True)
        ctx.exit(1)

    # Create venv if it doesn't exist
    if not venv_path.exists():
        click.echo(f"Creating virtual environment at {venv_path}...")
        try:
            subprocess.run(['python3', '-m', 'venv', str(venv_path)], check=True)
            click.echo("✓ Virtual environment created")
        except subprocess.CalledProcessError as e:
            click.echo(f"Error creating virtual environment: {e}", err=True)
            ctx.exit(1)

    # Install dependencies
    pip_path = venv_path / 'bin' / 'pip3'
    click.echo("Installing dependencies...")

    try:
        result = subprocess.run(
            [str(pip_path), 'install', '-r', str(requirements_file)],
            check=True,
            capture_output=True,
            text=True
        )
        click.echo(result.stdout)
        click.secho("✓ Dependencies installed successfully", fg='green')
    except subprocess.CalledProcessError as e:
        click.echo(f"Error installing dependencies: {e}", err=True)
        click.echo(e.stderr, err=True)
        ctx.exit(1)


@setup.command(name='all')
@click.pass_context
def setup_all(ctx):
    """Run all setup steps (checkout benchmarks, validator, and install deps)."""
    click.echo("Running complete setup...\n")

    click.echo("Step 1/3: Checking out benchmarks...")
    ctx.invoke(checkout_benchmarks)

    click.echo("\nStep 2/3: Checking out validator...")
    ctx.invoke(checkout_validator)

    click.echo("\nStep 3/3: Installing dependencies...")
    ctx.invoke(install_deps)

    click.secho("\n✓ Setup complete!", fg='green')
