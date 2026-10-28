"""
The resource budget of an SV-COMP run: the memory and CPU time that the explorer and all executor
JVMs share. SV-COMP (bench-defs swat.xml) limits the whole run to 15 GB of memory and 15 min of CPU
time, summed over all processes, on 4 cores.

A monitor thread polls the resident memory and CPU time of the explorer and its descendants. Close to
a limit it stops the work in progress itself, so that the run still ends with a verdict instead of
being killed from outside:
- memory: the executor run in progress is killed (it ends without a trace, so the path it was to
  explore stays unexplored) and exploration goes on with the other branches; a Z3 query is
  interrupted (the branch counts as undecided). Either rules out a SAFE verdict.
- CPU time: exploration stops and the run ends with UNKNOWN, unless a violation was found already.
"""
import ctypes
import os
import signal
import threading
import time
from contextlib import contextmanager

import log

logger = log.get_logger()

MB = 1000 * 1000
MIB = 1024 * 1024

# BenchExec counts in SI units: memlimit "15 GB" is 15e9 bytes.
DEFAULT_MEMORY_LIMIT_MB = 15000
DEFAULT_CPU_LIMIT_S = 900

# Kept free below the memory limit: page cache and the files the run writes (which count against
# the limit in a BenchExec container), the wrapper scripts, and growth between two polls (the
# executor JVM was seen to grow by 2 GB/s).
HEADROOM_MB = 2000
# If the explorer alone is above the limit minus this, nothing can be stopped to make room.
HARD_HEADROOM_MB = 1000
# How long an interrupted Z3 query may stay over the memory threshold. Z3 does not react to an
# interrupt while it bit-blasts, during which it can allocate gigabytes in seconds. (Z3's own
# memory_max_size is no way out: Z3 then aborts the process in some places.)
INTERRUPT_GRACE_S = 1.0
# The explorer's share (Python, the execution tree, Z3). The executor JVM gets half of the rest as
# heap; the other half is for its native memory, mostly JavaSMT's Z3 context, which grows with the
# trace about as fast as the heap.
EXPLORER_SHARE_MB = 1500
MIN_HEAP_MB = 256

# Stop this much before the CPU limit: polling lag (up to 4 cores per poll interval), killing the
# executor and writing the verdict, the witness and the statistics.
CPU_MARGIN_S = 20
POLL_S = 0.2
# Finding all descendants means reading every process's parent, which is slow on a busy machine;
# in between, only the processes found last time and the executor are measured.
FULL_SCAN_S = 5
# How long the main thread may take to wind down after the CPU deadline before the monitor ends
# the run itself.
GRACE_S = 10

CLK_TCK = os.sysconf('SC_CLK_TCK')
PAGE_SIZE = os.sysconf('SC_PAGE_SIZE')


def cgroup_memory_limit(proc_root: str = '/proc', cgroup_root: str = '/sys/fs/cgroup') -> int | None:
    """The tightest memory limit (bytes) of this process's cgroup and its ancestors, or None.

    BenchExec (and a systemd scope with MemoryMax) enforce the limit through the cgroup, so it is
    the limit that actually applies when it is lower than the configured one.
    """
    return limiting_cgroup(proc_root, cgroup_root)[0]


def limiting_cgroup(proc_root: str = '/proc', cgroup_root: str = '/sys/fs/cgroup') -> tuple[int | None, str | None]:
    """(limit, directory) of the cgroup with the tightest memory limit, see cgroup_memory_limit()."""
    try:
        lines = open(f'{proc_root}/self/cgroup').read().splitlines()
    except OSError:
        return None, None
    limits = []
    for line in lines:
        parts = line.split(':', 2)
        if len(parts) != 3:
            continue
        _, controllers, path = parts
        if controllers == '':  # cgroup v2
            base, name = cgroup_root, 'memory.max'
        elif 'memory' in controllers.split(','):  # cgroup v1
            base, name = f'{cgroup_root}/memory', 'memory.limit_in_bytes'
        else:
            continue
        path = path.rstrip('/')
        while True:
            try:
                value = open(f'{base}{path}/{name}').read().strip()
                if value.isdigit() and int(value) < 2**60:  # v1 reports "no limit" as a huge number
                    limits.append((int(value), f'{base}{path}'))
            except OSError:
                pass
            if not path:
                break
            path = path.rsplit('/', 1)[0]
    return min(limits) if limits else (None, None)


