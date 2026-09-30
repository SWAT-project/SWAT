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
  branch **by position**. Tree ids (executor iids) and graph node ids are never compared, and there
  is no resynchronisation. If the two disagree on how many branches a path has, pruning silently
  uses the wrong nodes and can produce false SAFE verdicts. Any change to what the executor
  reports as a branch, including the transformers under `symbolic-executor/.../instrument/`, needs
  a matching change in the extractor.
- **Concrete branches count:** they are in the tree too. Filtering to symbolic branches happens
  after `dfs`.
- **Walker giving up is safe:** when `walk_till_branch` returns `None`, the walker has no
  information, and `dfs` treats everything as interesting. That fallback is always safe.
- **`<clinit>` is masked:** static initializers are not in the graph, so the walk is frozen inside
  them.
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
- **Verdicts:** false SAFE (`violation -> safe`) is the outcome that matters most, and a pruning
  bug causes exactly that. Check any new one before trusting a run.
