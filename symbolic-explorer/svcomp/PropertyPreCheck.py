"""
Static property pre-check for the SV-COMP driver.

The executor jar contains a conservative bytecode analysis (de.uzl.its.swat.staticcheck.
ExceptionFreedomCheck) of the whole program on the classpath. It proves, or fails to prove:

- for no-runtime-exception: no RuntimeException can escape Main.main or a static initializer;
- for valid-assert: no AssertionError is ever created, so no assert can fail.

A proven property holds on every execution, independently of what the symbolic exploration can
model, so the driver may answer SAFE even where context loss, precision loss, uncaught exceptions
or an unexhausted path space would otherwise prevent it. A failed, crashed or timed-out check
proves nothing, and the driver proceeds as without it.
"""

import json
import os
import subprocess
import time

import log
logger = log.get_logger()

CHECK_CLASS = "de.uzl.its.swat.staticcheck.ExceptionFreedomCheck"
CHECK_TIMEOUT_S = 60

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

    @staticmethod
    def _is_swat_library(path: str) -> bool:
        """Z3's Java bindings, which SWAT itself puts on the target's classpath: not part of the program."""
        return os.path.basename(path) == "com.microsoft.z3.jar"

    def command(self) -> list[str]:
        classpath = [os.path.abspath(p) for p in self.args.classpath if not self._is_swat_library(p)]
        return [self.args.java_path, "-Xmx2g", "-cp", self.args.agent, CHECK_CLASS, *classpath]

    def run(self) -> bool:
        """Runs the check (blocking) and returns whether it proved the property."""
        if not self.enabled:
            return False
        start = time.perf_counter()
        try:
            proc = subprocess.run(self.command(), capture_output=True, text=True, timeout=CHECK_TIMEOUT_S)
            result = json.loads(proc.stdout.strip().splitlines()[-1])
            field, reason_field = PROVES[self.prop]
            self.proven = result.get(field) is True
            self.reason = result.get(reason_field)
            self.status = "proven" if self.proven else "not_proven"
        except subprocess.TimeoutExpired:
            self.status = "timeout"
        except Exception as e:
            logger.error(f'[PRECHECK] Property pre-check failed: {type(e).__name__}: {e}')
            self.status = "failed"
        self.duration = time.perf_counter() - start
        logger.info(f'[PRECHECK] {self.prop}: {self.status} after {self.duration:.2f}s ({self.reason})')
        return self.proven

    def stats(self) -> dict:
        return {
            'enabled': self.enabled,
            'status': self.status,
            'proven': self.proven,
            'reason': self.reason,
            'duration_s': self.duration,
        }
