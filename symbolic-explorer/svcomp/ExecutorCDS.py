"""Class data sharing (AppCDS) archive for the executor JVMs of one verification run.

Every exploration round starts a new executor JVM, which loads, parses and verifies the same few
thousand classes again: those of the agent jar (ASM, Jackson, logback, the executor itself) and the
JDK classes outside the JDK's default CDS archive. A static AppCDS archive of these classes, dumped
once per run, lets each later JVM map them instead (about a quarter of a round's CPU time).

Only how classes are loaded changes, never which bytes: the JVM still offers every archived class
to the agent's ClassFileTransformers, and a class a transformer changes is parsed from the new
bytes as before. Task classes come from directories, which CDS does not archive.

Layout: CDS archives classes from the leading jar entries of the class path only (a class path
directory before them that classes are loaded from makes the dump fail). The class path therefore
gets a prefix of all its jars plus the agent jar, in their order, before the original class path
(the agent jar is otherwise appended by the JVM behind it). This is done only when no file of a
moved jar also exists in an entry that preceded it, so that every class and resource name still
resolves to the same file. The prefix is used from round 0 on, so all rounds load classes alike.

Steps: round 0 writes the list of loaded classes (-XX:DumpLoadedClassList); before round
DUMP_AT_ROUND the archive is dumped (-Xshare:dump, about one and a half rounds of CPU time, so
short runs do not pay for it); later rounds use it (-XX:SharedArchiveFile). If any step fails the
run goes on without the archive; a JVM that cannot use an archive (e.g. a jar changed) silently
runs without it (-Xshare:auto, the default).
"""
import os
import shutil
import subprocess
import tempfile
import time
import zipfile

import log

logger = log.get_logger()

# The round before which the archive is dumped: runs of fewer rounds do not pay for the dump
DUMP_AT_ROUND = 3
# Do not scan class path directories with more files than this for name clashes
MAX_SCAN_FILES = 50_000
DUMP_TIMEOUT_S = 120


def _names(entry: str) -> set[str] | None:
    """The file names (as class path resource names) of a class path entry; None if unknown."""
    if os.path.isdir(entry):
        names = set()
        for root, _, files in os.walk(entry):
            rel = os.path.relpath(root, entry)
            for f in files:
                names.add(f if rel == '.' else f'{rel}/{f}'.replace(os.sep, '/'))
                if len(names) > MAX_SCAN_FILES:
                    return None
        return names
    if os.path.isfile(entry):
        try:
            with zipfile.ZipFile(entry) as z:
                return {n for n in z.namelist() if not n.endswith('/')}
        except (zipfile.BadZipFile, OSError):
            return None
    return set()  # a missing entry contributes nothing


def class_path_layout(cp: list[str], agent: str) -> list[str] | None:
    """The class path with the jar prefix (see the module doc), or None if moving the jars could
    change which file a class or resource name resolves to."""
    effective = list(cp) + [agent]
    prefix: list[str] = []
    for e in effective:
        if e.endswith('.jar') and os.path.isfile(e) and e not in prefix:
            prefix.append(e)
    if not prefix:
        return None
    cache: dict[str, set[str] | None] = {}

    def names(e):
        if e not in cache:
            cache[e] = _names(e)
        return cache[e]

    for k, jar in enumerate(prefix):
        moved = names(jar)
        if moved is None:
            return None
        # Entries that came before the jar and do not now (all but the earlier prefix jars)
        for e in effective[:effective.index(jar)]:
            if e in prefix[:k]:
                continue
            other = names(e)
            if other is None or moved & other:
                return None
    return prefix + list(cp)


class ExecutorCDS:
    def __init__(self, enabled: bool, base_dir: str | None = None, dump_at_round: int = DUMP_AT_ROUND):
        self.enabled = enabled
        self.base_dir = base_dir
        self.dump_at_round = dump_at_round
        self.dir: str | None = None
        self.layout: list[str] | None = None
        self.archive: str | None = None
        self.classlist: str | None = None
        self.dumped = False
        self.dump_cpu_s = 0.0
        self.dump_wall_s = 0.0
        self._layout_done = False

    def class_path(self, cp: list[str], agent: str) -> list[str]:
        """The class path for the executor JVM (computed once per run; the same in every round,
        also after a failed dump)."""
        if self._layout_done or not self.enabled:
            return self.layout or cp
        if not self._layout_done:
            self._layout_done = True
            try:
                self.layout = class_path_layout(cp, agent)
            except Exception as e:  # never fail a run over an optimisation
                logger.warning(f'[CDS] class path layout failed: {e}')
                self.layout = None
            if self.layout is None:
                logger.info('[CDS] jars cannot be moved to the front of the class path, CDS is off')
                self.enabled = False
                return cp
            try:
                self.dir = tempfile.mkdtemp(prefix='swat-cds-', dir=self.base_dir)
            except OSError as e:
                logger.warning(f'[CDS] no directory for the archive: {e}')
                self.enabled = False
                self.layout = None
                return cp
            self.classlist = os.path.join(self.dir, 'classes.lst')
            self.archive = os.path.join(self.dir, 'executor.jsa')
        return self.layout if self.layout is not None else cp

    def jvm_options(self, round_idx: int) -> list[str]:
        """The CDS options of the executor JVM of the given round."""
        if not self.enabled or self.dir is None:
            return []
        if round_idx == 0:
            return [f'-XX:DumpLoadedClassList={self.classlist}']
        if self.dumped:
            return [f'-XX:SharedArchiveFile={self.archive}']
        return []

    def prepare_round(self, round_idx: int, java_path: str, jvm_options):
        """Dumps the archive before round dump_at_round; jvm_options() gives the executor's heap and
        other JVM options (the dump has to use the same heap layout, e.g. compressed oops)."""
        if not self.enabled or self.dumped or self.dir is None or round_idx < self.dump_at_round:
            return
        self.enabled = False  # one attempt; set again below on success
        if not (self.classlist and os.path.isfile(self.classlist) and os.path.getsize(self.classlist) > 0):
            logger.info('[CDS] no class list from round 0, CDS is off')
            return
        prefix = [e for e in self.layout if e.endswith('.jar')]
        prefix = prefix[:next((i for i, e in enumerate(self.layout) if not e.endswith('.jar')), len(prefix))]
        cmd = [java_path, *jvm_options(), '-Xshare:dump', f'-XX:SharedClassListFile={self.classlist}',
               f'-XX:SharedArchiveFile={self.archive}', '-cp', ':'.join(prefix)]
        before = os.times()
        start = time.perf_counter()
        try:
            with open(os.path.join(self.dir, 'dump.log'), 'w') as out:
                p = subprocess.run(cmd, stdout=out, stderr=subprocess.STDOUT, timeout=DUMP_TIMEOUT_S)
            ok = p.returncode == 0 and os.path.isfile(self.archive)
        except (OSError, subprocess.SubprocessError) as e:
            logger.warning(f'[CDS] archive dump failed: {e}')
            ok = False
        after = os.times()
        self.dump_wall_s = time.perf_counter() - start
        self.dump_cpu_s = (after.children_user + after.children_system) - (before.children_user + before.children_system)
        if ok:
            self.enabled = self.dumped = True
            logger.info(f'[CDS] archive dumped before round {round_idx} '
                        f'({os.path.getsize(self.archive) >> 20} MiB, {self.dump_cpu_s:.2f} s CPU)')
        else:
            logger.warning(f'[CDS] archive dump failed (see {self.dir}/dump.log), CDS is off')

    def cleanup(self):
        if self.dir is not None:
            shutil.rmtree(self.dir, ignore_errors=True)
            self.dir = None