def cgroup_cpu_time(directory: str | None) -> float | None:
    """CPU seconds of all processes in a cgroup (v2 cpu.stat), or None."""
    if directory is None:
        return None
    try:
        with open(f'{directory}/cpu.stat') as f:
            for line in f:
                if line.startswith('usage_usec '):
                    return int(line.split()[1]) / 1e6
    except (OSError, ValueError):
        pass
    return None


def _stat(pid: int, proc_root: str = '/proc') -> tuple[int, float, int] | None:
    """(ppid, CPU seconds, RSS bytes) of a process, or None if it is gone."""
    try:
        with open(f'{proc_root}/{pid}/stat') as f:
            fields = f.read().rsplit(')', 1)[1].split()
    except (OSError, IndexError):
        return None
    return int(fields[1]), (int(fields[11]) + int(fields[12])) / CLK_TCK, int(fields[21]) * PAGE_SIZE


def descendants(pid: int, proc_root: str = '/proc') -> dict[int, tuple[float, int]]:
    """{pid: (CPU seconds, RSS bytes)} of the live descendants of a process."""
    stats = {}
    for entry in os.listdir(proc_root):
        if entry.isdigit():
            s = _stat(int(entry), proc_root)
            if s is not None:
                stats[int(entry)] = s
    children: dict[int, list[int]] = {}
    for p, (ppid, _, _) in stats.items():
        children.setdefault(ppid, []).append(p)
    result, todo = {}, list(children.get(pid, []))
    while todo:
        p = todo.pop()
        result[p] = stats[p][1:]
        todo.extend(children.get(p, []))
    return result


def _malloc_trim():
    """Returns freed heap memory to the system (glibc keeps it otherwise, e.g. after a Z3 query)."""
    try:
        ctypes.CDLL('libc.so.6').malloc_trim(0)
    except (OSError, AttributeError):
        pass


