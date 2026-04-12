from pathlib import Path
from typing import Optional, TypedDict
import enum

class ExpectedVerdict(enum.Enum):
    VIOLATION = False
    SAFE = True

class Verdict(enum.Enum):
    VIOLATION = "== ERROR"
    SAFE = "== OK"
    UNKNOWN = "== DONT-KNOW"
    NO_SYMBOLIC_VARS = "== NON-SYMBOLIC"
    
class VerificationCategory(enum.Enum):
    VALID_ASSERT_PRP = "valid-assert.prp"
    NO_RUNTIME_EXCEPTION_PRP = "no-runtime-exception.prp"
    NO_DEADLOCK_PRP = "no-deadlock.prp"

class Command(TypedDict):
    target_dir: Path
    target: Path
    log_dir: Path
    command: list[str]
    
class VerificationResult(TypedDict):
    points: int
    case: str
    
class VerificationTask(TypedDict):
    category: VerificationCategory
    verdict: ExpectedVerdict
    file_path: Path
    input_files: list[Path]
    command: Optional[Command]
    result: Optional[VerificationResult]
    
    