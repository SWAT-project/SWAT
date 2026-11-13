# SWAT

Dynamic symbolic execution for Java:

- `symbolic-executor/` is the Java agent that instruments bytecode and records execution traces.
- `symbolic-explorer/` is the Python driver that builds the execution tree, picks branches to
  flip and calls the solver.
- `targets/sv-comp/` holds the SV-COMP harness and benchmarks.

## Static pre-analysis (SA)

The explorer can prune its search with a control-flow graph from the extractor in
`../cfg-extraction`. That graph is loaded by `symbolic-explorer/data/StaticAnalysisGraph/SAGraph.py`
and walked in `strategy/DFS.py`; the module comments of both define the terms below.

**Terms** (used in the code, logs and stats):

- **Target point** (`SANode.isTarget`): where the property may be violated, i.e. an assertion point
  (valid-assert) or a site that may raise a RuntimeException (no-runtime-exception). Later targets
  include shell calls and SQL statement building, so prefer changes that work for any kind.
- **Interesting:** a target point is reachable, given the walk's call stack (`is_interesting`).
  `dfs` prunes the sides of branches that are not.
- **Pairing** (`DFS._pair`): how a tree branch was matched with a graph branch. **Verified** (key
  and call stack checked), **resynced** (found again by key), **unverified** (followed, but a key
  or a call site is missing to check it), **lost** (no graph node), **masked** (a branch the graph
  has no node for: the walk holds its place). Only verified and resynced pairings prune or slice.
- **Desync:** a key or call-stack mismatch: logged as `[DFS] SA desync`, recorded as `sa_desync`
  (turns SAFE into UNKNOWN), and the walk is lost from there.
- **Sliced / merged / decision** (`DFS._classify`): a sliced branch is irrelevant to the targets
  (relevance, below), a merged one was merged by a loop summary; neither is flipped nor keeps its
  constraint. **Poisoned:** a sliced branch that turned out relevant after all.

**The walk:**

- **Lockstep by position:** `dfs` walks the execution tree and the SA graph together, pairing
  branch for branch. If the two disagree on how many branches a path has, pruning uses the wrong
  nodes and can produce false SAFE verdicts. Any change to what the executor reports as a branch,
  including the transformers under `symbolic-executor/.../instrument/`, needs a matching change in
  the extractor. Concrete branches count too; filtering to symbolic branches happens after `dfs`.
- **Keys check the pairing:** every trace branch carries a key (`owner.name(desc)|line|kind|ordinal`,
  `instrument/BranchKeys.java`, named in `InstructionMethodAdapter.nameInstruction`) and the frame
  stack change since the previous branch (`ShadowContext.takeStackDelta`). `SAGraph.check_branch`
  compares them with the graph node's key and the walk's call stack; when the walk is lost,
  `SAGraph.resync` finds the branch by key, with the call stack rebuilt from the trace. The
  extractor computes the same keys; change both together.
- **Losing the walk is safe:** a lost or unverified walk prunes and slices nothing. The walk is
  lost at incomplete graph nodes, at call sites with several callees (exact dispatch to known
  overrides: it resyncs inside whichever callee ran; the summaries take the union), and after an
  exception the graph does not model (below).
