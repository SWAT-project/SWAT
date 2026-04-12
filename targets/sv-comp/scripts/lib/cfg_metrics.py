"""
Module for extracting CFG metrics from SV-COMP benchmarks using cfg-extractor.
"""

import json
import logging
import subprocess
import tempfile
from pathlib import Path
from typing import Optional, TypedDict, Any

from .dtypes import VerificationTask
from .selection import extract_testcases

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

SCRIPT_DIR = Path(__file__).resolve().parent.parent
BASE_PATH = SCRIPT_DIR.parents[2]
CFG_EXTRACTOR_JAR = BASE_PATH / 'libs' / 'cfg-extractor' / 'cfg-extractor-1.0-SNAPSHOT.jar'


class CFGExtractionCommand(TypedDict):
    """Command to extract CFG metrics for a verification task."""
    target: Path
    test_case_dir: Path
    java_files: list[Path]
    output_dir: Path
    command: list[str]


def check_cfg_extractor_available() -> bool:
    """
    Check if cfg-extractor JAR is available.

    Returns:
        True if cfg-extractor.jar exists, False otherwise
    """
    if not CFG_EXTRACTOR_JAR.exists():
        logger.error(f"cfg-extractor JAR not found at {CFG_EXTRACTOR_JAR}")
        logger.error("Run './gradlew downloadCfgExtractor' to download it")
        return False
    return True


def find_main_class(compile_dir: Path) -> Optional[str]:
    """
    Find the fully qualified name of the class containing the main method.

    Args:
        compile_dir: Directory containing compiled .class files

    Returns:
        Fully qualified class name if found, None otherwise
    """
    for class_file in compile_dir.rglob('*.class'):
        # Get the relative path and convert to fully qualified name
        rel_path = class_file.relative_to(compile_dir)
        class_name = str(rel_path.with_suffix('')).replace('/', '.')

        # Check if this class has a main method using javap
        try:
            result = subprocess.run(
                ['javap', '-p', class_name],
                cwd=compile_dir,
                capture_output=True,
                text=True,
                timeout=5
            )
            if 'public static void main(java.lang.String[])' in result.stdout:
                return class_name
        except Exception:
            continue

    return None


def generate_cfg_command(
    ver_task: VerificationTask,
    analysis_dir: Path,
    analysis_type: str = 'inter'
) -> CFGExtractionCommand:
    """
    Generate a CFG extraction command for a verification task.

    Args:
        ver_task: Verification task containing input files
        analysis_dir: Base directory for analysis outputs
        analysis_type: 'intra' for per-method CFGs, 'inter' for inter-procedural (default: 'inter')

    Returns:
        CFGExtractionCommand with command details
    """
    test_case_dir = ver_task['file_path'].parent

    # Expand input_files to find actual Java files (input_files can contain directories)
    java_files = []
    for input_item in ver_task['input_files']:
        input_path = test_case_dir / input_item
        if input_path.is_file() and str(input_path).endswith('.java'):
            java_files.append(input_path)
        elif input_path.is_dir():
            # Recursively find all Java files in the directory
            java_files.extend(input_path.rglob('*.java'))
        elif str(input_item).endswith('.java'):
            # Handle case where the file might not exist yet
            potential_file = test_case_dir / input_item
            if potential_file.exists():
                java_files.append(potential_file)

    # Create output directory based on task structure (similar to logs)
    # Get relative path from sv-benchmarks/java/
    try:
        sv_benchmarks_java = Path('sv-benchmarks') / 'java'
        # Find the sv-benchmarks parent directory
        parts = list(test_case_dir.parts)
        if 'sv-benchmarks' in parts:
            sb_index = parts.index('sv-benchmarks')
            rel_target_path = Path(*parts[sb_index+2:])  # Skip sv-benchmarks/java
        else:
            rel_target_path = Path(ver_task['file_path'].stem)
    except (ValueError, IndexError):
        rel_target_path = Path(ver_task['file_path'].stem)

    target_name = ver_task['file_path'].stem
    category_suffix = ver_task['category'].value.replace('.prp', '')
    output_dir = analysis_dir / rel_target_path / f"{target_name}_{category_suffix}"

    # Build command - will be executed with compilation step
    # The actual command will be built during execution
    command: CFGExtractionCommand = {
        'target': ver_task['file_path'],
        'test_case_dir': test_case_dir,
        'java_files': java_files,
        'output_dir': output_dir,
        'command': []  # Will be populated during execution
    }

    return command


