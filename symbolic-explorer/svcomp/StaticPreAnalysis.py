"""
Static pre-analysis (SA) for the SV-COMP driver, run in a background thread.

The extractor is an external Java process. While it runs (and while its graph is loaded) the
explorer can already explore without pruning; the driver adopts the graph once it is ready. With
``--wait-for-sa`` the driver waits for it before the first round instead, as it used to.

The graph is built in a private ``SAGraph`` and only handed out once it is fully loaded, so the
explorer never sees a half-loaded graph. ``cancel()`` kills the extractor and joins the thread, so
no Java process outlives the explorer when exploration finishes first.
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


class SAStatus(Enum):
    DISABLED = "disabled"
    NOT_STARTED = "not_started"
    RUNNING = "running"
    LOADED = "loaded"
    FAILED = "failed"
    TIMEOUT = "timeout"
    CANCELLED = "cancelled"  # exploration finished before the pre-analysis


class StaticPreAnalysis:
    def __init__(self, args):
        self.args = args
        self.status = SAStatus.NOT_STARTED if self.enabled else SAStatus.DISABLED
        self.duration: float | None = None  # wall time from start until done or cancelled
        self._graph: SAGraph | None = None
        self._start: float | None = None
        self._thread: threading.Thread | None = None
        self._proc: subprocess.Popen | None = None
        self._lock = threading.Lock()  # orders starting the extractor against cancel()
        self._cancelled = threading.Event()
        self._done = threading.Event()

    @property
    def enabled(self) -> bool:
        return bool(self.args.sa_file or self.args.sa_path)

    def start(self):
        if not self.enabled:
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
            if self._cancelled.is_set():
                self.status = SAStatus.CANCELLED
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

    def _run_extractor(self) -> bool:
        """Runs the extractor; False if it was cancelled or timed out."""
        cmd = ["java", "-jar", os.path.join(self.args.sa_path, "build", "libs", "cfg-extractor-1.0-SNAPSHOT-all.jar"),
               ':'.join(os.path.abspath(p) for p in self.args.classpath), self.args.logdir, self.args.target, "main", "inter"]
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
            'mode': 'sequential' if self.args.wait_for_sa else 'parallel',
            'status': self.status.value,
            'failed': self.status in (SAStatus.FAILED, SAStatus.TIMEOUT),
            'timed_out': self.status == SAStatus.TIMEOUT,
            'cancelled': self.status == SAStatus.CANCELLED,
            'duration_s': self.duration,
            # Round whose branch selection first used the graph; None if it never was.
            'adopted_at_round': adopted_at_round,
        }
