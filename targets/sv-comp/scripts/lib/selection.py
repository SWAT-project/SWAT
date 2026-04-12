from typing import Any
from collections import defaultdict
import yaml
import logging
from pathlib import Path
from pprint import pformat
from .dtypes import ExpectedVerdict, VerificationCategory, VerificationTask

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

SCRIPT_DIR = Path(__file__).resolve().parent

def validate_property_file_format(data: dict[str, Any]) -> bool:
    # Validate the structure
    return (
        "format_version" in data and data["format_version"] == "2.0" and
        "input_files" in data and isinstance(data["input_files"], list) and
        "properties" in data and isinstance(data["properties"], list) and
        len(data["properties"]) >= 1 and # type: ignore
        "property_file" in data["properties"][0] and "expected_verdict" in data["properties"][0] and
        "options" in data and "language" in data["options"] and data["options"]["language"] == "Java"
    )

def extract_testcases(path: Path) -> list[VerificationTask]:
    ver_tasks: list[VerificationTask] = []

    # Use Path.rglob to recursively find all YAML files
    yaml_files = [*path.rglob("*.yml"), *path.rglob("*.yaml")]
    for file_path in yaml_files:
        logger.debug(f"Loading YAML file: {file_path}")
        with open(file_path, 'r') as f:
            try:
                data: dict[str, Any] = yaml.safe_load(f)
            except yaml.YAMLError as e:
                raise ValueError(f"Error parsing YAML file {file_path}: {e}")

            if not validate_property_file_format(data):
                raise ValueError(f"Invalid format in YAML file: {file_path}")

            for property in data["properties"]:
                ver_task: VerificationTask = {
                    'category': VerificationCategory(property['property_file'].split('/')[-1]),
                    'verdict': ExpectedVerdict(property['expected_verdict']),
                    'file_path': file_path,
                    'input_files': data["input_files"],
                    'command': None,
                    'result': None
                }
                ver_tasks.append(ver_task)

            logger.debug(f"Loaded test case: {file_path}")

    return ver_tasks


def print_testcase_statistics(ver_tasks: list[VerificationTask]) -> None:
    """Print concise statistics about test cases by category."""
    # Count verdicts for each category using nested defaultdict
    stats: defaultdict[VerificationCategory, defaultdict[ExpectedVerdict, int]] = defaultdict(lambda: defaultdict(int))

    for task in ver_tasks:
        stats[task['category']][task['verdict']] += 1

    logger.info(f"{'='*60}")
    logger.info(f"Test Case Statistics (Total: {len(ver_tasks)})")
    logger.info(f"{'='*60}")
    logger.info(f"{'Property':<30} {'Safe':<10} {'Violation':<10}")
    logger.info(f"{'-'*60}")

    for category in VerificationCategory:
        safe_count = stats[category][ExpectedVerdict.SAFE]
        violation_count = stats[category][ExpectedVerdict.VIOLATION]
        logger.info(f"{category.value:<30} {safe_count:<10} {violation_count:<10}")

    logger.info(f"{'='*60}")


if __name__ == "__main__":
    logger.info("Testing testcase extraction script...")
    test_dir = (SCRIPT_DIR / ".." / "sv-benchmarks").resolve()
    logger.info(f"Base directory: {test_dir}")
    test_cases = extract_testcases(test_dir)
    logger.info(f"Loaded {len(test_cases)} test cases from directory: {test_dir}")

    print_testcase_statistics(test_cases)

    logger.info("\nFirst 3 test cases:")
    for i, case in enumerate(test_cases[:3], 1):
        logger.info(f"\nTest Case {i}:\n{pformat(dict(case), width=100)}")