def execute_cfg_extraction(
    cfg_command: CFGExtractionCommand,
    analysis_type: str = 'inter',
    timeout: int = 60,
    skip_existing: bool = True
) -> dict[str, Any]:
    """
    Execute CFG extraction for a single command.

    This compiles the Java files and runs cfg-extractor.

    Args:
        cfg_command: CFG extraction command details
        analysis_type: 'intra' or 'inter' (default: 'inter')
        timeout: Timeout in seconds for extraction
        skip_existing: Skip if CFG files already exist (default: True)

    Returns:
        Dictionary with execution results
    """
    if not cfg_command['java_files']:
        return {
            'success': False,
            'error': 'No Java files found',
            'cfgs_extracted': 0,
            'output_dir': None
        }

    # Create output directory
    cfg_command['output_dir'].mkdir(parents=True, exist_ok=True)

    # Check if CFG already exists
    if skip_existing:
        existing_cfgs = list(cfg_command['output_dir'].glob('*.json'))
        if existing_cfgs:
            logger.debug(f"Skipping {cfg_command['target']} - CFGs already exist")
            return {
                'success': True,
                'error': None,
                'cfgs_extracted': len(existing_cfgs),
                'skipped': True,
                'output_dir': str(cfg_command['output_dir']),
                'cfg_files': [f.name for f in existing_cfgs]
            }

    # Create a temp directory to compile Java files
    with tempfile.TemporaryDirectory(prefix='svcomp-cfg-compile-') as temp_compile_dir:
        compile_dir = Path(temp_compile_dir)

        # Compile Java files to .class files
        javac_cmd = ['javac', '-d', str(compile_dir)] + [str(f) for f in cfg_command['java_files']]

        try:
            logger.debug(f"Compiling: {' '.join(str(f.name) for f in cfg_command['java_files'])}")
            compile_result = subprocess.run(
                javac_cmd,
                capture_output=True,
                text=True,
                timeout=30
            )

            if compile_result.returncode != 0:
                return {
                    'success': False,
                    'error': f'Compilation failed: {compile_result.stderr[:200]}',
                    'cfgs_extracted': 0,
                    'output_dir': str(cfg_command['output_dir'])
                }

            # Count class files
            class_files = list(compile_dir.rglob('*.class'))
            if not class_files:
                return {
                    'success': False,
                    'error': 'No class files generated',
                    'cfgs_extracted': 0,
                    'output_dir': str(cfg_command['output_dir'])
                }

            # Find the main class
            main_class = find_main_class(compile_dir)
            if not main_class:
                logger.warning(f"No main class found for {cfg_command['target']}, extracting all classes")

            # Build cfg-extractor command
            cfg_cmd = [
                'java',
                '-jar',
                str(CFG_EXTRACTOR_JAR),
                str(compile_dir),
                str(cfg_command['output_dir']),
            ]

            if main_class:
                cfg_cmd.extend([main_class, 'main', analysis_type])
            else:
                # If no main class, just extract all methods with intra analysis
                cfg_cmd.append('intra')

            logger.debug(f"Running cfg-extractor: {' '.join(cfg_cmd)}")

            # Run cfg-extractor
            result = subprocess.run(
                cfg_cmd,
                capture_output=True,
                text=True,
                timeout=timeout
            )

            # Save cfg-extractor output to log file
            log_file = cfg_command['output_dir'] / 'cfg-extractor.log'
            try:
                with open(log_file, 'w') as f:
                    f.write("=== CFG Extractor Command ===\n")
                    f.write(' '.join(cfg_cmd) + '\n\n')
                    f.write("=== Return Code ===\n")
                    f.write(f"{result.returncode}\n\n")
                    f.write("=== Standard Output ===\n")
                    f.write(result.stdout)
                    f.write("\n\n=== Standard Error ===\n")
                    f.write(result.stderr)
                logger.debug(f"Saved cfg-extractor output to {log_file}")
            except Exception as e:
                logger.warning(f"Failed to save log file: {e}")

            if result.returncode != 0:
                return {
                    'success': False,
                    'error': f'cfg-extractor failed: {result.stderr[:200]}',
                    'cfgs_extracted': 0,
                    'output_dir': str(cfg_command['output_dir'])
                }

            # Count extracted CFG files
            cfg_files = list(cfg_command['output_dir'].glob('*.json'))

            return {
                'success': True,
                'error': None,
                'cfgs_extracted': len(cfg_files),
                'class_files_compiled': len(class_files),
                'main_class': main_class,
                'output_dir': str(cfg_command['output_dir']),
                'cfg_files': [f.name for f in cfg_files]
            }

        except subprocess.TimeoutExpired:
            return {
                'success': False,
                'error': 'Extraction timed out',
                'cfgs_extracted': 0,
                'output_dir': str(cfg_command['output_dir'])
            }
        except Exception as e:
            return {
                'success': False,
                'error': f'Unexpected error: {str(e)}',
                'cfgs_extracted': 0,
                'output_dir': str(cfg_command['output_dir'])
            }


