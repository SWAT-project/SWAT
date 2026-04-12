import os
from typing import Any


IS_RUNNING_IN_CI: bool = os.getenv("CI", "false") == "true"

def ci_print(message: Any) -> None:
    """Print a message formatted for CI systems."""
    if IS_RUNNING_IN_CI:
        print(f"{message}")