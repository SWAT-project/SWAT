# SWAT

Dynamic symbolic execution for Java:

- `symbolic-executor/` is the Java agent that instruments bytecode and records execution traces.
- `symbolic-explorer/` is the Python driver that builds the execution tree, picks branches to
  flip and calls the solver.
- `targets/sv-comp/` holds the SV-COMP harness and benchmarks.

## Static pre-analysis (SA)

The explorer can prune its search with a control-flow graph from the extractor in
`../cfg-extraction`. That graph is loaded by `symbolic-explorer/data/StaticAnalysisGraph/SAGraph.py`
and used in `strategy/DFS.py`.

- **Lockstep walk:** `dfs` walks the execution tree and the SA graph together, pairing branch for
  branch **by position**. If the two disagree on how many branches a path has, pruning uses the
  wrong nodes and can produce false SAFE verdicts. Any change to what the executor reports as a
  branch, including the transformers under `symbolic-executor/.../instrument/`, needs a matching
  change in the extractor.
- **Keys check the walk:** every trace branch carries a key (`owner.name(desc)|line|kind|ordinal`,
  `instrument/BranchKeys.java`, named in `InstructionMethodAdapter.nameInstruction`) and the frame
  stack change since the previous branch (`ShadowContext.takeStackDelta`). `dfs` compares them with
  the graph node's key and its own call stack (`SAGraph.check_branch`): a mismatch logs
  `[DFS] SA desync`, stops the walk on that path and records `sa_desync`, which turns SAFE into
  UNKNOWN. When the walk has no node, `SAGraph.resync` finds the next branch by key, with the call
  stack rebuilt from the trace. The extractor computes the same keys; change both together.
- **Guards that cannot raise:** a phantom guard the extractor marks `cannotRaise` (the property
  pre-check proves its implicit exception cannot happen, cfg-extraction `RaiseSites`) gets a dead
  node as its exception side in `SAGraph.load_json_graph`, so that side is never interesting.
- **Concrete branches count:** they are in the tree too. Filtering to symbolic branches happens
  after `dfs`.
- **Walker giving up is safe:** when `walk_till_branch` returns `None`, the walker has no
  information, and `dfs` treats everything as interesting. That fallback is always safe.
- **Call sites with several callees** (exact dispatch to known overrides): `walk_till_branch`
  stops there, and the walk resyncs inside whichever callee ran; summaries take the union.
- **Library callbacks that may reach an assert** make the extractor count every library call as
  an assertion point; `--sa-no-callback-targets` (and `svcomp test run --sa-no-callback-targets`)
  turns that off, which is unsound. The extractor gets the target's classpath without Z3's jar
  (`StaticPreAnalysis._is_swat_library`): its classes would count as application code.
- **`<clinit>` and callbacks are masked:** static initializers are not in the graph, and neither is
  application code that library code calls back (the executor flags those branches `callback`,
  see `ShadowContext.inCallback`), so the walk is frozen inside them.
- **Targets per property:** pruning aims at the graph's target points for the property being
  checked (`StaticPreAnalysis.SA_TARGETS`): assertion points (`assertionPointIds`) under
  valid-assert, and under no-runtime-exception the sites that may raise a RuntimeException plus the
  `uncaught[T]` nodes of RuntimeException types (`runtimeExceptionPointIds`, from the property
  pre-check's value analysis, cfg-extraction `RaiseSites`). `SANode.isAssert` marks a target point
  of either kind. Later targets include shell calls and SQL statement building, so prefer changes
  that work for any kind of target point.
  - **No-runtime-exception is opt-in in the harness** (`svcomp test run --sa-no-runtime-exception`;
    the explorer prunes whenever it gets `--sa-path`). On the 736 tasks (300 s, 2026-10-24) it is
    sound (no false SAFE) and cuts queries on the solved SAFE tasks by a third, but solves nothing
    new: the pre-check already proves 426 tasks and the extractor costs 3 to 6 s per task. (Its
    first run also lost 9 SAFE to desyncs, fixed since: see SWAT helpers below.)
- **SWAT helpers and `nondetObject` in the walk:** branches taken inside SWAT's own helpers (the
  reference-comparison rewrite `UtilInstrumented.refEquals`) are flagged like callbacks
  (`ShadowContext.inSwatHelper`), since how many there are depends on runtime types; the graph has
  none of them. `Verifier.nondetObject(type, factory)`, which the executor runs as
  `factory.createObject()`, is expanded and keyed as that call by the extractor. Before these, 73 of
  978 valid-assert tasks had an SA desync.
