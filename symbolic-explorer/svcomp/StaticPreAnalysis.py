"""
Static pre-analysis (SA) for the SV-COMP driver, run in a background thread.

The extractor is an external Java process. By default the driver waits for it before the first
round. With ``--parallel-sa`` the explorer already explores without pruning while it runs (and
while its graph is loaded), and the driver adopts the graph once it is ready.

The graph is built in a private ``SAGraph`` and only handed out once it is fully loaded, so the
explorer never sees a half-loaded graph. ``cancel()`` kills the extractor and joins the thread, so
no Java process outlives the explorer when exploration finishes first.

The graph's only targets are asserts (``new AssertionError`` sites), so pruning by it is only sound
for the valid-assert property; under any other property the pre-analysis is not run at all. A
graph whose program may run application code outside main's call tree (another thread, a shutdown
hook, an uncaught-exception handler) is not used either, see ``SAGraph.async_entry_sites``.
"""

import os
import subprocess
import threading
import time
import traceback
from enum import Enum

from data.StaticAnalysisGraph.SAGraph import SAGraph

import log
logger = log.get_logger()

EXTRACTOR_TIMEOUT_S = 120

# The only property whose violations are the graph's targets (assertion points).
SA_PROPERTY = "valid-assert.prp"


class SAStatus(Enum):
    DISABLED = "disabled"
    NOT_STARTED = "not_started"
    RUNNING = "running"
    LOADED = "loaded"
    FAILED = "failed"
    TIMEOUT = "timeout"
    CANCELLED = "cancelled"  # exploration finished before the pre-analysis
    REJECTED = "rejected"  # loaded, but pruning by the graph would not be sound (see rejected_reason)


