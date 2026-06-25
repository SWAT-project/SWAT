# Shadow Heap Redesign — Implementation Handoff

You are implementing the shadow-heap redesign (G1, G2, G_oob, G3, G4). The **tests are already
written** and act as your acceptance criteria: a small set is intentionally *red* (failing) and
encodes the behavior you must produce. This doc tells you what exists, what to make pass, where the
bugs live, and the invariants you must not break.

Branch: **`fix/heap-design`** (off `dev`). Two commits: `a7557fa` (harness foundation),
`b19df47` (case-matrix fan-out).

Read first: `docs/heap-redesign-plan.md` (the goals G1–G4 + phase order),
`docs/heap-redesign-tests.md` (behavioral cases), `docs/test-architecture.md` (the test levels).

## The test contract (how to work)

- Tests live in `symbolic-executor/src/test/groovy/de/uzl/its/swat/symbolic/heap/` (L0/L1) and
  `.../testsupport/agent/` + `.../heap/*AgentSpec` (L2).
- **Red cases are marked `@PendingFeature(reason=…)`.** They run and assert the *desired* behavior;
  while failing they report as *skipped* (the suite stays green). **When your change makes one
  pass, `@PendingFeature` turns it into a BUILD FAILURE** that says "remove the annotation" — that
  is your signal the fix landed. Then delete the `@PendingFeature` line and commit.
- **Green tests are regression guards — they must stay green.**
- Current status: **33 tests, 29 passed, 4 pending, 0 failed.**

Run:
```bash
# L0 + L1 (fast, in-process). Scope to these packages — the legacy de/uzl/its/value/** suite
# (~1670 rows) fails for unrelated reasons; do NOT run the whole `test` task.
./gradlew :symbolic-executor:test --tests "de.uzl.its.swat.symbolic.heap.*" \
                                  --tests "de.uzl.its.swat.symbolic.processor.*"
# L2 (forked real agent; builds the jar first)
./gradlew :symbolic-executor:agentTest
```
Build the agent jar with `:symbolic-executor:copyJar`. **Never run `spotlessApply`/`spotlessCheck`**
— it reformats the whole module and buries your diff; CI does not gate on it.

## What to implement, mapped to the red tests

### Phase 1 — G1: canonical registry + faithful key + register-on-create
Makes these flip green:
- **O-4** (`ObjectIdentitySpec`, L0): two shadow wrappers for one identity must compare
  reference-equal. Today `ObjectValue.IF_ACMPEQ` is `bfm.makeBoolean(this == o2)` on the *wrapper*
  (`ObjectValue.java:152`), so duplicate wrappers compare unequal. Fix = one canonical wrapper per
  identity (or compare by identity/address), so `IF_ACMPEQ` is correct. **This is a correctness fix**
  (non-String `IF_ACMPEQ` drives real `==` semantics), not cleanup.
- **O-5 "heap stores colliding-hash objects without merging"** (`ObjectIdentitySpec`, L0):
  two distinct objects sharing an identity hash must not collapse. Today `JVMHeap` is
  `Map<Integer identityHashCode, Value>`, so a hash collision overwrites. Fix = a faithful,
  collision-free, reference-keyed registry (weak `IdentityHashMap` is the default choice).
  ⚠ **This test currently drives the legacy `ShadowContext.putToHeap(int)` API.** When you change
  the registration API to find-or-create by faithful key, **update O-5's `when:` block** to register
  via the new API — the *assertion* (`heapSize() == 2`) is the stable contract and must hold.

Also covered by G1 but **not yet tested** (write these as you build the re-entry path): O-1
(aliasing through fields), O-2 (re-entry recovers the same object), O-3 (object born in unmodeled
code), O-6 (identity reuse after death/eviction). The L1 boundary fixture (below) is the starting
point for O-2/O-3.

### Phase 2 — G2: value-type boundary recovery (collapse policy v1)
Makes these flip green:
- **V-1** (`ValueRecoverySpec`, L1): a symbolic, already-lowercase String receiver of an unmodeled
  `toLowerCase()` returns `this`; the recovered result must **not** carry the receiver's symbolic
  formula (assert: recovered vars disjoint from receiver vars), must be concrete, and context loss
  must be flagged (the flag already fires today). Root cause: `SymbolicInstructionVisitor`
  `visitGETVALUE_Object` (~`:1265`) reconciles the `PlaceHolder` result by
  `getFromHeap(inst.address)` and pushes the receiver's `StringValue` back — identity-recovery of a
  value type. Fix (per plan §G2 v1 "collapse"): at a **mirrored invoke boundary of an unmodeled
  value-returning method**, do NOT identity-recover for value types (`Util.deInternedClasses` =
  String + 6 boxed wrappers); produce the concretized value + context-loss flag.
- **V-1 (L2)** (`HeapRecoveryV1AgentSpec`): the same scenario run through the REAL agent
  (`ToLowerCaseTarget`); assert the branch on the result does not reference the symbolic input
  variable. This is the faithful end-to-end check — it must flip together with the L1 V-1.

