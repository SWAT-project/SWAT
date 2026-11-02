"""
Static property pre-check for the SV-COMP driver.

A conservative bytecode analysis of the whole program on the classpath, part of the static
pre-analysis in cfg-extraction (de.uzl.its.cfg.staticcheck.ExceptionFreedomCheck, built as
cfg-extractor-precheck.jar). It proves, or fails to prove:

- for no-runtime-exception: no RuntimeException can escape Main.main or a static initializer;
- for valid-assert: no AssertionError is ever created, so no assert can fail.

A proven property holds on every execution, independently of what the symbolic exploration can
model, so the driver may answer SAFE even where context loss, precision loss, uncaught exceptions
or an unexhausted path space would otherwise prevent it. A failed, crashed or timed-out check
proves nothing, and the driver proceeds as without it.

The check prints one JSON line: tool and version (cfg-extraction's git describe), and per property
whether it is proven and why (not).
"""

import json
import os
import subprocess
import time

import log
logger = log.get_logger()

CHECK_CLASS = "de.uzl.its.cfg.staticcheck.ExceptionFreedomCheck"
CHECK_TIMEOUT_S = 60
JAR_NAME = "cfg-extractor-precheck.jar"
_EXPLORER_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
# Where the jar is found without --precheck-jar: next to symbolic-explorer in the SV-COMP archive,
# and in the build output of the cfg-extraction checkout next to SWAT's in the workspace.
DEFAULT_JARS = (
    os.path.join(_EXPLORER_DIR, "..", "cfg-extraction", JAR_NAME),
    os.path.join(_EXPLORER_DIR, "..", "..", "cfg-extraction", "build", "libs", JAR_NAME),
)

# The property file name -> the field of the check's result that proves it.
PROVES = {
    "no-runtime-exception.prp": ("exception_free", "reason"),
    "valid-assert.prp": ("assertion_free", "assertion_reason"),
}


class PropertyPreCheck:
    def __init__(self, args, prop: str):
        self.args = args
        self.prop = prop
        self.enabled = prop in PROVES and not getattr(args, "no_property_precheck", False)
        self.status = "disabled" if not self.enabled else "not_started"
        self.proven = False
        self.reason: str | None = None
        self.duration: float | None = None
        self.version: str | None = None

    @staticmethod
    def _is_swat_library(path: str) -> bool:
        """Z3's Java bindings, which SWAT itself puts on the target's classpath: not part of the program."""
        return os.path.basename(path) == "com.microsoft.z3.jar"

    def jar(self) -> str | None:
        """The check's jar: --precheck-jar, else the first of DEFAULT_JARS that exists."""
        given = getattr(self.args, "precheck_jar", None)
        if given:
            return given
        return next((os.path.normpath(p) for p in DEFAULT_JARS if os.path.isfile(p)), None)

    def command(self) -> list[str]:
        classpath = [os.path.abspath(p) for p in self.args.classpath if not self._is_swat_library(p)]
        return [self.args.java_path, "-Xmx2g", "-cp", self.jar(), CHECK_CLASS, *classpath]

    def run(self) -> bool:
        """Runs the check (blocking) and returns whether it proved the property."""
        if not self.enabled:
            return False
        start = time.perf_counter()
        if self.jar() is None:
            logger.error(f'[PRECHECK] No {JAR_NAME} (looked in {", ".join(map(os.path.normpath, DEFAULT_JARS))}); skipping the property pre-check')
            self.status = "missing_jar"
            return False
        try:
            proc = subprocess.run(self.command(), capture_output=True, text=True, timeout=CHECK_TIMEOUT_S)
            result = json.loads(proc.stdout.strip().splitlines()[-1])
            field, reason_field = PROVES[self.prop]
            self.proven = result.get(field) is True
            self.reason = result.get(reason_field)
            self.version = result.get("version")
            self.status = "proven" if self.proven else "not_proven"
        except subprocess.TimeoutExpired:
            self.status = "timeout"
        except Exception as e:
            logger.error(f'[PRECHECK] Property pre-check failed: {type(e).__name__}: {e}')
            self.status = "failed"
        self.duration = time.perf_counter() - start
        logger.info(f'[PRECHECK] {self.prop}: {self.status} after {self.duration:.2f}s ({self.reason}; cfg-extraction {self.version})')
        return self.proven

    def stats(self) -> dict:
        return {
            'enabled': self.enabled,
            'status': self.status,
            'proven': self.proven,
            'reason': self.reason,
            'duration_s': self.duration,
            'version': self.version,
        }
