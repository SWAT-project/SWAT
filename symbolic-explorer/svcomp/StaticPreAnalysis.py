"""
Static pre-analysis (SA) for the SV-COMP driver, run in a background thread.

The extractor is an external Java process. By default the driver waits for it before the first
round. With ``--parallel-sa`` the explorer already explores without pruning while it runs (and
while its graph is loaded), and the driver adopts the graph once it is ready.

The graph is built in a private ``SAGraph`` and only handed out once it is fully loaded, so the
explorer never sees a half-loaded graph. ``cancel()`` kills the extractor and joins the thread, so
no Java process outlives the explorer when exploration finishes first.

The graph names the target points of two properties: asserts (``new AssertionError`` sites) for
valid-assert, and the sites that may raise a RuntimeException for no-runtime-exception (found with
the property pre-check's value analysis, see cfg-extraction's RaiseSites). Under any other property
the pre-analysis is not run at all. A graph whose program may run application code outside main's
call tree (another thread, a shutdown hook, an uncaught-exception handler) is not used either, see
``SAGraph.async_entry_sites``.
"""

import argparse
import os
import subprocess
import threading
import time
import traceback
from enum import Enum
from typing import Any

from data.StaticAnalysisGraph.SAGraph import SAGraph
from svcomp.ResourceBudget import SA_MIN_HEAP_MIB

import log
logger = log.get_logger()

EXTRACTOR_TIMEOUT_S = 120
# The extractor's fat jar in a cfg-extraction checkout (--sa-path); --sa-jar names the jar itself.
EXTRACTOR_JAR = os.path.join("build", "libs", "cfg-extractor-1.0-SNAPSHOT-all.jar")
# The extractor writes this file first, into the log directory (cfg-extraction's Main.LOOP_FACTS_FILE).
LOOP_FACTS_FILE = "loop_facts.json"
# The class the SV-COMP executor runs (SVCompDriver.build_command): the graph's entry without --target.
SVCOMP_MAIN_CLASS = "Main"