- **Exceptions the graph does not model** (null dereferences, casts, library calls, Errors): the
  extractor gives them pseudo types (`?Exception`, `?Error`) whose escapes SAGraph treats as
  caught by any handler listed for them (`unknown_handlers`), so paths into such handlers stay
  interesting. When one actually happens, the executor records an `UNMODELLED_EXCEPTION` special
  event and `dfs` stops trusting the graph until it finds it again at a later branch by key
  (resync, see below).
- **SA runs before exploring by default:** in sv-comp mode exploration waits for
  `svcomp/StaticPreAnalysis.py`, so the graph is used from round 0 and runs are deterministic.
  `--parallel-sa` instead runs the extractor in a thread while exploration starts unpruned;
  `SVCompDriver.adopt_sa_graph` switches to the graph once it is loaded, and it is cancelled
  (extractor killed) if exploration finishes first. That hides SA bugs whenever the graph loads
  too late, and makes results depend on load. In parallel runs the `static_pre_analysis` timing
  stage is 0 and its duration is `static_pre_analysis_wall`; `run_info.json` in the run dir records
  the SA mode.

Tests for the walk, runnable without a SWAT run:

```bash
cd symbolic-explorer && python3 -m unittest tests.test_sa_walk -v
python3 tests/check_marking_on_graphs.py <graph.json>...   # checks marking against an explicit search
../targets/sv-comp/scripts/.venv/bin/python3 -m unittest tests.test_divergence -v   # needs the venv
```

`dfs` only descends into real `data.BinaryExecutionTree.Node` instances. In tests, build them with
`Node.__new__(Node)` and set the fields directly. `pytest` is not installed; use `unittest`.

## Decoding executor iids

The branch ids in the explorer log, e.g. `[DFS] @<iid>/<sa node>`, are packed as
`[loop count:6][switch case:4][cid:19][mid:11][inst:24]`. See `GlobalStateForInstrumentation`.

- `cid` is the class index, in load order.
- `mid` is the method index, in declaration order (as `javap -p` lists methods).

Decoding the iids tells you which method each event really came from. That is the quickest way to
spot a walk that has fallen out of step with the execution.

## SV-COMP runs

```bash
cd targets/sv-comp && ./scripts/svcomp test run --mode parallel --no-witness --categories valid-assert.prp
./scripts/svcomp test run --help      # --suite, --limit-nr-tests, --no-sa, --parallel-sa, --sa-rank-distance, --search-order, --solver-rlimit, --testcase-timeout-s ...
```

- **Duration:** a full valid-assert run takes about 40 min with the default 900 s timeout, and
  much longer with `--testcase-timeout-s 3600`. Only compare runs made with the same timeout.
- **Resource budget:** as in SV-COMP, the explorer and all executor JVMs share one memory and CPU
  budget (`--memory-limit` MB, default 15000; `--cpu-limit` s, default 900; the harness passes
  `--testcase-timeout-s` as the CPU limit). The executor heap is derived from the memory limit (a
  lower cgroup limit wins). Close to a limit the explorer kills the executor run or interrupts the
  Z3 query in progress; 20 s before the CPU limit it ends with UNKNOWN. Both rule out SAFE, see
  `symbolic-explorer/svcomp/ResourceBudget.py` and `resources` in `stats.json`.
- **Output:** results go to `targets/sv-comp/runs/run_<timestamp>/`. Per-task results are in
  `results/results_<prp>_*.json`, and per-task logs (`explorer.log`,
  `Main_main_interprocedural.json`, `stats.json`) are in `logs/<suite>/<task>_<prp>/`.
- **One run at a time:** runs use this working tree live. Every task re-imports
  `symbolic-explorer/` and uses `../cfg-extraction/build/libs/`. Ports are pre-assigned upward
  from 9000 when a run starts. So don't start a run while another still has live tasks (check
  `ps --ppid <svcomp.py pid>`), and remember that editing either repo affects a run in progress.
- **Solver limits:** `--solver-rlimit N` gives each branch's first query N Z3 resource units
  (deterministic, unlike a timeout; slow queries run at about 2.5M units/s, so 25000000 is about
  10 s). A branch that runs out is retried later with 5x the limit, after the cheaper queries; a
  lone retry runs unlimited. Off by default. Each query's time and rlimit use are in the
  `[SOLVER]` debug lines of `explorer.log`.
