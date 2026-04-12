"""Analysis commands for SV-COMP results."""

import click
import logging
from pathlib import Path
import sys

logger = logging.getLogger(__name__)


@click.group(name='analyze')
def analyze():
    """Analysis commands for test results."""
    pass


@analyze.command(name='results')
@click.argument('results_file', type=click.Path(exists=True), required=False)
@click.option('--logs-dir', type=click.Path(exists=True), help='Path to logs directory')
@click.pass_context
def analyze_results(ctx, results_file, logs_dir):
    """Analyze results to identify context losses and failures.

    If RESULTS_FILE is not provided, will use the most recent results file.
    """
    from lib.analysis import main as run_analysis

    script_dir = ctx.obj['script_dir']

    # Determine results file
    if results_file:
        results_path = Path(results_file)
    else:
        # Look for latest results file
        results_dir = script_dir.parent / 'results'
        if results_dir.exists():
            json_files = list(results_dir.glob('results_*.json'))
            if json_files:
                results_path = max(json_files, key=lambda p: p.stat().st_mtime)
                click.echo(f"Using latest results file: {results_path}")
            else:
                click.echo("Error: No results files found in results/", err=True)
                ctx.exit(1)
        else:
            click.echo("Error: results/ directory not found and no file specified", err=True)
            ctx.exit(1)

    # Determine logs directory
    if logs_dir:
        logs_path = Path(logs_dir)
    else:
        logs_path = script_dir.parent / 'logs'

    if not logs_path.exists():
        click.secho(f"Warning: Logs directory not found at {logs_path}", fg='yellow')

    click.echo(f"Analyzing results from: {results_path}")
    click.echo(f"Looking for logs in: {logs_path}\n")

    # Run the analysis by temporarily modifying sys.argv
    old_argv = sys.argv
    try:
        sys.argv = ['analyse_ctx_loss.py', str(results_path)]
        run_analysis()
    except SystemExit:
        pass  # analysis script calls sys.exit
    finally:
        sys.argv = old_argv


@analyze.command(name='compare')
@click.argument('current_file', type=click.Path(exists=True))
@click.argument('reference_file', type=click.Path(exists=True))
@click.pass_context
def compare(ctx, current_file, reference_file):
    """Compare two result files.

    Shows what changed between REFERENCE_FILE and CURRENT_FILE.
    """
    from lib import compare_results

    click.echo(f"Comparing results:")
    click.echo(f"  Reference: {reference_file}")
    click.echo(f"  Current:   {current_file}\n")

    try:
        compare_results(current_file, reference_file)
    except click.exceptions.Exit:
        # Re-raise Exit exceptions (from ctx.exit calls) without logging
        raise
    except Exception as e:
        click.echo(f"Error comparing results: {e}", err=True)
        logger.exception("Comparison failed")
        ctx.exit(1)


@analyze.command(name='stats')
@click.option('--benchmark-dir', type=click.Path(exists=True), help='Path to sv-benchmarks directory')
@click.pass_context
def stats(ctx, benchmark_dir):
    """Print statistics about test cases."""
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

    click.echo(f"Analyzing test cases in: {test_dir}\n")

    try:
        ver_tasks = extract_testcases(test_dir)
        print_testcase_statistics(ver_tasks)
    except click.exceptions.Exit:
        # Re-raise Exit exceptions (from ctx.exit calls) without logging
        raise
    except Exception as e:
        click.echo(f"Error analyzing test cases: {e}", err=True)
        logger.exception("Stats analysis failed")
        ctx.exit(1)