# The properties the graph has target points for, and the metadata field that lists them.
SA_TARGETS = {
    "valid-assert.prp": "assertionPointIds",
    "no-runtime-exception.prp": "runtimeExceptionPointIds",
}


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
    def __init__(self, args: argparse.Namespace) -> None:
        self.args = args
        # -Xmx (MiB) for the extractor, and whether the budget is too small to run it at all. Set by
        # set_budget() from the resource budget; until then no explicit heap is passed (as in tests).
        self._sa_heap_mib: int | None = None
        self._sa_disabled_by_budget: bool = False
        self.status = SAStatus.NOT_STARTED if self.enabled else SAStatus.DISABLED
        self.duration: float | None = None  # wall time from start until done or cancelled
        self.rejected_reason: str | None = None
        self._graph: SAGraph | None = None
        self._start: float | None = None
        self._thread: threading.Thread | None = None
        self._proc: subprocess.Popen[bytes] | None = None
        self._lock = threading.Lock()  # orders starting the extractor against cancel()
        self._cancelled = threading.Event()
        self._done = threading.Event()
        # Whether to compute which branches are irrelevant to the targets (see Relevance)
        self.slicing = not getattr(args, "no_sa_slicing", False)
        self.relevance_stats: dict[str, Any] | None = None  # see RelevanceStats

    @property
    def requested(self) -> bool:
        return bool(self.args.sa_file or self.args.sa_path or getattr(self.args, "sa_jar", None))

    @property
    def extractor_jar(self) -> str:
        """The extractor to run: --sa-jar, else the fat jar in the --sa-path checkout."""
        sa_jar = getattr(self.args, "sa_jar", None)
        if sa_jar:
            return str(sa_jar)
        return os.path.join(self.args.sa_path, EXTRACTOR_JAR)

    @property
    def enabled(self) -> bool:
        return self.requested and self.property_supported and not self._sa_disabled_by_budget

    def set_budget(self, sa_heap_mib: int | None) -> None:
        """Sets the extractor's heap from the resource budget. None means the budget is too small to
        run the pre-analysis (see ResourceBudget.sa_heap_mib), which is then skipped instead of run
        into an out-of-memory error. Call before start()."""
        self._sa_heap_mib = sa_heap_mib
        self._sa_disabled_by_budget = sa_heap_mib is None
        self.status = SAStatus.NOT_STARTED if self.enabled else SAStatus.DISABLED

    @property
    def property_supported(self) -> bool:
        return self.targets is not None

    @property
    def entry(self) -> tuple[str, str]:
        """
        The class and method the graph starts at: main of the target class, or the method that
        --sa-entry names (`pkg.Class#method`, e.g. an HTTP endpoint, where the executor starts
        tracing). The extractor finds the method by name, so it must not be overloaded.
        """
        sa_entry = getattr(self.args, "sa_entry", None)
        if sa_entry:
            entry_class, _, entry_method = sa_entry.partition("#")
            return entry_class, entry_method
        # The SV-COMP executor always runs Main, and the SV-COMP archive passes no --target.
        return self.args.target or SVCOMP_MAIN_CLASS, "main"

    @property
    def graph_file(self) -> str:
        """Where the extractor writes the graph: named after the entry's simple class name and method."""
        entry_class, entry_method = self.entry
        return os.path.join(self.args.logdir, f"{entry_class.rsplit('.', 1)[-1]}_{entry_method}_interprocedural.json")

    @property
    def loop_facts_file(self) -> str:
        """Where the extractor writes the loop facts (cfg-extraction's Main.LOOP_FACTS_FILE): the loop
        heads and summaries of every class on the classpath, which the executor reads instead of
        computing them (it computes those of any method the file lacks, so the file is optional)."""
        return os.path.join(self.args.logdir, LOOP_FACTS_FILE)

    @property
    def targets(self) -> str | None:
        """The graph's metadata field with the target points of the property, or None."""
        return SA_TARGETS.get(os.path.basename(getattr(self.args, "property", None) or ""))

    def start(self) -> None:
        if not self.enabled:
            if self._sa_disabled_by_budget:
                logger.info(f'[EXPLORER] Static pre-analysis is disabled: the memory budget is too small '
                            f'for the extractor (needs about {SA_MIN_HEAP_MIB} MiB of heap).')
            elif self.requested:
                logger.info(f'[EXPLORER] Static pre-analysis is disabled: its graph has no target points '
                            f'for property {self.args.property} (only {", ".join(SA_TARGETS)}).')
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

    def wait(self) -> None:
        self._done.wait()

    def take_graph(self) -> SAGraph | None:
        """The loaded graph, once and only once; None while running, or if SA failed or is disabled."""
        if not self._done.is_set():
            return None
        graph, self._graph = self._graph, None
        return graph

    def cancel(self, join_timeout_s: float = 10.0) -> None:
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
            self.duration = self._elapsed()

    def _elapsed(self) -> float:
        """The wall time since start()."""
        assert self._start is not None, "not started"
        return time.perf_counter() - self._start

    def _run(self) -> None:
        targets = self.targets
        assert targets is not None  # (only started if enabled)
        graph = SAGraph()
        try:
            if self.args.sa_file:
                logger.info(f'[EXPLORER] Loading static pre-analysis graph from provided file...')
                graph.load_json_graph(self.args.sa_file, targets, slicing=self.slicing)
                logger.info(f'[EXPLORER] Loaded static pre-analysis graph from provided file.')
            else:
                logger.info(f'[EXPLORER] Running static pre-analysis...')
                if not self._run_extractor():
                    return
                logger.info(f'[EXPLORER] Loading static pre-analysis graph...')
                graph.load_json_graph(self.graph_file, targets, slicing=self.slicing)
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
                if graph.relevance_stats is not None:
                    self.relevance_stats = graph.relevance_stats.as_dict()
                    logger.info(f'[EXPLORER] Relevance: {self.relevance_stats}')
        except Exception as e:
            if self._cancelled.is_set():
                self.status = SAStatus.CANCELLED
            else:
                logger.error(f'[EXPLORER] Failed to get static pre-analysis information. Exception of type {type(e).__name__}: {e}')
                logger.error(traceback.format_exc())
                self.status = SAStatus.FAILED
        finally:
            # Record on every path: a timed-out, failed or cancelled pre-analysis still costs wall time.
            self.duration = self._elapsed()
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
        cmd = ["java",
               # Explicit heap: the extractor scans the JDK/JRT (~1.5 GiB), and the JVM's container
               # default (~25% of the cgroup) is far too little under a tight limit and OOMs. Sized
               # to the budget by set_budget(); absent only when no budget was set (e.g. tests).
               *([f"-Xmx{self._sa_heap_mib}m"] if self._sa_heap_mib is not None else []),
               "-jar", self.extractor_jar,
               ':'.join(os.path.abspath(p) for p in self.args.classpath if not self._is_swat_library(p)),
               self.args.logdir, *self.entry, "inter"] \
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

    def stats(self, adopted_at_round: int | None) -> dict[str, Any]:
        return {
            'enabled': self.enabled,
            'requested': self.requested,
            'property_supported': self.property_supported,
            'mode': 'parallel' if getattr(self.args, "parallel_sa", False) else 'sequential',
            'callback_targets': not getattr(self.args, "sa_no_callback_targets", False),
            'slicing': self.slicing,
            'relevance': self.relevance_stats,
            'status': self.status.value,
            'failed': self.status in (SAStatus.FAILED, SAStatus.TIMEOUT),
            'timed_out': self.status == SAStatus.TIMEOUT,
            'cancelled': self.status == SAStatus.CANCELLED,
            'rejected_reason': self.rejected_reason,
            'duration_s': self.duration,
            # Round whose branch selection first used the graph; None if it never was.
            'adopted_at_round': adopted_at_round,
        }