class ResourceBudget:

    def __init__(self, memory_limit_mb: int = DEFAULT_MEMORY_LIMIT_MB, cpu_limit_s: float = DEFAULT_CPU_LIMIT_S,
                 cpu_used_s: float = 0.0, cgroup_limit: int | None = None, wall_limit_s: float | None = None):
        """cpu_used_s: CPU time the run used before the explorer started (e.g. compiling the task),
        which counts against the same limit. cgroup_limit: the cgroup's memory limit; read from the
        system unless given. wall_limit_s: the wall time the run may take (default: the CPU limit,
        as under BenchExec and the harness); a target that hangs without using CPU (a deadlock)
        would otherwise be killed before the explorer reports anything."""
        self.configured_memory_limit = memory_limit_mb * MB
        if cgroup_limit is None:
            cgroup_limit, self._cgroup_dir = limiting_cgroup()
        else:
            self._cgroup_dir = None
        self.cgroup_memory_limit = cgroup_limit
        self.memory_limit = min(self.configured_memory_limit, self.cgroup_memory_limit or self.configured_memory_limit)
        self.memory_threshold = self.memory_limit - HEADROOM_MB * MB
        self.hard_memory_threshold = self.memory_limit - HARD_HEADROOM_MB * MB
        self.heap_mib = max(MIN_HEAP_MB * MB, (self.memory_threshold - EXPLORER_SHARE_MB * MB) // 2) // MIB
        self.cpu_limit = cpu_limit_s
        self.cpu_used_before = cpu_used_s
        self.cpu_deadline = cpu_limit_s - CPU_MARGIN_S
        self.wall_limit = wall_limit_s if wall_limit_s is not None else cpu_limit_s
        self.wall_deadline = self.wall_limit - CPU_MARGIN_S
        self._started_at = time.monotonic()
        self.wall_deadline_reached = False

        self.cpu_exceeded = threading.Event()
        self.cpu_exceeded_at: float | None = None
        self.peak_rss = 0
        self.cpu_used = cpu_used_s
        self.memory_hits = 0  # how often work was stopped to stay within the memory limit
        self.solver_interruptions = 0

        self._lock = threading.Lock()
        self._executor = None  # the running executor process (subprocess.Popen)
        self._executor_stopped: str | None = None  # why the monitor killed it: 'memory' or 'cpu'
        self._solver_context = None  # the Z3 context of a running query
        self._solver_interrupted: str | None = None
        self._solver_interrupted_at = 0.0
        self._tracked: set[int] = set()  # descendants found by the last full scan
        self._last_scan = 0.0
        self._thread: threading.Thread | None = None
        self._stop = threading.Event()
        # Called (from the monitor thread) when the run must end now: the explorer alone is over
        # the memory limit, or the main thread did not wind down after the CPU deadline.
        self.on_emergency = None

    def describe(self) -> str:
        cg = f', cgroup limit {self.cgroup_memory_limit // MB} MB' if self.cgroup_memory_limit else ''
        return (f'memory limit {self.memory_limit // MB} MB{cg} (executor heap {self.heap_mib} MiB, '
                f'stop at {self.memory_threshold // MB} MB), CPU limit {self.cpu_limit:.0f} s '
                f'(stop at {self.cpu_deadline:.0f} s, {self.cpu_used_before:.1f} s used before)')

    # ---- measuring ----

    def usage(self) -> tuple[int, float]:
        """(RSS bytes, CPU seconds) of the run so far: the explorer and all its descendants, plus the
        CPU time used before the explorer started. Where the memory limit comes from a cgroup (as
        under BenchExec), the CPU time is at least that of the cgroup, which also counts the wrapper
        scripts that started the explorer and keep running next to it."""
        own = _stat(os.getpid())
        rss = own[2] if own else 0
        t = os.times()
        cpu = self.cpu_used_before + t.user + t.system + t.children_user + t.children_system
        if time.monotonic() - self._last_scan >= FULL_SCAN_S:
            self._tracked = set(descendants(os.getpid()))
            self._last_scan = time.monotonic()
        proc = self._executor
        for pid in self._tracked | ({proc.pid} if proc is not None else set()):
            st = _stat(pid)
            if st is not None:
                cpu += st[1]
                rss += st[2]
        return rss, max(cpu, cgroup_cpu_time(self._cgroup_dir) or 0.0)

    # ---- the work the monitor may stop ----

    @contextmanager
    def executor(self, proc):
        """Marks proc as the running executor. Yields a callable that tells, once it ended, whether
        the monitor stopped it, and why ('memory' or 'cpu'), or None."""
        with self._lock:
            self._executor, self._executor_stopped = proc, None
        try:
            yield lambda: self._executor_stopped
        finally:
            with self._lock:
                self._executor = None

    @contextmanager
    def solver(self, context):
        """Marks a Z3 query on context as running. Yields a callable that tells, once it returned,
        whether the monitor interrupted it, and why, or None."""
        with self._lock:
            self._solver_context, self._solver_interrupted = context, None
        try:
            yield lambda: self._solver_interrupted
        finally:
            with self._lock:
                self._solver_context = None
            if self._solver_interrupted:
                _malloc_trim()

    def _stop_work(self, reason: str) -> bool | None:
        """Stops the executor run or the Z3 query in progress: True if it was running, None if it
        is being stopped already, False if there is none."""
        with self._lock:
            proc = self._executor
            if proc is not None and proc.poll() is None:
                if self._executor_stopped:
                    return None
                self._executor_stopped = reason
                # SIGKILL: on SIGTERM the executor's shutdown hook would send the trace cut short
                # as if the target had ended there.
                for pid in [*descendants(proc.pid), proc.pid]:
                    try:
                        os.kill(pid, signal.SIGKILL)
                    except OSError:
                        pass
                return True
            if self._solver_context is not None:
                if self._solver_interrupted:
                    self._solver_context.interrupt()  # for a check started since
                    return None
                self._solver_interrupted = reason
                self._solver_interrupted_at = time.monotonic()
                self.solver_interruptions += 1
                self._solver_context.interrupt()
                return True
        return False

    # ---- the monitor ----

    def start(self):
        self._started_at = time.monotonic()
        logger.info(f'[BUDGET] {self.describe()}')
        self._thread = threading.Thread(target=self._monitor, name='resource-budget', daemon=True)
        self._thread.start()

    def stop(self):
        self._stop.set()

    def _monitor(self):
        while not self._stop.wait(POLL_S):
            try:
                self.check()
            except Exception as e:  # the monitor must not die
                logger.error(f'[BUDGET] Monitor error: {e}')

    def check(self):
        """One poll: measure, and act if a limit is near."""
        rss, cpu = self.usage()
        self.peak_rss = max(self.peak_rss, rss)
        self.cpu_used = cpu
        wall = time.monotonic() - self._started_at + self.cpu_used_before
        if wall >= self.wall_deadline and not self.cpu_exceeded.is_set():
            # Ends the run like the CPU deadline (cpu_exceeded): UNKNOWN unless a verdict is in
            logger.warning(f'[BUDGET] Wall time {wall:.1f} s reached the deadline of {self.wall_deadline:.0f} s '
                           f'(CPU {cpu:.1f} s: something waits without working, e.g. a deadlocked target), stopping')
            self.wall_deadline_reached = True
            self.cpu_exceeded_at = time.monotonic()
            self.cpu_exceeded.set()
        if cpu >= self.cpu_deadline or self.wall_deadline_reached:
            if not self.cpu_exceeded.is_set():
                logger.warning(f'[BUDGET] CPU time {cpu:.1f} s reached the deadline of {self.cpu_deadline:.0f} s, stopping')
                self.cpu_exceeded_at = time.monotonic()
                self.cpu_exceeded.set()
            # Also stops what was started after the deadline, until the main thread has wound down
            self._stop_work('cpu')
            if time.monotonic() - self.cpu_exceeded_at > GRACE_S:
                self._emergency(f'the explorer did not stop within {GRACE_S} s of the CPU deadline')
        if rss >= self.memory_threshold:
            stopped = self._stop_work('memory')
            if stopped:
                self.memory_hits += 1
                logger.warning(f'[BUDGET] Memory {rss // MB} MB reached {self.memory_threshold // MB} MB, '
                               f'stopped the work in progress')
            elif stopped is None and self._solver_context is not None:
                # An interrupted query that keeps its memory, or keeps growing
                if rss >= self.hard_memory_threshold or time.monotonic() - self._solver_interrupted_at > INTERRUPT_GRACE_S:
                    self._emergency(f'a Z3 query uses {rss // MB} MB of the {self.memory_limit // MB} MB limit and does not stop')
            elif stopped is False:
                _malloc_trim()
                if self.usage()[0] >= self.hard_memory_threshold:
                    self._emergency(f'the explorer uses {rss // MB} MB of the {self.memory_limit // MB} MB limit')

    def _emergency(self, why: str):
        logger.error(f'[BUDGET] Ending the run: {why}')
        if self.on_emergency is not None:
            self.on_emergency(why)

    def stats(self) -> dict:
        return {
            'memory_limit_mb': self.memory_limit // MB,
            'cgroup_memory_limit_mb': self.cgroup_memory_limit // MB if self.cgroup_memory_limit else None,
            'executor_heap_mib': self.heap_mib,
            'peak_rss_mb': self.peak_rss // MB,
            'memory_hits': self.memory_hits,
            'solver_interruptions': self.solver_interruptions,
            'cpu_limit_s': self.cpu_limit,
            'cpu_deadline_s': self.cpu_deadline,
            'cpu_used_s': round(self.cpu_used, 1),
            'cpu_deadline_reached': self.cpu_exceeded.is_set(),
            'wall_deadline_reached': self.wall_deadline_reached,
        }