Already green and must stay green: **V-2** (new-object transform doesn't alias — same fixture, fresh
result address), **V-5** (`new String(String)` copy ctor keeps φ — keep `StringValue.invokeInit`
copying the formula, `StringValue.java:217`), **V-6** (interned-literal reuse), **V-9** (concrete
grounding), **F-1/F-2** (context-loss flag fires iff symbolic data flows in).

Not yet tested (Phase-2+): V-3 (single-φ round-trip — needs a field round-trip fixture, distinct
from the invoke fixture), V-4 (conflicting-φ set → realize but recover concrete), V-7 (boxed
analogue), V-8 (primitive context loss — needs a primitive-return fixture).

### Later phases (no tests yet — add them when the mechanism exists)
G_oob out-of-band change detection (E-1, needs an L2 mutating target); G4a purity whitelist
(E-2, F-4); G3 output de-interning (D-1/D-2/D-3); G4b UF + soundness (U-1…U-5).

## Invariants you must preserve (the harness depends on them)

1. **Heap inspection view.** `ShadowContext.heapSize()/heapLookup(int)/heapEntries()` and
   `JVMHeap.size()/values()` are the read-only seam the O-tests assert against. Your new
   registry **must keep implementing these** (same semantics: `heapEntries()` = one entry per
   canonical identity; `heapSize()` counts distinct identities). Tests flip red→green without
   changing their assertions.
2. **Soundness flags + getters.** `SymbolicTraceHandler.isSymbolicContextLoss()` /
   `isReferenceSemanticChange()` and the `TraceDTO` booleans (`symbolicContextLoss`,
   `symbolicPrecisionLoss`, `referenceSemanticChange`) are read by L1 and L2 tests. Keep them.
3. **L1 fixture.** `BaseSymbolicInstructionProcessorSpec.executeBoundaryRecovery(receiver, owner,
   name, desc, concreteResult, resultAddress)` drives INVOKEVIRTUAL→INVOKEMETHOD_END→GETVALUE_Object.
   `resultAddress == receiver.address` = this-return; fresh address = new object. If you change the
   recovery API/addresses, keep this fixture working (or update it in lockstep).
4. **L2 channel.** `solver.mode=PRINT` prints the `TraceDTO` JSON to stdout (`Intrinsics.terminate()`
   PRINT branch). `AgentRun` parses it. Don't break PRINT mode.

## Key code locations (audit-confirmed; verify line numbers, they drift)

- Recovery cache: `symbolic/shadow/JVMHeap.java`, `symbolic/shadow/ShadowContext.java`
  (`putToHeap`/`getFromHeap`).
- The V-1 recovery path: `symbolic/SymbolicInstructionVisitor.java` `visitGETVALUE_Object`
  (~`:1242`–`1340`); the heap miss-create at ~`:1289`.
- The `==` bug: `symbolic/value/reference/ObjectValue.java` `IF_ACMPEQ` (`:152`), `equals`
  (`:126` — note it NPEs when `fields == null`; tighten if you touch it).
- Unmodeled stub: `symbolic/value/reference/lang/StringValue.java` `invokeToLowerCase` (`:1125`,
  returns `PlaceHolder.instance`); copy ctor `invokeInit` (`:217`).
- Flag decision: `symbolic/invoke/InvocationHandler.java` `invoke` (`:38`–`121`) — records context
  loss when result is a `PlaceHolder` and a symbolic arg/receiver is present.
- Run lifecycle / solver modes / PRINT: `instrument/Intrinsics.java` `terminate()`;
  `config/Config.java` `solver.mode` (default LOCAL).

## Gotchas

- The **legacy `de/uzl/its/value/**` suite (~1670 tests) fails** for unrelated API-migration reasons
  — scope your runs to the `heap`/`processor` packages.
- `@Symbolic` on a **local variable** crashes the annotation transformer
  (`AnnotationTransformer.transform`, `NoThreadContextException`) without `-g`; L2 targets use it on
  a **parameter**. (Candidate hardening if you touch the transformer — not required.)
- We deliberately did **not** merge `feat/svcomp-testcase-metadata` (stats.json) — it's an L3
  enabler; L2 reads the `TraceDTO` directly.
- `@PendingFeature` masks *any* failure as pending; that's why each red spec asserts currently-true
  preconditions first (or as a separate non-pending feature) so an infra break is a real failure.
  Keep that convention when you add red tests.

## Suggested order

1. **G1** → make O-4 + O-5 green (canonical registry, faithful key, fix `IF_ACMPEQ`, update O-5's
   registration call). Watch the two O reds flip; remove their `@PendingFeature`.
2. **G2** → make V-1 (L1) + V-1 (L2) green (collapse policy at the value-type invoke boundary).
   Remove their `@PendingFeature`. Keep V-2/V-5/V-6/V-9/F-1/F-2 green.
3. Build the re-entry fixture and add O-1/O-2/O-3/O-6; then proceed through G_oob/G4a/G3/G4b,
   adding the corresponding tests (E/F/D/U) as each mechanism lands.

The `swat-test` skill (`.claude/skills/swat-test/SKILL.md`) has the per-level recipe for adding the
remaining tests.