- **Masked branches:** branches in `<clinit>` (not in the graph), in application code that library
  code calls back (flagged `callback` by the executor, `ShadowContext.inCallback`), in SWAT's own
  helpers (the reference-comparison rewrite `UtilInstrumented.refEquals`, flagged like callbacks,
  `ShadowContext.inSwatHelper`, since how many there are depends on runtime types), and choice
  branches (the executor's search for the element a symbolic index selects).
  `Verifier.nondetObject(type, factory)`, which the executor runs as `factory.createObject()`, is
  expanded and keyed as that call by the extractor.
- **Exceptions the graph does not model** (null dereferences, casts, library calls, Errors): the
  extractor gives them pseudo types (`?Exception`, `?Error`) whose escapes SAGraph treats as
  caught by any handler listed for them (`unknown_handlers`), so paths into such handlers stay
  interesting. When one actually happens, the executor records an `UNMODELLED_EXCEPTION` marker,
  which `Tree.structural` keeps as `Node.unmodelled_exception` on the next node; the walk is lost
  there until it resyncs.
- **Guards that cannot raise:** a phantom guard the extractor marks `cannotRaise` (the property
  pre-check proves its implicit exception cannot happen, cfg-extraction `RaiseSites`) gets a dead
  node as its exception side in `SAGraph.load_json_graph`, so that side is never interesting.
- **Observability:** each selection logs one line, e.g. `[DFS] Walk: 34 branches (33 verified,
  0 resynced, 0 unverified, 0 lost, 1 masked), 12 sides pruned; 32 sliced, 0 merged, 0 covered,
  0 poisoned` (`DFS.describe_walk`; "covered": candidates left out as covered by a path with the
  same relevant decisions), and `stats.json` has the same counts in `static_analysis.last_walk`.
  The per-branch trace of the walk is at level `DFS.TRACE`, below DEBUG.

**The graph and its targets:**

- **Targets per property** (`StaticPreAnalysis.SA_TARGETS`): `assertionPointIds` under
  valid-assert; under no-runtime-exception the sites that may raise a RuntimeException plus the
  `uncaught[T]` nodes of RuntimeException types (`runtimeExceptionPointIds`, from the property
  pre-check's value analysis, cfg-extraction `RaiseSites`). Incomplete nodes are target points too.
  - **No-runtime-exception is opt-in in the harness** (`svcomp test run --sa-no-runtime-exception`;
    the explorer prunes whenever it gets `--sa-path`). On the 736 tasks (300 s, 2026-10-24) it is
    sound (no false SAFE) and cuts queries on the solved SAFE tasks by a third, but solves nothing
    new: the pre-check already proves 426 tasks and the extractor costs 3 to 6 s per task.
- **Library callbacks that may reach an assert** make the extractor count every library call as
  an assertion point; `--sa-no-callback-targets` (and `svcomp test run --sa-no-callback-targets`)
  turns that off, which is unsound. The extractor gets the target's classpath without Z3's jar
  (`StaticPreAnalysis._is_swat_library`): its classes would count as application code.
- **Code outside main's call tree** (threads, shutdown hooks, executors: `SAGraph.async_entry_sites`)
  makes the graph unusable: it is rejected and the run explores unpruned.
- **SA runs before exploring by default:** in sv-comp mode exploration waits for
  `svcomp/StaticPreAnalysis.py`, so the graph is used from round 0 and runs are deterministic.
  `--parallel-sa` instead runs the extractor in a thread while exploration starts unpruned;
  `SVCompDriver.adopt_sa_graph` switches to the graph once it is loaded, and it is cancelled
  (extractor killed) if exploration finishes first. That hides SA bugs whenever the graph loads
  too late, and makes results depend on load. In parallel runs the `static_pre_analysis` timing
  stage is 0 and its duration is `static_pre_analysis_wall`; `run_info.json` in the run dir records
  the SA mode.

**Relevance (slicing)** (needs cfg-extraction `v0.2.0` or later for the data-flow facts): a branch
whose region (up to its immediate post-dominator) cannot affect whether a target is reached, nor
any value a target or a relevant branch reads, is irrelevant. `dfs` slices it: it is not flipped,
`collect_path_constrains` leaves its constraint out, and candidates are identified by the sequence
of relevant decisions before them (their sig), so paths that differ only in irrelevant decisions
count as one. Computed in `data/StaticAnalysisGraph/Relevance.py` from the extractor's per-node
data-flow facts; off with `--no-sa-slicing` (explorer and harness). Results and where it helps:
`swat-benchmarks/RESULTS.md`, section "Relevance (slicing)".

- **Poisoning:** each path carries the regions of its sliced branches; a branch inside one that
  the graph cannot account for (masked, unverified, lost, or relevant) or a run that ends there
  with an uncaught exception (`Leaf.abnormal`, ancestors `abnormal_below`) poisons those branches
  (`Node.poisoned`: relevant for good), and `select_branch` walks again.
- **Statements that may throw** to a local handler or a harmful escape are implicit branches in
  the analysis: what they read becomes relevant with them (else a null receiver's origin gets
  sliced: four sa-soundness false SAFEs before this).
- **Queries:** a sliced query's full query is solved first; the sliced one only decides where the
  full one is UNSAT. A run of a sliced solution may reach its branch on another path with the
  same relevant decisions: `check_projected_divergence` checks that after the next walk.
- **Known limitations** (fixes sketched in RESULTS.md, relevance "Open"): all library state is
  one location `g:lib` (fix: type families, then allocation sites with an escape check), and the
  slice is context-insensitive (fix: a "return value relevant / not" variant per method, chosen
  per call site and looked up by the walk's stack).

**Loop facts** (since 2026-11-04): *where* the loop heads are, which locals they hold, and which
diamonds and counting loops can be summarized is static analysis and lives in cfg-extraction
(`de.uzl.its.cfg.loops`: `LoopHeads`, `LoopSummaries`, `BodyAnalysis`, `TaintCheck`). The extractor
writes the facts of every class on the classpath to `loop_facts.json` in the log directory, first
thing; the driver passes it as `-DloopFacts`. When `InstructionClassAdapter` instruments a method,
`instrument/loopfacts/LoopFactsSource` takes its facts from the file if the method's code hash
matches, else computes them with the same library, which the executor bundles (`cfgLoopsJar`
builds `cfg-extractor-loops.jar` from `../cfg-extraction`, `-PcfgExtractionDir`, or a clone of
`cfg-extraction.ref`; the shadow jar relocates it). What stays in the executor is what runs: the
probes (`LoopHeadProbes`, `SummaryProbes`), `Merger` and `LoopStateMatcher`. In the file, jumps are
named by ordinal and labels by the number of real instructions before them (`Positions`).

- **The facts are in the prepared classes themselves** (since 2026-11-11): the driver passes the
  prepared directories as `--annotate-loop-facts`, the extractor annotates each method with its facts
  (cfg-extraction `LoopFactsAnnotations`), and `LoopFactsSource` takes a method's facts from its
  annotation, else from `-DloopFacts`, else computes them, always only for a matching hash. The
  executor logs the split at exit (`[LOOP FACTS] n from annotations, ...`, with the debug log on).
  This is the carrier for later SA facts (section 5 of `/home/niklas/SWAT/BRIEF-sa-annotations.md`).
- **The facts are computed on prepared classes** (class preparation, below): the extractor sees
  the code after SWAT's rewrites, as the executor does, so the facts file applies to every method
  with facts (7753 of 7753 over the task classpaths of sv-benchmarks and swat-benchmarks,
  2026-11-11; on the classes on disk it was 2697: the rewrites renumber locals and insert code).
  Without preparation (`--no-prepare-classes`, or when it fails) the executor computes the facts of
  rewritten methods itself, so results are the same either way; facts that only a whole-program
  analysis (or a person) can provide reach every method only through preparation.
- **Tests:** `./gradlew test --tests 'de.uzl.its.cfg.loops.*'` in cfg-extraction (`LoopFactsSpec`:
  the round trip, and that the facts do not depend on how the class was read).

**Class preparation** (since 2026-11-11, `svcomp/ClassPreparation.py`, executor `prepare/Prepare` and
`instrument/rewrite/`): SWAT's semantic rewrites (no-cache, reference equality, SV-COMP; the
transformers that change *what* runs, before the instrumentation) are applied once, ahead of time,
so that the static analysis and the executor see the same code.

- **One implementation, two places:** `Rewrites.steps()` is the chain (same order as the agent
  always ran it). `Prepare` runs it over the task's classpath (about 0.2 s): each directory entry
  `i` is copied to `<logdir>/prepared/<i>`, with every class the chain changes replaced by its
  rewritten version; jars stay as they are. At load time, `RewriteTransformer` runs the same chain
  on every class that is not prepared (jars, classes generated at run time, other classpaths).
- **Marker:** a prepared class carries an invisible class annotation `Lde/uzl/its/swat/Prepared;`
  with `version` (`Rewrites.FORMAT_VERSION`) and `config` (`Rewrites.configHash()`: transformer
  type, include/exclude packages, `useStringInterning`). The agent passes a matching class on
  without rewriting; a mismatching one is an error (a class must never be rewritten twice). Bump
  `FORMAT_VERSION` when a rewrite changes.
- **The invariant:** a prepared class is byte for byte what the agent's load-time chain makes of
  the original. `scripts/check-prepare.py <bench dirs>` checks it per task classpath (the agent
  dumps its output with `-Dswat.dumpRewritten`, `prepare/LoadClasses` loads every class), and that
  the agent rewrites nothing on a prepared classpath. So the rewrites must be deterministic: nondet
  call ids are derived from the call site (`NondetIds`: class, method, descriptor, ordinal), not
  from a load-order counter.
- **Who gets what:** the extractor and the executor get the prepared classpath; the property
  pre-check keeps the original classes (it models the SV-COMP `Verifier` and a JDK whitelist, and
  its proof is about the original program). Witnesses use the original source directories.
- **Fallback:** if preparation fails or times out, the run uses the original classpath, as
  before; `stats.json` has `class_preparation` (status, counts, duration). Off with
  `--no-prepare-classes` (explorer and harness).

**Hand-written facts** (user facts, since 2026-11-12; `annotations/` module, executor `facts/`):
facts a person (or an LLM) writes into the program's source, for real applications whose loops
and libraries the SA cannot handle. Off unless asked for: executor `-DuserFacts=true`, explorer and
harness `--user-facts`; a swat-benchmarks task asks for it with `options: swat-user-facts: true` in
its `.yml` (harness `--no-user-facts` overrides). Never for SV-COMP, which measures SWAT itself.

- **The facts** (package `de.uzl.its.swat.annotations`):
  - `@Irrelevant` on a class, method or constructor, and `Swat.beginIrrelevant()` /
    `Swat.endIrrelevant()` around statements or a loop (the code between the two calls in bytecode
    order): their branches, and those of everything they call, cannot affect a target. The
    instrumentation registers the covered instructions, branches and call sites (`UserFacts.register`,
    by iid like the branch keys). At run time a frame entered from a covered call site carries the
    fact (`Frame.userFact`, set in `ShadowContext.pushFrame`), so a branch is covered if its own
    instruction is or any frame below it is (`ShadowContext.irrelevantFact`): callees,
    constructors, virtual calls and recursion, but only below the irrelevant code. The trace flags
    covered branches `merged` with `userFact`, and `dfs` treats them as no decision. Library code
    calling back into the application is not tracked symbolically at all (context loss), so facts
    do not help there.
  - `if (Swat.summary()) { closed form } else { loop }`: a loop summary in plain Java (javac picks
    the locals; any Java expression works). `Swat.summary()` returns false; with user facts the
    rewrite `UserFactsTransformer` turns it into `UserFacts.summary("<id>")`, which returns true.
  - `@Target` on a method: reaching it is a violation. `UserFactsTransformer` starts the method with
    `UserFacts.target("@Target reached: ...")`, which throws an AssertionError (a violation for the
    executor); the method keeps its code.
  - `@NotCalledBack` on a method (2026-11-13): library code never calls it, although it overrides a
    library method or is a lambda target (see "Callbacks" below).
  - Library facts (2026-11-13): a stub file `swat-library-facts.txt` at the root of a classpath
    directory, one library method per line (`java.util.HashMap.get(java.lang.Object) reads=HashMap`;
    `reads=`/`writes=` families of library state, `nocallback`, `pure`, `arrays`; format in
    cfg-extraction's `facts/LibraryFacts`).
- **Where:** `UserFactsTransformer` is the first of SWAT's rewrites (`Rewrites.steps`), so prepared
  classes have it; the config hash covers `userFacts`. The extractor reads the facts itself
  (`--user-facts`, cfg-extraction `facts/UserFacts`, also on unprepared classes), and so does the
  property pre-check for `@Target` (a reachable `@Target` method is not assertion-free). So both
  run on the original classes too, and a failed preparation no longer switches anything off.
- **What the graph does with them** (since 2026-11-13; cfg-extraction "Hand-written facts"):
  `@Target`: the method's entry is an assertion point. `@Irrelevant` and regions: their nodes carry
  `userFact` (a region line that also holds code outside the region only `userFactNear`); the
  relevance analysis (`Relevance._user_facts`) refutes a fact whose nodes (near ones included)
  contain a target point, may end the program or call code that may reach a target, and `dfs`
  then ignores the executor's flag for it; otherwise it computes relevance a second time with the
  fact's own branches taken as irrelevant (`SANode.irrelevant_assuming`; never its callees', which
  may also run from a callback, a lambda or a static initializer), which slices branches outside
  the fact that only feed it. `Swat.summary()`: the branch's loop side is a
  dead end (`deadSide`). Library facts: a call with facts reads and writes only `g:lib:<family>`,
  every other library call all of `g:lib` and every family (`DataflowFacts`); `nocallback` and
  `@NotCalledBack` relax the callback rule (`CallbackTargets`: a library call that cannot call back
  is no assertion point, a method that is not called back is no callback).
- **Trust and checks:** a wrong `@Irrelevant`, summary or library fact can hide a violation. `dfs`
  records a fact as relied on (`Tree.relied_facts`) only where the relevance analysis did not slice
  the branch by itself, and for a branch sliced only assuming the facts; the graph's own facts
  (library facts used, `@NotCalledBack`) always count once the graph is used. The explorer logs
  `[USER FACTS] SAFE assuming n user fact(s): ...` and records them in `stats.json` (`user_facts`,
  with the irrelevance facts checked and refuted). `@NotCalledBack` is also checked at run time: a
  callback into such a method refutes it (`UserFacts.calledBack`, reported as `REFUTED ...` among the
  trace's facts), and the verdict cannot be SAFE.
- **Results:** swat-benchmarks `user-facts` and `extractor-facts` (RESULTS.md, "Hand-written
  facts", "Hand-written facts in the extractor"); agent test `UserFactsAgentSpec`; explorer
  `tests.test_relevance` (`UserFactsTest`).

**Loop summaries** (applied by executor `symbolic/summary`): diamonds (an int `if` whose sides are
straight-line int arithmetic on locals, also nested, also `/`/`%` by constants) and counting loops
(`i < b` or `i <= b`, step one) are merged into formulas; their branches are flagged `merged` and
`dfs` treats them as no decision, like sliced ones. A counting loop whose body does other things
too (calls, heap) is flagged `mergedConditional`, honored only if `Relevance.summary_ok` (the loop
changes nothing relevant but its locals), and then it opens a region like a sliced branch; its
branches are flagged only when it leaves through its head (`BackEdge` plans tell iterations from
re-entries). `TaintCheck` drops a summary whose merged values could reach a call, a heap or array
store, an array index or size, or a return, since a dropped constraint must not be needed where the
executor concretizes (13 false SAFE before, via `Verifier.assume`). Ignoring a `merged` flag is
always sound. Config `loopSummaries` (on in `sv-comp.cfg`), `--no-loop-summaries`. Results, limits,
next steps: swat-benchmarks/RESULTS.md, "Loop summarization". The analysis' tests are in
cfg-extraction (see loop facts above).

**Other exploration features:**

- **Small models**: a solution with a numeric input (integral or floating-point, also infinite)
  beyond 65536 is re-solved with the inputs bounded first (`SVCompDriver.prefer_small`,
  `--no-prefer-small-models`); a loop bound of 2^30 otherwise makes the next run never end (+4
  valid-assert, +1 no-runtime-exception), and an approximate Math model is only right near its
  samples. Bounded queries get at most twice the units of the branch's unbounded query, at least
  10M (`BOUNDED_RLIMIT`): they can be far harder than the unbounded one.
- **Approximate models** (`SampledMath`, executor): Math functions are exact at sample inputs
  (nice values, and since 2026-11-03 the edges of the double and integral ranges, so overflows to
  infinity and NaN are reachable) and the run's own result elsewhere; floating-point `%`
  (`JavaRemainder`) is exact where the quotient is at most 1 in magnitude. Both are declared
  approximate, so a branch over them is a precision loss and rules out SAFE. Before 2026-11-03
  `%` had no formula at all and silently gave false SAFE (PieSegment).
- **Loop-head state matching** (`loopStateMatching`, executor `LoopStateMatcher`, heads from the
  loop facts; explorer
  `--no-loop-state-matching`): a branch below a state already reached at a smaller position is not
  flipped. It is what makes nondet-guarded loops with a repeating concrete state SAFE (28 tasks of
  sv-benchmarks valid-assert, 27 of them in `float_unboundedloop`, +56 points, with dfs and
  coverage alike; 2 in swat-benchmarks `dispatcher`). A run stops reporting states after any call
  into untracked code other than a whitelist (`isPureCall`): from 2026-10-19 to 2026-11-03
  `Class.desiredAssertionStatus`, which every class with an `assert` calls, was missing, and the
  feature did nothing in any valid-assert task.
- **Only live locals are hashed** (since 2026-11-11, cfg-extraction `LoopHeads`, loop facts
  version 2): a local dead at the head (written again before any read, exception edges included)
  is neither hashed nor required to be concrete. It changed no verdict on sv-benchmarks or
  swat-benchmarks valid-assert (`run_20261111_193209`, `_194711`); swat-benchmarks `loop-liveness`
  shows where it does (a symbolic input in a local that is dead at the head) and has the
  soundness pairs (RESULTS.md, "Loop-state matching with live locals only").
- **Heads in callees** (since 2026-11-13, loop facts version 3): the head's method no longer has to
  be the only application frame. cfg-extraction's `LoopHeads.callSites` lists the call sites whose
  operand stack holds only the call's arguments, with the locals live after the call; a `LOOP_CALL`
  probe before each (to application code) records those locals on the caller's shadow frame
  (`Frame.loopCall`, with the iid of the call's INVOKE). At a head in a callee every frame below
  must have such a record for the call that pushed the frame above it, the application frames must
  be contiguous (no library frame between them: a callback) and the shadow stack must hold exactly
  them; the hash adds each caller's call site and live locals (only those must be concrete). A head
  in the entry method hashes as before. Lambdas still stop all states (`invokedynamic`), threads
  are still excluded. swat-benchmarks `loop-frames` (RESULTS.md, "Loop-state matching across
  frames").
- **Hangs**: the executor's `DeadlockWatchdog` (config `deadlockWatchdog`) ends a run whose
  application threads all wait forever, and `ResourceBudget` has a wall-clock deadline (default:
  the CPU limit), so a hang ends UNKNOWN instead of being killed by the harness.

Tests for the walk, runnable without a SWAT run:

```bash
cd symbolic-explorer && python3 -m unittest tests.test_sa_walk -v
python3 tests/check_marking_on_graphs.py <graph.json>...   # checks marking against an explicit search
../targets/sv-comp/scripts/.venv/bin/python3 -m unittest tests.test_divergence -v   # needs the venv
../targets/sv-comp/scripts/.venv/bin/python3 -m unittest tests.test_relevance -v    # relevance and slicing
../targets/sv-comp/scripts/.venv/bin/python3 -m unittest tests.test_prefer_small -v # small models
```

The SA modules are fully annotated and pass mypy (not in the venvs; install it into a scratch
venv), run from `symbolic-explorer/`:

```bash
mypy --explicit-package-bases --ignore-missing-imports --follow-imports=silent --check-untyped-defs \
     --disallow-untyped-defs --disallow-any-generics strategy/DFS.py strategy/StrategyService.py \
     data/StaticAnalysisGraph/*.py data/BinaryExecutionTree/*.py svcomp/StaticPreAnalysis.py
```

`dfs` only descends into real `data.BinaryExecutionTree.Node` instances. In tests, build them with
`Node.__new__(Node)` and set the fields directly (the per-node flags have class defaults). `pytest`
is not installed; use `unittest`.

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
  check is skipped (`missing_jar`). CI builds it at the cfg-extraction tag in `cfg-extraction.ref` (cfg-extraction is public, MIT)
  and the archive ships it as `cfg-extraction/cfg-extractor-precheck.jar`. Its soundness gate is
  `cfg-extraction/scripts/precheck_gate.py` (0 contradictions over sv-benchmarks).
- **SV-COMP archive** (`scripts/package-svcomp.sh`, run by CI; `run-swat.sh` → `run_swat.py`):
  ships the extractor's fat jar as `cfg-extraction/cfg-extractor.jar`, built by CI at the same
  ref as the pre-check, and `run_swat.py` passes it as `--sa-jar` (instead of `--sa-path`, a
  checkout) under valid-assert only, like the harness. Without `--target` the graph starts at
  `Main.main`, the class the executor runs. `scripts/check-svcomp-package.sh` fails unless the
  smoke test's `stats.json` has `static_analysis.status` `loaded`. Every jar in the archive
  carries `META-INF/swat-third-party.json` (the fat jar's from cfg-extraction's
  `fatJarComponentList`), which `third_party_licenses.py` needs for each bundled component.
- **Search order:** `--search-order` picks which open branch is flipped next (`StrategyService`):
  `coverage` (default since 2026-11-03: sides no trace took yet, then the shallowest), `dfs`,
  `shallow`, `coverage-dfs`, `generational`. The harness always passes it explicitly. Plain DFS
  keeps unrolling input-parsing loops one char per round with ever larger queries. The order changes
  only how fast violations are found, not SAFE. Coverage against dfs (300 s, 32 workers, with SA,
  2026-11-03, runs `run_20261103_103945` to `_115003`): swat-benchmarks 694 vs 688 (+6 violations,
  none lost), sv-benchmarks valid-assert 1034 vs 1031 (+4 that dfs never found in five runs, -1 a
  flaky one), no-runtime-exception the same but for `WeibullGenerator_exceptionless`, a real bug dfs
  finds and coverage did not in 300 s (label fixed below). SAFE verdicts were identical; violations
  both find took fewer rounds with coverage. Without SA (978 valid-assert tasks, 300 s), coverage
  found 6 more violations and lost none.
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
  - `argv-tasks/WeibullGenerator_exceptionless`: no-runtime-exception is false (a tiny negative
    `beta` makes `1 / beta` -Infinity, `(int)` of it Integer.MIN_VALUE and `Math.pow` +Infinity,
    which times `lamda = 0` is NaN and throws, e.g. beta's nondet = -2.0722615e-317); not reported
    upstream yet.
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
