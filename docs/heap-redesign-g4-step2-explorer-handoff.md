# G4 step 2 — explorer hand-off: cross-run UF observed-pair accumulation

**Status:** the EXECUTOR side is implemented + committed. This document specifies the remaining
**explorer** work, deliberately deferred to coordinate with the upcoming explorer rework (static CFG
+ explorer-side decisions), since it touches the solve path and verdict logic.

## What the executor already does (done)

For a whitelisted pure unmodeled value-returning call (G4 step 1, e.g. `String.trim`/`strip`), the
executor models the result as a generic axiom-free UF `pure_<Class>_<method>(inputs)`. Step 2 adds:
each run, at the GETVALUE recovery, the executor emits the **observed ground pair** for that call as a
constraint:

```
pure_<sig>(<constant observed inputs>) == <constant observed output>
e.g.   (= (pure_String_trim "abc ") "abc")
```

built with the SAME cached UF declaration as the symbolic result UF (so it constrains the same
symbol). It is emitted via `symbolicTraceHandler.addConstraint(...)`, so it travels to the explorer on
the existing UF channel: `SymbolicTrace.getConstraints()` → `DTOBuilder` `UFDTO` (DTOBuilder.java:78-81)
→ explorer `Parser.parse_ufs` → `Database.add_trace` → `tree.add(...)` (onto `Node.ufs`) **and**
`tree.record_ufs(...)` (into the per-testcase `Tree.ufs` set). Verified by `PureFunctionUFSpec` U-7
(the pair is emitted, and is ground — over the constant input, no free variable).

Emitting one pair per run is **sound on its own**: a single observed pair is a true fact about the
real function and cannot self-contradict, so nothing here can cause a wrong verdict today.

## Why the explorer work is needed (the gap)

Accumulation across runs does **not** currently reach the solver:
- **`Tree.ufs` is dead code** — written by `Tree.record_ufs` (Tree.py:60), read nowhere.
- The live UF injection (`StrategyService.collect_uf_definitions`, StrategyService.py:78-82 →
  `Z3Handler.solve`, SolverHandler.py:407-409) reads **`Node.ufs`**, which is frozen in the `Node`
  constructor (Node.py:51) and never merged across runs (`Tree.add_recursive` updates only
  `node.constraint`, Tree.py:116-117). So a run's pair is injected only onto branches that run first
  created; flipping a pre-existing branch never sees the accumulated table.

So without the work below, step 2 is (almost) inert: the per-testcase lookup table accumulates in a
dead set and never tightens later solves.

## Required explorer changes (land these together — see the coupling note)

### 1. Inject the accumulated per-testcase UF set at solve time
Thread the accumulated set through the existing chokepoint. `collect_uf_definitions(node)` cannot reach
`Tree.ufs` (a `Node` has no tree back-reference; `solve_branch(..., endpoint_id=None)` is called with
no endpoint by 4/5 drivers: SVCompDriver.py:261, TargetDriver.py:154, SimpleDriver.py:160,
HTTPDriver.py:681, SVCompHandler.py:195 — only PassiveDriver passes it). So:
- In `StrategyService.select_branch` (which already holds the post-`add_trace` deepcopy `tree`,
  StrategyService.py:16-23), pass `tree.ufs` into `solve_branch` → `collect_uf_definitions`, and union
  those definition strings into `path_constraints` alongside the existing `node.ufs` walk.
- Do **NOT** add a `get_tree(endpoint_id)` inside `solve_branch` (endpoint_id is None on the main
  paths). Use the snapshot `select_branch` already has.
- **No staleness:** the per-round lifecycle is sequential — `Database.add_trace` calls `tree.add` then
  `tree.record_ufs` on the live tree before `retrieve_solution` → `select_branch` deepcopies, so the
  snapshot reflects all prior runs.
- Format is identical (both `Node.ufs` defs and `Tree.ufs` strings are the same `fmgr.dumpFormula`
  output), and re-asserting the same fact is idempotent in Z3 — double-adds are harmless. Once the
  union lands, the per-node `node.ufs` walk is a redundant subset and may be dropped.

### 2. Contradiction guard at accumulation (structural, not string-set identity)
`Tree.ufs` is a `Set` keyed by full-string identity — it cannot tell "same UF application, different
RHS" from two unrelated facts. Add a guard in `Tree.record_ufs` (Tree.py:52-60), narrowed to
`pure_`-named applications: parse each observed pair (via the existing `ConstraintManager`
parse machinery), key on `(uf-name, args) → rhs`; on a key collision with a **different** rhs, drop
the new pair (do not store both). A contradiction can only arise from a nondeterministic whitelist
entry; the guard prevents it from poisoning the accumulated set.

### 3. Solve-time UNSAT backstop (verdict soundness — defense in depth)
Before any UNSAT→SAFE conclusion (`SVCompDriver.py:271-273` sets SAFE when no branch is SAT), if the
accumulated `pure_` UF facts ALONE are UNSAT, downgrade SAFE→UNKNOWN instead. This converts the
worst case (a bad/nondeterministic whitelist entry making the injected set self-contradictory →
**false SAFE** for the whole testcase) into a conservative UNKNOWN. ~3 lines, gated on `pure_` ufs
being present so it costs nothing on non-G4 testcases.

### 4. L3 test (the L2 anchor does NOT cover this)
Drive the explorer directly (no JVM): `Database.instance().reset()` + `clear_constraint_cache()`,
then `add_trace` run A (a branch on `pure_String_trim(s)`, no pair) → the flip is SAT; `add_trace`
run B (same branch + an observed pair that forbids the value the flip needs) → assert
`select_branch`/`solve_branch` on that branch flips SAT→UNSAT. This proves an accumulated pair from an
earlier run constrains a later flip — i.e. injection is live, not inert.

## Coupling note (important)

**Items 1, 2, 3 must land together.** The executor emission and item 1 (injection) are what make the
accumulated facts reach the solver; once they do, a nondeterministic whitelist entry can make the set
UNSAT → false SAFE (`SVCompDriver.py:271-273`). So injection MUST NOT ship without the contradiction
guard (2) and the UNSAT backstop (3). Until this lands, the executor's emitted pairs sit in
`Tree.ufs`/`Node.ufs` and (apart from the existing within-run `Node.ufs` injection, which is benign —
a single pair can't contradict) do not affect verdicts.

This also raises the bar on whitelist curation: with cross-run injection live, a wrongly-whitelisted
non-deterministic/side-effecting method becomes a *soundness* risk (potential false SAFE), not just an
imprecision. Keep `PureMethods` strictly pure + deterministic.

## Key file references
Executor (done): `symbolic-executor/.../invoke/InvocationHandler.java` (buildPureUF, PureUFModel),
`.../value/PlaceHolder.java` (observedApplication), `.../SymbolicInstructionVisitor.java`
(visitGETVALUE_Object emit), `.../UFs/PureFunctionUF.java`, `.../trace/SymbolicTraceHandler.java:93`
(addConstraint), `.../trace/DTOBuilder.java:78-81` (UFDTO). Test: `PureFunctionUFSpec` U-7.
Explorer (to do): `symbolic-explorer/strategy/StrategyService.py:16-23,78-96`,
`data/BinaryExecutionTree/Tree.py:39,52-60,103-130`, `Node.py:51`, `solver/SolverHandler.py:391-393,
407-409`, `driver/SVCompDriver.py:261,271-273`, `solver/ConstraintCache.py`.