@analyze.command(name='cfg-metrics')
@click.option('--benchmark-dir', type=click.Path(exists=True), help='Path to sv-benchmarks directory')
@click.option('--analysis-dir', type=click.Path(), help='Output directory for CFG analysis (default: analysis/)')
@click.option('--analysis-type', type=click.Choice(['intra', 'inter']), default='inter', help='Analysis type: intra-procedural or inter-procedural')
@click.option('--categories', multiple=True, help='Filter by category (e.g., valid-assert.prp)')
@click.option('--target', type=str, help='Filter by target path substring')
@click.option('--limit', type=int, help='Limit number of benchmarks to process')
@click.option('--force', is_flag=True, help='Force re-extraction even if CFGs already exist')
@click.pass_context
def cfg_metrics(ctx, benchmark_dir, analysis_dir, analysis_type, categories, target, limit, force):
    """Extract CFG metrics and metadata for all benchmarks.

    Compiles Java benchmarks and runs cfg-extractor to generate
    control flow graphs and complexity metrics.
    """
    from lib import (
        extract_testcases,
        extract_cfg_metrics_batch,
        check_cfg_extractor_available,
    )

    script_dir = ctx.obj['script_dir']

    # Check if cfg-extractor is available
    if not check_cfg_extractor_available():
        click.echo("Error: cfg-extractor not available", err=True)
        click.echo("Run './gradlew downloadCfgExtractor' from project root", err=True)
        ctx.exit(1)

    # Determine benchmark directory
    if benchmark_dir:
        test_dir = Path(benchmark_dir)
    else:
        test_dir = script_dir.parent / 'sv-benchmarks'

    if not test_dir.exists():
        click.echo(f"Error: Benchmark directory not found at {test_dir}", err=True)
        click.echo("Run 'svcomp setup checkout-benchmarks' first", err=True)
        ctx.exit(1)

    # Determine analysis directory
    if analysis_dir:
        output_dir = Path(analysis_dir)
    else:
        output_dir = script_dir.parent / 'analysis'

    click.echo(f"Extracting CFG metrics:")
    click.echo(f"  Benchmarks: {test_dir}")
    click.echo(f"  Output: {output_dir}")
    click.echo(f"  Analysis type: {analysis_type}\n")

    try:
        # Extract test cases
        ver_tasks = extract_testcases(test_dir)

        if not ver_tasks:
            click.echo("No test cases found", err=True)
            ctx.exit(1)

        # Apply filters
        if categories:
            from lib.dtypes import VerificationCategory
            category_enums = [VerificationCategory(cat) for cat in categories]
            ver_tasks = [t for t in ver_tasks if t['category'] in category_enums]

        if target:
            ver_tasks = [t for t in ver_tasks if target in str(t['file_path'])]

        if limit:
            ver_tasks = ver_tasks[:limit]

        click.echo(f"Processing {len(ver_tasks)} benchmark(s)...\n")

        # Extract CFG metrics
        skip_existing = not force
        results = extract_cfg_metrics_batch(ver_tasks, output_dir, analysis_type, skip_existing)

        # Print summary
        click.echo("\n" + "="*60)
        successful = sum(1 for r in results if r['success'])
        skipped = sum(1 for r in results if r.get('skipped', False))
        extracted = successful - skipped
        failed = len(results) - successful
        total_cfgs = sum(r.get('cfgs_extracted', 0) for r in results)

        if failed == 0:
            click.secho(f"✓ All {len(results)} benchmarks processed ({extracted} extracted, {skipped} skipped)", fg='green')
        else:
            click.secho(f"⚠ {extracted} extracted, {skipped} skipped, {failed} failed (of {len(results)} total)", fg='yellow')

        click.echo(f"Total CFG files: {total_cfgs}")
        click.echo(f"Output directory: {output_dir}")
        click.echo("="*60)

    except click.exceptions.Exit:
        raise
    except Exception as e:
        click.echo(f"Error extracting CFG metrics: {e}", err=True)
        logger.exception("CFG metrics extraction failed")
        ctx.exit(1)


@analyze.command(name='cfg-single')
@click.argument('benchmark_file', type=click.Path(exists=True))
@click.option('--analysis-dir', type=click.Path(), help='Output directory for CFG analysis (default: analysis/)')
@click.option('--analysis-type', type=click.Choice(['intra', 'inter']), default='inter', help='Analysis type: intra-procedural or inter-procedural')
@click.option('--force', is_flag=True, help='Force re-extraction even if CFGs already exist')
@click.pass_context
def cfg_single(ctx, benchmark_file, analysis_dir, analysis_type, force):
    """Extract CFG metrics for a single benchmark.

    BENCHMARK_FILE should be a path to a task.yml file.
    """
    from lib import extract_single_benchmark, check_cfg_extractor_available

    script_dir = ctx.obj['script_dir']

    # Check if cfg-extractor is available
    if not check_cfg_extractor_available():
        click.echo("Error: cfg-extractor not available", err=True)
        click.echo("Run './gradlew downloadCfgExtractor' from project root", err=True)
        ctx.exit(1)

    benchmark_path = Path(benchmark_file)

    # Determine analysis directory
    if analysis_dir:
        output_dir = Path(analysis_dir)
    else:
        output_dir = script_dir.parent / 'analysis'

    click.echo(f"Extracting CFG metrics for: {benchmark_path}")
    click.echo(f"Output directory: {output_dir}")
    click.echo(f"Analysis type: {analysis_type}\n")

    try:
        skip_existing = not force
        result = extract_single_benchmark(benchmark_path, output_dir, analysis_type, skip_existing)

        if result is None:
            click.echo("Error: Could not process benchmark", err=True)
            ctx.exit(1)

        if result['success']:
            if result.get('skipped'):
                click.secho(f"⊘ Skipped (already exists) - {result.get('cfgs_extracted', 0)} CFG(s)", fg='yellow')
            else:
                click.secho(f"✓ Successfully extracted {result.get('cfgs_extracted', 0)} CFG(s)", fg='green')

            click.echo(f"Output: {result.get('output_dir')}")

            if 'cfg_files' in result:
                click.echo("\nCFG files:")
                for cfg_file in result['cfg_files']:
                    click.echo(f"  - {cfg_file}")
        else:
            click.secho(f"✗ Failed: {result.get('error')}", fg='red')
            ctx.exit(1)

    except click.exceptions.Exit:
        raise
    except Exception as e:
        click.echo(f"Error extracting CFG metrics: {e}", err=True)
        logger.exception("CFG metrics extraction failed")
        ctx.exit(1)