- **Property pre-check:** before round 0, `svcomp/PropertyPreCheck.py` runs cfg-extraction's
  `cfg-extractor-precheck.jar` (`de.uzl.its.cfg.staticcheck.ExceptionFreedomCheck`, an ASM pass,
  about 0.2 s). A proof of no-runtime-exception or valid-assert gives SAFE without exploring
  (round 0 still runs as a cross-check). The jar comes from `--precheck-jar`, else from next to
  `symbolic-explorer/` (the SV-COMP archive), else `../cfg-extraction/build/libs/`; without it the
  check is skipped (`missing_jar`). CI builds it at the cfg-extraction tag in `cfg-extraction.ref`
  and the archive ships it as `cfg-extraction/cfg-extractor-precheck.jar`. Its soundness gate is
  `cfg-extraction/scripts/precheck_gate.py` (0 contradictions over sv-benchmarks).
- **Search order:** `--search-order` picks which open branch is flipped next (`StrategyService`):
  `dfs` (default), `shallow`, `coverage` (sides no trace took yet, then the shallowest),
  `coverage-dfs`, `generational`. Plain DFS keeps unrolling input-parsing loops one char per round
  with ever larger queries; `coverage` found 6 more violations than `dfs` on 978 valid-assert tasks
  (300 s, no SA) and lost none. The order changes only how fast violations are found, not SAFE.
- **Int/string queries:** `solver/Int2BvLifting.py` rewrites the executor's bit-vector ints where
  they meet string lengths, indices and char codes into Integer arithmetic before solving, and
  solves bit-vector inputs used only as numbers over Integer stand-ins (`lift_inputs`, mapped back
  to the inputs by `Z3Handler.map_lifted_inputs`). Mixed `int_to_bv`/`ubv_to_int` queries are
  what makes Z3 slow on string code.
- **Benchmarks:** `targets/sv-comp/sv-benchmarks/` is a sparse checkout of `java/` from
  SWAT-project/SV-Benchmarks, which mirrors upstream sv-benchmarks with precompiled `.class` files;
  `.last_sv_commit` names the upstream commit. Older suites are kept next to it as
  `sv-benchmarks-<date>/` (currently `sv-benchmarks-2025-11-03`, which all runs before the switch on 2026-09-30
  used); select one with `--benchmark-dir`, and only compare scores of runs on the same
  suite. To update: move the current suite aside, then `./scripts/svcomp setup checkout-benchmarks`
  (`--source <local clone>` if GitHub is not reachable over SSH).
- **SA benchmarks:** `targets/sv-comp/swat-benchmarks/` holds generated tasks that measure SA
  pruning, and `sa-soundness`, SAFE/VIOLATION pairs where pruning can go wrong (run with
  `--benchmark-dir swat-benchmarks --suite sa-soundness`). See its README and RESULTS.md, which
  also lists the known SA and executor false SAFEs.
- **Local label fixes:** tasks whose upstream label is wrong are fixed in the suite checkout
  itself, as commits there (`git -C targets/sv-comp/sv-benchmarks log`) with a comment in the
  task's `.yml`. A fresh checkout drops them, so re-apply any that upstream has not fixed yet:
  - `argv-tasks/ReverseInterpolator_true`: valid-assert is false (float rounding, e.g. input
    16.84626f); upstream MR 1730 is still open.
  - `argv-tasks/PieSegment_true`: valid-assert is false (`angle % 360` is negative for negative
    angles); upstream MR 1675 is still open.
  - `argv-tasks/AlbersProjection_true`: valid-assert is false (the early return for small `Te`
    skips the finite check, e.g. qs = 0, Te = 0 returns 0); no upstream MR yet (MR 1703 was
    undone by commit b4857700).
- **Verdicts:** false SAFE (`violation -> safe`) is the outcome that matters most, and a pruning
  bug causes exactly that. Check any new one before trusting a run.
- **String inputs:** the explorer passes solved Strings as `-Dswat.input...` escaped by
  `escape_java_string` (`driver/SVCompDriver.py`), and the executor's `Verifier.nondetString`
  undoes it (`unescape`). Change both together. Solved Strings are read from Z3 by code point
  (`SolverHandler.encode_value`); `as_string()` output is ambiguous.
- **Soundness flags that downgrade SAFE to UNKNOWN:** symbolic context loss is also recorded when
  instrumented code runs untracked ("unknown space", e.g. a callback from library code), and
  precision loss when an object array or an untracked array is indexed symbolically (both only
  track the concrete element).
