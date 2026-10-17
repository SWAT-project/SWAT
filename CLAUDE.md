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
- **Concrete branches count:** they are in the tree too. Filtering to symbolic branches happens
  after `dfs`.
- **Walker giving up is safe:** when `walk_till_branch` returns `None`, the walker has no
  information, and `dfs` treats everything as interesting. That fallback is always safe.
- **`<clinit>` and callbacks are masked:** static initializers are not in the graph, and neither is
  application code that library code calls back (the executor flags those branches `callback`,
  see `ShadowContext.inCallback`), so the walk is frozen inside them.
- **Asserts are only the first target:** pruning aims at assertion points for now because they
  are easy to test (SV-COMP's valid-assert). Later targets include shell calls, SQL statement
  building and uncaught exceptions, so prefer changes that work for any kind of target point.
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
./scripts/svcomp test run --help      # --suite, --limit-nr-tests, --no-sa, --parallel-sa, --sa-rank-distance, --solver-rlimit, --testcase-timeout-s ...
```

- **Duration:** a full valid-assert run takes about 40 min with the default 900 s timeout, and
  much longer with `--testcase-timeout-s 3600`. Only compare runs made with the same timeout.
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
