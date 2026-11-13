"""
Class preparation for the SV-COMP driver: SWAT's semantic rewrites, applied once, ahead of time.

The executor's agent rewrites every application class before instrumenting it (de-interned
strings and boxes, reference comparisons through ``UtilInstrumented.refEquals``, the SV-COMP
``Verifier`` calls redirected to SWAT's; executor ``instrument/rewrite/Rewrites``). The static
analysis, run on the classes on disk, would analyse different code: other local slots, other
instructions. Preparation applies the same rewrites before both, with the executor's own code
(``de.uzl.its.swat.prepare.Prepare`` in the agent jar, the configuration of the run):

- it copies each directory of the classpath to ``<logdir>/prepared/<i>``, with every class the
  rewrites change replaced by its rewritten version, marked as prepared;
- the extractor (graph, branch keys, relevance and loop facts) and the executor get that prepared
  classpath; the agent recognizes the marker and does not rewrite those classes again, it only
  instruments them. Classes it has no prepared version of (jars, classes generated at run time) it
  still rewrites at load time, with the same code.

The property pre-check keeps running on the original classes: it models the SV-COMP ``Verifier``
and a whitelist of JDK methods, and SWAT's helpers would be unknown calls to it. A proof on the
original program is what the verdict needs.

If preparation fails, the run goes on with the original classpath, as before (the agent then
rewrites everything at load time, and loop facts mostly come from the executor's own analysis);
a failure never costs a verdict. Off with --no-prepare-classes.
"""

import json
import os
import shutil
import subprocess
import time
from typing import Any

import log

logger = log.get_logger()

PREPARE_CLASS = "de.uzl.its.swat.prepare.Prepare"
PREPARE_TIMEOUT_S = 60


class ClassPreparation:
    def __init__(self, args: Any) -> None:
        self.args = args
        self.enabled: bool = not getattr(args, "no_prepare_classes", False)
        self.status: str = "disabled" if not self.enabled else "not_started"
        self.classpath: list[str] | None = None  # the prepared classpath, once preparation succeeded
        self.duration: float | None = None
        self.prepared: int = 0
        self.copied: int = 0
        self.failed: list[str] = []
        self.reason: str | None = None

    @property
    def out_dir(self) -> str:
        return os.path.join(getattr(self.args, "logdir", None) or "logs", "prepared")

    def command(self) -> list[str]:
        return [getattr(self.args, "java_path", None) or "java", "-Xmx1g",
                f"-Dconfig.path={self.args.config}",
                *(["-DuserFacts=true"] if getattr(self.args, "user_facts", False) else []),
                "-cp", self.args.agent, PREPARE_CLASS,
                self.out_dir, *[os.path.abspath(p) for p in self.args.classpath]]

    def run(self) -> list[str] | None:
        """Prepares the classpath (blocking); returns the prepared classpath, or None to use the original."""
        if not self.enabled:
            return None
        start = time.perf_counter()
        try:
            shutil.rmtree(self.out_dir, ignore_errors=True)
            proc = subprocess.run(self.command(), capture_output=True, text=True, timeout=PREPARE_TIMEOUT_S)
            if proc.returncode != 0:
                raise RuntimeError(f"exit status {proc.returncode}: {proc.stderr.strip()[-500:]}")
            result = json.loads(proc.stdout.strip().splitlines()[-1])
            classpath = result["classpath"]
            if len(classpath) != len(self.args.classpath):
                raise RuntimeError(f"prepared classpath has {len(classpath)} entries, expected {len(self.args.classpath)}")
            self.prepared = result.get("prepared", 0)
            self.copied = result.get("copied", 0)
            self.failed = result.get("failed", [])
            self.classpath = classpath
            self.status = "prepared"
        except subprocess.TimeoutExpired:
            self.status = "timeout"
            self.reason = f"timed out after {PREPARE_TIMEOUT_S}s"
        except Exception as e:  # noqa: BLE001 - preparation must never cost a verdict
            self.status = "failed"
            self.reason = f"{type(e).__name__}: {e}"
        self.duration = time.perf_counter() - start
        if self.classpath is None:
            logger.warning(f'[PREPARE] Class preparation {self.status} ({self.reason}); using the original classpath')
        else:
            logger.info(f'[PREPARE] Prepared {self.prepared} classes ({self.copied} unchanged, '
                        f'{len(self.failed)} failed) in {self.duration:.2f}s')
            for f in self.failed:
                logger.warning(f'[PREPARE] Not prepared (rewritten at load time instead): {f}')
        return self.classpath

    @property
    def prepared_dirs(self) -> list[str]:
        """The prepared copies of the classpath's directories (empty unless preparation succeeded)."""
        out = os.path.abspath(self.out_dir) + os.sep
        return [p for p in (self.classpath or []) if os.path.abspath(p).startswith(out)]

    def stats(self) -> dict[str, Any]:
        return {
            'enabled': self.enabled,
            'status': self.status,
            'reason': self.reason,
            'prepared': self.prepared,
            'copied': self.copied,
            'failed': len(self.failed),
            'duration_s': self.duration,
        }