def generate_cfg_commands(
    ver_tasks: list[VerificationTask],
    analysis_dir: Optional[Path] = None,
    analysis_type: str = 'inter'
) -> list[CFGExtractionCommand]:
    """
    Generate CFG extraction commands for multiple verification tasks.

    Args:
        ver_tasks: List of verification tasks
        analysis_dir: Base directory for analysis outputs (default: scripts/../analysis)
        analysis_type: 'intra' or 'inter' (default: 'inter')

    Returns:
        List of CFGExtractionCommand
    """
    if analysis_dir is None:
        analysis_dir = SCRIPT_DIR.parent / 'analysis'

    commands = []
    for ver_task in ver_tasks:
        command = generate_cfg_command(ver_task, analysis_dir, analysis_type)
        commands.append(command)

    return commands


def extract_cfg_metrics_batch(
    ver_tasks: list[VerificationTask],
    analysis_dir: Optional[Path] = None,
    analysis_type: str = 'inter',
    skip_existing: bool = True
) -> list[dict[str, Any]]:
    """
    Extract CFG metrics for multiple verification tasks.

    Args:
        ver_tasks: List of verification tasks
        analysis_dir: Base directory for analysis outputs
        analysis_type: 'intra' or 'inter' (default: 'inter')
        skip_existing: Skip extraction if CFG files already exist (default: True)

    Returns:
        List of extraction results
    """
    if not check_cfg_extractor_available():
        logger.error("cfg-extractor not available")
        return []

    if analysis_dir is None:
        analysis_dir = SCRIPT_DIR.parent / 'analysis'

    commands = generate_cfg_commands(ver_tasks, analysis_dir, analysis_type)
    results = []

    for i, command in enumerate(commands, 1):
        logger.info(f"Extracting CFG metrics [{i}/{len(commands)}]: {command['target'].parent.name}/{command['target'].name}")

        result = execute_cfg_extraction(command, analysis_type, skip_existing=skip_existing)
        result['target'] = str(command['target'])

        results.append(result)

        if result['success']:
            if result.get('skipped'):
                logger.info(f"  ⊘ Skipped (already exists) - {result['cfgs_extracted']} CFG(s)")
            else:
                logger.info(f"  ✓ Extracted {result['cfgs_extracted']} CFG(s)")
        else:
            logger.warning(f"  ✗ Failed: {result['error']}")

    # Print summary
    successful = sum(1 for r in results if r['success'])
    skipped = sum(1 for r in results if r.get('skipped', False))
    extracted = successful - skipped
    failed = len(results) - successful
    total_cfgs = sum(r['cfgs_extracted'] for r in results)
    logger.info(f"\n{'='*60}")
    logger.info(f"CFG extraction completed:")
    logger.info(f"  {extracted} extracted, {skipped} skipped, {failed} failed (of {len(results)} total)")
    logger.info(f"  {total_cfgs} total CFG files")
    logger.info(f"  Output directory: {analysis_dir}")
    logger.info(f"{'='*60}")

    return results


def extract_single_benchmark(
    benchmark_path: Path,
    analysis_dir: Optional[Path] = None,
    analysis_type: str = 'inter',
    skip_existing: bool = True
) -> Optional[dict[str, Any]]:
    """
    Extract CFG metrics for a single benchmark task file.

    Args:
        benchmark_path: Path to the benchmark YAML file
        analysis_dir: Base directory for analysis outputs
        analysis_type: 'intra' or 'inter'
        skip_existing: Skip if CFG files already exist

    Returns:
        Extraction result dictionary or None if benchmark not found
    """
    if not benchmark_path.exists():
        logger.error(f"Benchmark file not found: {benchmark_path}")
        return None

    # Extract test cases from the directory containing this benchmark
    task_dir = benchmark_path.parent
    ver_tasks = extract_testcases(task_dir)

    # Find the matching task
    matching_task = None
    for task in ver_tasks:
        if task['file_path'] == benchmark_path:
            matching_task = task
            break

    if not matching_task:
        logger.error(f"No verification task found for {benchmark_path}")
        return None

    # Extract CFG metrics for this single task
    results = extract_cfg_metrics_batch([matching_task], analysis_dir, analysis_type, skip_existing)

    return results[0] if results else None


if __name__ == "__main__":
    # Test the cfg-extractor availability
    logger.info("Checking cfg-extractor availability...")
    if check_cfg_extractor_available():
        logger.info("✓ cfg-extractor is available!")

        # Test with a small number of benchmarks
        task_dir = SCRIPT_DIR.parent / 'sv-benchmarks'
        if task_dir.exists():
            logger.info(f"Extracting test cases from {task_dir}...")
            ver_tasks = extract_testcases(task_dir)

            if ver_tasks:
                logger.info(f"Found {len(ver_tasks)} test cases")
                logger.info("Extracting metrics for first 2 tasks...")

                results = extract_cfg_metrics_batch(ver_tasks[:2], analysis_type='inter')
            else:
                logger.warning("No test cases found")
        else:
            logger.warning(f"Benchmark directory not found: {task_dir}")
    else:
        logger.error("✗ cfg-extractor is not available")
        logger.error("Run './gradlew downloadCfgExtractor' from the project root")