class StaticPreAnalysis:
    def __init__(self, args):
        self.args = args
        self.status = SAStatus.NOT_STARTED if self.enabled else SAStatus.DISABLED
        self.duration: float | None = None  # wall time from start until done or cancelled
        self.rejected_reason: str | None = None
        self._graph: SAGraph | None = None
        self._start: float | None = None
        self._thread: threading.Thread | None = None
        self._proc: subprocess.Popen | None = None
        self._lock = threading.Lock()  # orders starting the extractor against cancel()
        self._cancelled = threading.Event()
        self._done = threading.Event()

    @property
    def requested(self) -> bool:
        return bool(self.args.sa_file or self.args.sa_path)

    @property
    def enabled(self) -> bool:
        return self.requested and self.property_supported

    @property
    def property_supported(self) -> bool:
        return os.path.basename(getattr(self.args, "property", None) or "") == SA_PROPERTY

    def start(self):
        if not self.enabled:
            if self.requested:
                logger.info(f'[EXPLORER] Static pre-analysis is disabled: its graph only targets asserts, '
                            f'so it cannot prune for property {self.args.property} (only {SA_PROPERTY}).')
            else:
                logger.info(f'[EXPLORER] Static pre-analysis is disabled.')
            self._done.set()
            return
        self._start = time.perf_counter()
        self.status = SAStatus.RUNNING
        # Daemon, so a graph still being parsed can never keep the process alive.
        self._thread = threading.Thread(target=self._run, name='static-pre-analysis', daemon=True)
        self._thread.start()

    def is_done(self) -> bool:
        return self._done.is_set()

    def wait(self):
        self._done.wait()

    def take_graph(self) -> SAGraph | None:
        """The loaded graph, once and only once; None while running, or if SA failed or is disabled."""
        if not self._done.is_set():
            return None
        graph, self._graph = self._graph, None
        return graph

    def cancel(self, join_timeout_s: float = 10.0):
        """Stop the pre-analysis if it is still running: kill the extractor and join the thread."""
        if self._thread is None or self._done.is_set():
            return
        logger.info(f'[EXPLORER] Cancelling static pre-analysis that is still running.')
        self._cancelled.set()
        with self._lock:
            proc = self._proc
        if proc is not None and proc.poll() is None:
            proc.kill()
        self._thread.join(join_timeout_s)
        if self._thread.is_alive():
            # Only possible while parsing the graph in Python: no child process is left, and the
            # daemon thread ends with the process.
            logger.warning(f'[EXPLORER] Static pre-analysis thread did not stop within {join_timeout_s}s.')
            self.status = SAStatus.CANCELLED
            self.duration = time.perf_counter() - self._start

    def _run(self):
        graph = SAGraph()
        try:
            if self.args.sa_file:
                logger.info(f'[EXPLORER] Loading static pre-analysis graph from provided file...')
                graph.load_json_graph(self.args.sa_file)
                logger.info(f'[EXPLORER] Loaded static pre-analysis graph from provided file.')
            else:
                logger.info(f'[EXPLORER] Running static pre-analysis...')
                if not self._run_extractor():
                    return
                logger.info(f'[EXPLORER] Loading static pre-analysis graph...')
                graph.load_json_graph(os.path.join(self.args.logdir, f"{self.args.target}_main_interprocedural.json"))
                logger.info(f'[EXPLORER] Loaded static pre-analysis graph.')
            async_sites = graph.async_entry_sites()
            if self._cancelled.is_set():
                self.status = SAStatus.CANCELLED
            elif async_sites:
                # An assert on another thread or after main's frame is gone is not on any path
                # the walk follows, so no branch side could be pruned soundly.
                self.rejected_reason = f'application code may run outside main\'s call tree: {", ".join(async_sites[:5])}'
                logger.info(f'[EXPLORER] Not using the static pre-analysis graph, {self.rejected_reason}')
                self.status = SAStatus.REJECTED
            else:
                self._graph = graph
                self.status = SAStatus.LOADED
        except Exception as e:
            if self._cancelled.is_set():
                self.status = SAStatus.CANCELLED
            else:
                logger.error(f'[EXPLORER] Failed to get static pre-analysis information. Exception of type {type(e).__name__}: {e}')
                logger.error(traceback.format_exc())
                self.status = SAStatus.FAILED
        finally:
            # Record on every path: a timed-out, failed or cancelled pre-analysis still costs wall time.
            self.duration = time.perf_counter() - self._start
            logger.info(f'[EXPLORER] Static pre-analysis finished with status {self.status.value} after {self.duration:.2f}s.')
            self._done.set()

    @staticmethod
    def _is_swat_library(path: str) -> bool:
        """
        A library SWAT itself puts on the target's classpath (Z3's Java bindings). It is not part of
        the program, and the extractor would treat its classes as application code: their
        toString() methods, which assert, would count as callbacks that may reach an assert.
        """
        return os.path.basename(path) == "com.microsoft.z3.jar"

    def _run_extractor(self) -> bool:
        """Runs the extractor; False if it was cancelled or timed out."""
        cmd = ["java", "-jar", os.path.join(self.args.sa_path, "build", "libs", "cfg-extractor-1.0-SNAPSHOT-all.jar"),
               ':'.join(os.path.abspath(p) for p in self.args.classpath if not self._is_swat_library(p)),
               self.args.logdir, self.args.target, "main", "inter"] \
              + (["--no-callback-targets"] if getattr(self.args, "sa_no_callback_targets", False) else [])
        with self._lock:
            if self._cancelled.is_set():
                self.status = SAStatus.CANCELLED
                return False
            # Same process group as the explorer, so the harness' killpg on a timeout reaches it too.
            self._proc = subprocess.Popen(cmd)
        try:
            returncode = self._proc.wait(timeout=EXTRACTOR_TIMEOUT_S)
        except subprocess.TimeoutExpired as e:
            self._proc.kill()
            self._proc.wait()
            logger.error(f'[EXPLORER] Failed to get static pre-analysis information. TimeoutExpired: {e}')
            self.status = SAStatus.TIMEOUT
            return False
        if self._cancelled.is_set():
            self.status = SAStatus.CANCELLED
            return False
        if returncode != 0:
            raise subprocess.CalledProcessError(returncode, cmd)
        return True

    def stats(self, adopted_at_round: int | None) -> dict:
        return {
            'enabled': self.enabled,
            'requested': self.requested,
            'property_supported': self.property_supported,
            'mode': 'parallel' if self.args.parallel_sa else 'sequential',
            'callback_targets': not getattr(self.args, "sa_no_callback_targets", False),
            'status': self.status.value,
            'failed': self.status in (SAStatus.FAILED, SAStatus.TIMEOUT),
            'timed_out': self.status == SAStatus.TIMEOUT,
            'cancelled': self.status == SAStatus.CANCELLED,
            'rejected_reason': self.rejected_reason,
            'duration_s': self.duration,
            # Round whose branch selection first used the graph; None if it never was.
            'adopted_at_round': adopted_at_round,
        }
