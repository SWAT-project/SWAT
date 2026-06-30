# G3 — Output-boundary de-interning: Problem & Proposed Solution

Phase G3. Builds on G1 (`67505c4`) + G2 (`e84b46e`) + G_oob (`5b35699`) + G4 (`48763bc`/`6d39904`/`58c3932`).

## Implementation status

- **A1 (String): committed `994e642`.** Boundary de-intern of un-instrumented String returns + register-only-non-constant at the String recovery path. 2 design-audit rounds + 3 post-impl reviews.
- **A2 (boxed): implemented (this branch).** Extends de-interning to the six cached boxed wrappers in `Util.deInternedClasses` (Integer/Long/Short/Byte/Character/Boolean; **not** Float/Double — uncached, reference-equality). Two parts:
  - *Bytecode* (`NoCacheMethodAdapter.deInternReturn` + the `Boxed` table): boxed wrappers have no copy constructor, so the return is **unbox+rebox**ed (`new Integer(result.intValue())`, etc.), null-guarded. A local holds the unboxed primitive across the `NEW` — required for `long` (category-2, where `DUP_X1`/`SWAP` can't reorder a wide value) and uniform via `Type.getOpcode(ISTORE/ILOAD)`. Mirrors the existing `valueOf(primitive)` rewrites.
  - *Executor* (`visitGETVALUE_Object` non-String branch + `isConstantDeInternedValue`): register-only-non-constant, **scoped to `Util.isDeInternedClass(inst.val)`** so it touches only the de-interned boxed types. Mutable objects and Float/Double on this shared recovery path keep unconditional registration. A boxed wrapper carries its formula in the inner `BoxedValue.getVal()`, not the wrapper's own field — the predicate reads the right one.
  - *Tests*: `OutputDeInternSpec` (L1) — symbolic boxed → registered, constant boxed → not, **mutable object → always registered** (shared-path regression guard); `BoxedDeInternAgentSpec` (L2) — Integer (cat-1) + Long (cat-2 wide) returns load, verify, and run under the real agent (`AgentRun` asserts `exit==0`, so a malformed wrap would fail). 48 heap+processor + 5 L2 green.
  - *Note on the round-trip win*: boxed UF materialization is not yet active (the whitelist survey's "Future tier"), so today boxed shadows reaching recovery are constants or symbolic-input-derived; the soundness win (fresh identity → no aliasing collision) holds regardless, and register-only-non-constant is forward-ready for when boxed UFs land.
- **B (equality review): pending** — review `refEquals` / `==`→value-equality + the `reference_semantic_change` flag asymmetry for the de-interned cases.

## Problem (recap)

The shadow heap (G1) is now a **reference-keyed** weak identity map: a tracked value is recovered by the
real object's reference identity. For mutable objects (J1) that key is sound. For **immutable value types
(J2: String + boxed)** the reference is NOT a sound key — interning and `this`-returns make two logically
distinct values share one object (so one heap cell), or make a result share the receiver's object.

G2 defends at the *recovery* site (concretize an unmodeled value-typed return; don't consult/mutate the
heap for it). But a documented **residual** remains: if a value-typed result (e.g. `toLowerCase()`
returning `this`) flows through **untracked** space (an untracked field/store) and is later recovered by
its real reference, the heap lookup finds the *receiver's* stale symbolic cell → the concretized result
**re-aliases** to the receiver. This is **precision-only**: G2 already flags context-loss on the unmodeled
call (SAFE→UNKNOWN), and VIOLATION is replay-witnessed, so it cannot cause a wrong verdict — but it
pollutes tracking and inflates candidate-set sizes.

The principled fix for J2's "no sound reference key": make every produced value a **distinct object**, so
the reference key becomes sound for value types too (no two logical values share one object; no result
shares its receiver). That is **de-interning**, extended to method *returns*.

## What already exists (nocache, active by default `useStringInterning=true`)

`instrument/nocache/NoCacheMethodAdapter` already de-interns at instrumented sites:
- **String literals:** `LDC "x"` → `new String("x")`.
- **`String.intern()`** calls are removed.
- **Boxed `valueOf`** (`Integer/Long/Short/Byte/Character/Boolean.valueOf(prim)`) → `new Boxed(prim)`.

So de-interning of *inputs/literals* is done. G3 adds the missing **output boundary**: value-typed method
*returns*.

## Proposed solution (G3)

Extend `NoCacheMethodAdapter.visitMethodInsn`: after a call that returns a **value type** (v1: `String`),
wrap the result in a fresh object so the produced value has a fresh identity. Bytecode to wrap a `String`
already on the stack:
```
NEW java/lang/String ; DUP_X1 ; SWAP ; INVOKESPECIAL java/lang/String.<init>(Ljava/lang/String;)V
```
(leaves `new String(result)` on the stack). Gated on `useStringInterning` (the existing de-intern switch),
and only for calls at instrumented sites whose return descriptor is `Ljava/lang/String;`. Skip our own
de-intern `<init>` (void return — not value-returning, so naturally skipped) and `IGNORED_INVOCATIONS`-style
intrinsics.

Effect (D-1): every value-returning call yields a fresh-identity object → a `this`-return can no longer
alias the receiver, and the reference key is unique per produced value → the G2 residual closes and
candidate sets stay singletons.

## Why it's verdict-neutral / sound (D-2)

De-interning copies the value (`new String(s)` has the same content) — `.equals`/value semantics are
unchanged. The only behavioral change is **reference identity** (`==`): two formerly-`==` values become
`!=`. The executor already detects this (`SymbolicTraceHandler.recordReferenceSemanticChange`, fired in
`ObjectsInvocation` when de-interned values are reference-compared) and downgrades **VIOLATION→UNKNOWN**
(never SAFE→VIOLATION or VIOLATION→SAFE). So de-interning can only make a verdict *more* conservative
(a `==`-dependent VIOLATION becomes UNKNOWN), never wrong. The same reasoning already justifies the
existing literal/boxed de-interning.

## Acceptance tests (from heap-redesign-tests.md)

- **D-1 Fresh identity on produced values:** an unmodeled value-returning call at an instrumented site →
  result has a fresh identity distinct from inputs; a this-return cannot alias the receiver; candidate set
  is a singleton.
- **D-2 De-interning is verdict-neutral:** a program run with/without output de-interning → same verdict
  (soundness-neutral; de-interning never flips soundness).
- **D-3 reference-semantic-change only when warranted:** `==` on de-interned values → the flag fires only
  when de-interned `==` actually diverges from real reference semantics, not otherwise.

## The central tradeoff (the thing to decide)

De-interning returns is **precision on the SAFE/tracking side at a cost on the VIOLATION side**: more
distinct objects → more `==` divergence → more `reference_semantic_change` → more VIOLATION→UNKNOWN, plus a
`new String` allocation per value-typed return. The G2 residual it closes is precision-only (cannot cause a
wrong verdict). So G3's worth hinges on: does the tracking-precision gain (cleaner candidate sets, no
round-trip re-aliasing) outweigh the added VIOLATION→UNKNOWN cost + overhead?

## FINAL PLAN — boundary de-intern (un-instrumented value-type returns) + register-only-non-constant

Supersedes the "narrow this-return-prone" scoping below: that was a mis-framing (user-corrected). Interning is
**general** — a literal `"Hello"`, a constant, or any object returned from code we don't instrument can be
interned/shared, which is the same heap-key unsoundness as a `this`-return. So the criterion is the *boundary*,
not specific methods.

**Part 1 — Bytecode (`NoCacheMethodAdapter.visitMethodInsn`): de-intern un-instrumented value-type returns.**
After a call whose return type is a value type subject to interning (v1: `Ljava/lang/String;`) where **the
callee is not instrumented** (`!Util.shouldInstrument(owner)`), wrap the result, **null-guarded**:
```
DUP ; IFNULL L ; NEW java/lang/String ; DUP_X1 ; SWAP ; INVOKESPECIAL java/lang/String.<init>(Ljava/lang/String;)V ; L:
```
- **Skip the SWAT / sv-benchmarks intrinsic owners** (`org/sosy_lab/sv_benchmarks/Verifier`, `de/uzl/its/swat/**`
  — Intrinsics/Witness/UtilInstrumented/instrument.svcomp.Verifier): un-instrumented but the symbolic-input
  designation / witness seam — de-interning them would de-intern the symbolic input.
- Gate on `useStringInterning`. Value types only (boxed deferred; mutable J1 never de-interned).
- **Why complete:** instrumented code's literals are already de-interned at `LDC` (existing nocache), `intern()`
  removed, boxed `valueOf` de-cached; every value entering from un-instrumented space is now de-interned at that
  call. So the only un-de-interned values are exactly the un-instrumented returns this targets.

**Part 2 — Executor: register only NON-CONSTANT shadows (`:1366`).** The de-interned copy `O2` already registers
via the existing value-typed ADDRESS_UNKNOWN `putToHeap` (`:1366`) so it round-trips with no new mechanism — but
broad de-interning makes the **self-pinning leak** (a String is its own never-evicting weak-map key) grow. Fix:
guard that `putToHeap` on the shadow being **non-constant** — register iff `!fmgr.extractVariablesAndUFs(formula)
.isEmpty()`. A pure constant is fully reconstructible from the observed concrete (`inst.val`) on round-trip, so
registering it buys nothing; a symbolic/UF shadow carries a relationship that the concrete can't reconstruct, so
it must be registered. This keeps the full de-intern (identity fix for all) while bounding the leak to the
UF/symbolic round-trips that benefit.
- **Predicate = non-constant formula, NOT `isSymbolic()`** (a G4 UF result is built by a constructor, so its
  flag may be false though it carries a UF — guarding on the flag would wrongly skip exactly the win).
- **`:1366` is a SHARED path** (all value-typed ADDRESS_UNKNOWN recoveries, incl. modeled constant results like
  `concat`-of-constants), so the guard also stops registering those — safe by the same reconstructible argument,
  but broader than G3-only: auditors must confirm no non-G3 path relies on a constant being heap-registered. Also
  audit `:1363` (the `formula==null` sibling — a constant by definition; would now skip, consistent) and confirm
  `:1331` is irrelevant (placeholder path, value types return early at `:1303`).
- No other executor change. G2 concretize branch unchanged; the de-interned O2's UF survives (`invokeInit` copies
  the formula).

**Soundness / tests / costs:** as established by the prior audit — G3 can't flip a verdict; `==`/`refEquals` is
class-based + independent of de-interning; VIOLATION replay-witnessed; register-only-non-constant is sound
(constants reconstructible). Null-guard needed; the bytecode-wrap test MUST run with `checkClassAdapter=true`
(off in `sv-comp.cfg`). Tests at corrected altitudes: UF-survives-round-trip + a constant-still-recovers +
clobber-regression at L1; bytecode-wrap/null/loads+verifies + D-1 fresh-identity at L2; D-2 verdict-neutral at L3;
D-3 + measure the `Objects.equals` `reference_semantic_change` change (G3's O2 sets `userDeInterned`). Pre-existing
`replace(CharSequence)→this` `:1398` modeling inaccuracy: confirm de-intern (which wraps AFTER O1's GETVALUE)
doesn't worsen it.

### ROUND-2 VERDICT — CONVERGED (all 3 auditors): sound, complete, ready, with gates

- **Bytecode (auditor 1):** broad criterion sound. `!Util.shouldInstrument(owner)` is the correct, transformer-consistent boundary (and returns false for `java/lang/*` → those String returns de-interned, intended). **Refinement:** the skip-set MUST be an explicit literal owner check —
  `owner.startsWith("de/uzl/its/swat/") || owner.equals("org/sosy_lab/sv_benchmarks/Verifier")` — layered as an ADDITIONAL exclusion when `!shouldInstrument(owner)`; do NOT delegate to `shouldInstrument` (it already returns false for those, so it would no-op the skip; note its hard-coded true-cases for `svcomp/Verifier`+`UtilInstrumented` mean the explicit `de/uzl/its/swat/**` entry is the load-bearing protection for Intrinsics/Witness). Wrap mechanics invariant across invoke opcodes/stack depths. **Watch-item:** broad scope multiplies wrap sites → method-bytecode-size (64 KB) pressure on String-heavy methods.
- **Executor (auditor 2):** register-only-non-constant is correct + a STRICT IMPROVEMENT over the dropped fresh-guard (gates on shadow content, so it never wrongly skips a legit cell update — eliminates the round-1 first-wins hazard). Keeps G4 UF results; bounds the leak to symbolic/UF returns; round-trip + no-clobber hold. **Doc fixes:** (a) the reason to use `extractVariablesAndUFs` (not `isSymbolic()`) is that it's the safe SUPERSET (catches UF-over-constants) — NOT "the G4 result's flag is false" (for the result shadow reaching `:1366` it carries the input var, so the flag is true there); (b) the `:1363` `formula==null` sibling is always-constant → simply DROP its `putToHeap` rather than predicate-guard it.
- **Soundness/scope (auditor 3):** sound (broad scope touches only identity, which no `==`/witness path consults) + complete (boundary criterion + existing LDC/intern/boxed de-intern provably covers all interning entry points). register-only-non-constant resolves the heap-bloat leak. Residual over-reach = pure `new String` allocation on ALL String returns incl. modeled (no perf baseline) — accepted watch-item.

**TWO MUST-DO GATES (not optional):**
1. **Measure the `reference_semantic_change`-induced VIOLATION→UNKNOWN delta on the SV-COMP suite, before/after.** Broad scope makes the `Objects.equals(deInterned String, …)` firing strictly larger (every O2 sets `userDeInterned=true`); a currently-VIOLATION testcase flipping to UNKNOWN is a real score regression. (Mitigating fact: the suite currently has zero `Objects.equals` on Strings — auditor round-1 — and the `==`/refEquals path does NOT set the flag and is value-equality regardless of de-intern, so the expected delta is ~0; the gate is to confirm + catch regressions.)
2. **Run the bytecode-wrap test with `checkClassAdapter=true`** (`sv-comp.cfg` has it off, so a malformed wrap would otherwise degrade silently to a `VerifyError`→UNKNOWN).

**Tests:** L1 — UF-survives-untracked-round-trip; a constant-return still recovers (pins the `:1363` reconstruct path); clobber-regression; **the non-constant-guard predicate itself** (drive a GETVALUE at `:1366` with a constant-formula StringValue → assert `getFromHeap` stays null; with a UF-formula → assert registered). L2 (`checkClassAdapter=true`) — bytecode-wrap + null-return + loads-and-verifies + D-1 fresh-identity. L3 — D-2 verdict-neutral (de-intern on/off → same verdict). D-3 — assert the `Objects.equals`-on-de-interned firing + measure (gate 1).

### Round-2 audit re-check points (the changes vs the narrow plan)
1. The **broad criterion** (`!shouldInstrument(owner)` + value-type return, minus SWAT/sv-benchmarks intrinsics):
   is it sound + complete, is `Util.shouldInstrument(owner)` the right boundary test at the call site, and is the
   intrinsic skip-set correct/sufficient?
2. **Register-only-non-constant at the shared `:1366`**: is `!extractVariablesAndUFs(formula).isEmpty()` the right
   predicate, does it correctly keep G4 UF results (flag-independent), and is skipping constant registration safe
   for ALL value-typed ADDRESS_UNKNOWN recoveries (no non-G3 regression)?
3. Surviving items: null-guard + `checkClassAdapter=true` test; the `replace`/`:1398` interaction; the
   `Objects.equals` firing change; test altitudes.

## (Superseded) CONVERGED PLAN (narrow this-return-prone) — replaced by the FINAL PLAN above

Three independent auditors (bytecode / executor-clobber / soundness-scope-tests) converged. Soundness is
confirmed by all three (G3 cannot flip SAFE↔VIOLATION; the `==`→value-equality rewrite is class-based and
total-for-Strings, hence **independent of** whether G3 de-interns any object; VIOLATION is replay-witnessed on
the already-de-interned program). The plan is revised as follows:

1. **DROP the fresh-guard (it was misdiagnosed).** A *modeled* `this`-return (`toString()→this`) returns the
   receiver object carrying its **real address**, so it takes the address-match branch (`:1397-1399`) and does
   NO `putToHeap` — it never reaches `:1366`, so there is no clobber to guard. And the de-interned copy `O2` is
   always a fresh object (`getFromHeap(O2)==null` always), so the guard would never trigger on it; worse, a
   `getFromHeap==null` guard would change `:1366` from latest-wins to first-wins for already-keyed Strings,
   which some paths (the `setAddress`+`putToHeap` pattern) may rely on. → **Do not add the guard.** Instead,
   first write the clobber test against *current* code and confirm it is already green (it is, per audit).
2. **NO new executor code needed.** The de-interned `O2` (a fresh `String` carrying the copied UF/concretized
   formula via `invokeInit`, which preserves concrete+formula) already lands on the existing value-typed
   registration at `:1364-1366` (reached via the wrap's `<init>` GETVALUE), so it is `putToHeap`'d and
   **round-trips correctly** (recovering the G4 UF for whitelisted methods). The user's "does the symbolic
   handling store it correctly?" → **yes, the existing path already does**, once de-interning gives `O2` a
   fresh identity. (G2's concretize branch stays unchanged — O1 is consumed by the wrap; only O2 is registered.)
3. **NARROW the bytecode scope (all three flagged broad-scope problems):**
   - **MUST skip the symbolic-input / SWAT intrinsic owners.** `nocache` runs BEFORE the SV-COMP transformer,
     so a blanket String-return wrap would de-intern `org/sosy_lab/sv_benchmarks/Verifier.nondetString()` — the
     symbolic input at its designation seam. `NoCacheMethodAdapter` cannot see `InvocationHandler.IGNORED_INVOCATIONS`,
     so mirror a minimal skip-set (`de/uzl/its/swat/**` owners + `org/sosy_lab/sv_benchmarks/Verifier`).
   - **Restrict to `this`-return-prone `java/lang/String` methods**, not all String returns — bounds (a) the
     self-pinning heap leak (each de-interned String return is a never-evicting weak-map cell, since a String
     is its own key), (b) the per-return `new String` overhead on the hottest opcode class, and (c) the one
     real `reference_semantic_change` increase (C-4 below). Curated v1 set: `toLowerCase`, `toUpperCase`,
     `trim`, `strip`, `stripLeading`, `stripTrailing`, `substring` (by owner+name+desc at the call site).
     **Exclude `replace(CharSequence,CharSequence)`** (its modeled `this`-return can trip the `:1398`
     address-match assertion — a pre-existing inaccuracy G3 shouldn't poke) and `concat`/fresh-producers
     (no aliasing → pure cost).
4. **Null-guard confirmed needed**; and `instrumentation.checkClassAdapter=false` in `sv-comp.cfg`, so a bad
   wrap is NOT caught by the verifier (it surfaces as a silent `VerifyError`→UNKNOWN). → the bytecode-wrap test
   MUST run with `checkClassAdapter=true`.
5. **One real cost to test+measure (C-4):** G3's `O2` sets `userDeInterned=true` (via `invokeInit`), so a
   program doing `Objects.equals(trim(x), s)` now fires `reference_semantic_change` where a PlaceHolder result
   previously might not → more VIOLATION→UNKNOWN. Sound (only conservatism), but contradicts the earlier
   "~0 added firings"; narrowing the scope minimizes it. Cover with a D-3 test + measurement.
6. **Corrected test altitudes:** UF-survives-round-trip + clobber-regression at **L1** (drive GETVALUE with
   chosen identities, introspect `getFromHeap`); bytecode-wrap correctness + null-return + loads-and-verifies
   at **L2** with `checkClassAdapter=true`; D-2 verdict-neutral at **L3** (same verdict with de-intern on/off);
   D-3 `Objects.equals`-firing at L1/L2. The L1 fixture is post-instrumentation so it can't test the wrap;
   "candidate-set singleton" isn't observable at L2 without a new field — demote to L1 heap-introspection.
7. **Confirm the win materializes:** before paying even the narrow cost, confirm at least one in-scope pattern
   exists where a whitelisted pure String method's result round-trips through untracked space (the UF-survival
   win G3 uniquely enables).

Net: G3(b) shrinks to **a narrow, null-guarded, intrinsic-skipping bytecode de-intern of `this`-return-prone
String methods** + **no executor change** (existing registration suffices) + tests at corrected altitudes.

## (Original) PLAN — superseded by the converged plan above

Corrected after user steering: the earlier "defer" reasoning under-valued G3 (it framed it as a narrow
residual). G3 is the *core* fix that makes the reference key sound for value-typed returns, enabling them
to be tracked (UF for whitelisted, concretized otherwise) **through untracked round-trips** (option (b)).
The `==`/`refEquals` concern noted below is pre-existing, total-for-Strings, and INDEPENDENT of G3
(`refEquals` maps every String `==` to value-equality based on the class, not the object) — so it neither
motivates nor blocks G3.

### Part 1 — Bytecode: de-intern value-typed returns (NoCacheMethodAdapter.visitMethodInsn)
After a call whose **return descriptor is `Ljava/lang/String;`** (v1) at an instrumented site, give the
result a fresh identity, **null-guarded** (`new String((String)null)` would NPE):
```
DUP ; IFNULL L ; NEW java/lang/String ; DUP_X1 ; SWAP ; INVOKESPECIAL java/lang/String.<init>(Ljava/lang/String;)V ; L:
```
- Gate on the existing `useStringInterning` switch. Value types only (String in v1; mutable J1 returns keep
  their sound identity key — never de-intern them). Boxed returns deferred.
- Applies to **all** String returns incl. modeled ones: de-interning fixes the *real object identity*
  (this/interned), independent of how the executor shadows it. The shadow flows through unchanged — the
  wrap's `String.<init>(String)` runs `invokeInit`, copying the shadow's concrete + formula (so a modeled
  or G4-UF formula survives; only identity becomes fresh).

### Part 2 — Executor: make registration sound + round-trip-correct (visitGETVALUE_Object)
The forward (de-interned) object `O2` is a fresh `String` carrying the shadow's formula; it already lands on
the non-placeholder ADDRESS_UNKNOWN path which **already** `putToHeap`s it (`:1366`) → round-trip recovery
works with no new registration code needed for `O2`. The fix is to make that registration **non-clobbering**:
- **Fresh-guard the value-typed `putToHeap`**: register only when the object isn't already tracked
  (`getFromHeap(inst.val) == null` before `putToHeap` at `:1366`, and audit `:1363`/`:1331`). This prevents a
  **modeled `this`-return** (`O1 == receiver`, possibly reaching `:1366` with ADDRESS_UNKNOWN) from
  overwriting the receiver's symbolic cell — re-introducing the very aliasing G3 removes. (Unmodeled
  this-returns are already handled+returned in the G2 concretize branch `:1278-1304`, never reaching
  `:1366`, so they don't clobber today.)
- Net executor change is small: a guard, not a new mechanism. G2's deliberate "don't putToHeap" for the
  concretize branch can stay (O1 is consumed by the de-intern; O2 carries the shadow forward and is
  registered at `:1366`).

### Two-object (O1 → O2) model (the subtlety to audit)
Each de-interned return yields O1 = the method's real return (maybe `this`/interned, immediately consumed by
the wrap's `<init>`) and O2 = `new String(O1)` (fresh, flows forward). Both trigger a `GETVALUE_Object`. The
plan relies on: O1 (unmodeled) being handled in the G2 branch without registration; O1 (modeled this-return)
being **skipped by the fresh-guard** at `:1366`; O2 being fresh → registered at `:1366`. Round-trip recovers
O2's shadow (the UF for whitelisted methods — the goal).

### Acceptance tests
D-1 fresh identity (L2: this-return produces a distinct-identity result, candidate set singleton); a new
**round-trip** test (L2: a whitelisted pure return stored via untracked space then recovered keeps its UF —
no re-alias to receiver, shadow preserved); D-2 verdict-neutral; D-3 reference-semantic-change only when
warranted; plus a **clobber regression** test (a this-return must not overwrite the receiver's shadow).

### Key audit questions (for the three auditors)
1. **Bytecode**: is the null-guarded wrap correct (stack states, COMPUTE_FRAMES + CheckClassAdapter, the
   IFNULL frame)? Does `LocalVariablesSorter` need anything? Discarded-result / chained-call edges?
2. **The clobber/fresh-guard**: does a modeled `this`-return actually reach `:1366` with ADDRESS_UNKNOWN and
   `O1 == receiver`? Is the `getFromHeap == null` guard the right and sufficient fix at `:1366` (and
   `:1363`/`:1331`)? Could the guard wrongly SKIP a legitimately-fresh registration? Is there any non-G3 path
   that already clobbers here today?
3. **Round-trip correctness**: does O2 (fresh String with the copied UF/concretized formula) actually get
   registered at `:1366` and recovered on an untracked round-trip? Does `invokeInit` truly preserve the
   formula (so the UF survives the de-intern copy)?
4. **Soundness/verdict-neutrality (D-2)**: can de-interning + the registration change a verdict's soundness
   (vs only conservatism)? Interaction with G2 (concretize branch unchanged), G4 (UF formula carried through
   the copy), G_oob (divergence detection unaffected?).
5. **Scope/perf**: all String returns (incl. modeled) — acceptable overhead, or narrow? Is registering every
   de-interned String return going to bloat the heap / change candidate-set behavior unexpectedly?

## (Superseded) earlier defer analysis — kept for context

An adversarial review (verified against code) recommends **not doing G3 now**:
- **Precision-only + narrow.** The residual G3 closes is a 4-way intersection (unmodeled call ∧ this-return ∧ untracked round-trip ∧ receiver previously heap-registered) and already yields the correct *conservative* verdict (G2 context-loss → SAFE→UNKNOWN at `InvocationHandler.java:138`/`SVCompDriver.py:304`; VIOLATION replay-witnessed). No current target exhibits a precision problem it would fix.
- **The "cost tradeoff" as framed is ~0.** `reference_semantic_change` fires ONLY on `Objects.equals(String,String)` (one caller, `ObjectsInvocation.java:47`); the SV-COMP suite has zero such String uses. So broad de-interning adds ~0 flag firings — but also means the advertised "VIOLATION→UNKNOWN conservatism" does NOT cover the `==` path.
- **Real costs/bugs G3 would add:** a `new String` per String return incl. already-MODELED returns (overhead, redundant); and a real **null-return NPE** (`new String((String)null)` throws) for any in-scope method that can return null.
- **It widens a pre-existing soundness approximation (the real issue):** `String ==` is rewritten by `RefEqualityMethodAdapter` → `UtilInstrumented.refEquals` → `Objects.equals` (value-equality) for de-interned classes, **with no flag**. So `new String("x") == new String("x")` is modeled as **true** (real JVM: false), unflagged, and VIOLATION is witnessed by re-running this de-interned+value-equality program. This is independent of G3 (it exists for literal/boxed de-interning today) but G3 would enlarge the de-interned-String population and thus the divergence surface. **Deciding whether this `==`→value-equality approximation is acceptable is the higher-value soundness question in this area — not the G2 residual.**

**If G3 is done anyway:** narrow hard (only the unmodeled `this`-return-prone String methods — `toLowerCase/toUpperCase/trim/strip/substring`-style — not all String returns; feasible at the call site via callee owner/name), add a null guard, and first resolve/document the `==`/refEquals gap.

## Risks / open questions for review

- **Worth it + scope.** Is *broad* String-return de-interning right, or should it be narrowed (only
  `this`-return-prone methods like `toLowerCase`/`trim`; or only value-returning calls in symbolic scope)
  to bound the `reference_semantic_change` cost? Is G3 worth doing now at all, given it's precision-only and
  G1/G2/G4 already prevent wrong verdicts?
- **Bytecode correctness.** Is `NEW; DUP_X1; SWAP; INVOKESPECIAL <init>(String)V` the right wrap for a
  String on the stack? COMPUTE_FRAMES/verify implications; interaction with the existing `CheckClassAdapter`.
- **reference_semantic_change cost (D-3).** Does the existing flag fire *only when warranted*? Will broad
  return de-interning materially increase VIOLATION→UNKNOWN on the SV-COMP suite? (Plan says "measure
  candidate-set sizes; monitor reference_semantic_change cost.")
- **Verdict-neutrality (D-2).** Confirm de-interning a return can never change a verdict's soundness, only
  conservatism.
- **Interaction with G2/G4.** Does de-interning returns make G2's concretize partly redundant (the heap
  recovery would no longer find the receiver)? Does it interfere with G4's UF-modeled returns (the UF
  result is a materialized value; de-interning the real object underneath it — any conflict)?
- **Performance.** Return sites are hot-ish (every value-returning call); a `new String` per String return.
  No perf baseline exists. Acceptable, or gate/narrow?
- **Boxed returns.** Defer to a follow-up (wrapping a boxed return needs unbox+rebox), or include? v1 =
  String only (matches G2/G4 v1 scope).
- **Testing altitude.** D-1/D-3 likely L2 (real instrumentation + real identities); D-2 is L3 (verdict
  with/without). What's feasible?

---

# G3-B — Exact reference-equality via provenance (replaces the conservative flag)

Phase G3-B. Design for review (after A1 `994e642` + A2 `791ce41`). Converged with the user before audit.

## Problem
De-interning gives value-type objects fresh identities, which breaks reference-`==`. The current mitigation
(pre-G3) rewrites every `==`/`!=` to `UtilInstrumented.refEquals`, which uses **value-equality** for
de-interned classes, and a flag (`recordReferenceSemanticChange`) that downgrades VIOLATION->UNKNOWN when
value-equality might diverge from real reference-`==`. `UtilInstrumented` is instrumented, so the `==` path's
inner `Objects.equals` converges at `ObjectsInvocation`, which is where the flag lives. The flag is sound but:
1. **String-only** (`instanceof StringValue`) -> A2's de-interned **boxed** `==` divergence is unflagged
   (e.g. `Integer.valueOf(200) == Integer.valueOf(200)`: real false, de-intern+value-eq true) -> a real
   soundness gap.
2. **Over-fires** -> it flags comparisons that do NOT actually diverge (operands sharing the same original but
   de-interned to distinct copies: `"x" == "x"`, a this-return `r == s`, and any `s == "literal"`), producing
   spurious UNKNOWN and **losing SAFEs the user confirms cost us**.

Root cause: `==` is **reference identity** (a concrete, per-path fact), but de-intern + value-equality models
it as a **value comparison**, which diverges exactly for equal-value / distinct-original-identity operands.

## Design — model `==` by ORIGINAL identity (exact), drop the flag
1. **Stop de-interning constants.** `NoCacheMethodAdapter.visitLdcInsn` no longer de-interns String literals.
   Literals are constants -> A1 `register-only-non-constant` never heap-registers them -> they cause no
   reference-key collision -> they never needed distinct identities. Left interned, their `==` is real
   reference identity (exact, no map). This removes the largest source of `==` divergence (`"x"=="x"`,
   `s=="literal"`) for free and shrinks the provenance map to the genuinely-needed residual.
2. **Provenance map** for the remaining de-interned objects (non-constant un-instrumented returns -
   this-returns etc.): a process-wide identity-keyed, weak-keyed map `copy -> root(original)` (guava
   `MapMaker().weakKeys()`, the same primitive `JVMHeap` uses), populated at the de-intern step
   (`NoCacheMethodAdapter` records `(copy, original)` when wrapping a return; `root` collapses chains so a
   copy-of-a-copy resolves to the ultimate original).
3. **`refEquals`** (concrete branch decider): `shouldUseValueEquality(a,b) ? root(a) == root(b) : a == b` -
   **reference on the originals**, NOT `Objects.equals`. Exact original `==`.
4. **Symbolic `==` becomes a per-path concrete constant.** Reference identity is not solver-controllable
   (distinct allocations are never `==` regardless of input values; value comparison is `.equals`), so the
   `refEquals` result is concretized. **Delete** `recordReferenceSemanticChange` and the `instanceof
   StringValue` divergence block in `ObjectsInvocation`. Explicit `Objects.equals` / instance `.equals` stay
   value-equality (correct - those are value comparisons and are de-intern-invariant).

Result: `==` exact in every case (interned literal, this-return, distinct objects, alias, boxed cache) for
String AND boxed; no flag; no spurious UNKNOWN.

## Why each case is now exact
| Operands | real `==` | provenance model | how |
|---|---|---|---|
| alias (`b = a`) | true | true | same object / same root |
| different values | false | false | distinct roots |
| two interned literals `"x"`,`"x"` | true | true | not de-interned -> same interned object |
| `s == "literal"` (input vs literal) | false | false | distinct roots (s not de-interned, literal interned) |
| `new String("x") == "x"` | false | false | distinct roots |
| this-return `s.toLowerCase() == s` | true | true | copy's root **is** s |
| boxed `valueOf(200) == valueOf(200)` | false | false | distinct roots |

## Open questions for the auditors
- **Soundness:** does provenance + concretized `==` reproduce real reference-`==` in EVERY case above (and any
  missed), for String AND boxed? Does concretizing lose any constraint the solver actually needs (claim: no -
  identity is concrete/per-path)? Any path where a wrong verdict could result?
- **Literal-handling safety:** is leaving literals interned safe - does anything rely on the original LDC
  de-intern (why was it there)? Interaction with `register-only-non-constant`, the heap, G2 concretize, G4 UFs?
  Do literals ever need distinct identities for a reason we're missing?
- **Feasibility & cost:** the de-intern bytecode restructure to keep the original accessible (String: an extra
  DUP; boxed: reuse the local); the map reachable consistently from concrete `refEquals` (raw object) and the
  symbolic side (`shadow.concrete`); weak-key GC behavior; per-`==` lookup cost; the interaction with
  `UtilInstrumented` being instrumented (do we still need that special-case once `refEquals` is reference-only
  and concretized?).
- **Scope:** does the de-intern step record provenance for ALL de-interned returns (incl. constant returns it
  can't distinguish at bytecode), and does that matter for map size / correctness?

---

## G3-B REVISED (after round-1 of 3 design auditors + a characterization experiment)

Round-1 outcome: the design is sound + buildable but the framing had two errors, one "blocker" was a mis-analysis, and a characterization experiment changed the mechanism. Revised design below; this is what round-2 reviews.

### Experiment (settles the auditor contradiction)
Ran a symbolic `s`, `if (s == "abc")` under the real agent. Result: branch constraint = `(assert true)` (NOT a flippable value-equality constraint), AND `symbolicContextLoss=true` AND `referenceSemanticChange=true`. Conclusions:
- `==` is **concretized** today (the value-equality computed inside `refEquals`'s instrumented body is discarded; the call's boolean is concretized). The feasibility auditor was right; the "solver-controllable value-equality" framing was wrong.
- **Context-loss is a SECOND flag** firing in this area (because `refEquals(s,…)` concretizes a symbolic operand). Fixing only `referenceSemanticChange` would NOT recover the SAFE — context-loss would still fire.

### Corrected mechanism — MODEL `refEquals` as a reference comparison (not "concretize")
G3-B must make `refEquals` a **modeled** reference comparison so that BOTH flags correctly do not fire and the result is exact:
- Route `refEquals` to a model that returns `root(a) == root(b)` as a **concrete identity boolean** for de-interned classes (`a == b` otherwise). Because the result depends only on object identity, not on `s`'s symbolic value, it must NOT trigger context-loss, and (provenance being exact) needs no `reference_semantic_change` downgrade.
- This supersedes the earlier "concretize the result" wording. The point is to model `==` as the (concrete, per-path) reference fact it is, sourced from `root`.

### Soundness (corrected) — the round-1 "blocker" was a mis-analysis
The soundness auditor's claimed VIOLATION->SAFE (un-instrumented callee returns a JVM-shared object, SWAT de-interns it, `root(b)!=a` -> missed violation) does NOT hold: we record `root(copy) = the genuine pre-wrap returned object`, which carries the **real JVM identity**. So `root(a)==root(b)` reproduces real `==` exactly, including canonical-cache hits. The auditor assumed `root` points to a fresh copy; it points to the genuine original. **Hard requirement that makes this true:** record the genuine pre-wrap reference at EVERY de-intern site.

Corrected justification (replaces "identity isn't solver-controllable"): concretizing `==` is sound because real reference-`==` is a **per-path concrete fact** and VIOLATION is **concretely witnessed** on the transformed program. Removing the discarded value-equality loses nothing real — a `==`-true path that only value-equality reached is infeasible in real Java (distinct objects are never `==` regardless of value).

### Scope (decided): `==`/`!=` only; identity-hash is a documented residual (option A)
Provenance makes `==`/`!=` exact. `System.identityHashCode` / `IdentityHashMap` / identity-keyed logic in **un-instrumented** code on a de-interned value is an **irreducible, pre-existing, rare residual** (nocache has de-interned literals/boxed since before G3): reinjecting the original at the boundary would break the round-trip recovery (the de-interned identity must persist across the boundary for `getFromHeap`), and a redirect can't reach un-instrumented calls. Decision (user): document it; do NOT build a redirect or reinjection. The exactness claim is scoped to `==`/`!=`.

### Implementation obligations (from all three round-1 auditors)
- **Atomic package:** stop-de-interning-literals (point 1) + model-refEquals-via-root (point 3) + delete-the-flag (point 4) must land together (the only consumer of `userDeInterned` is the flag block). Point 1 is **String-literal-only** (no boxed-literal analogue; boxed exactness comes from provenance).
- **Record provenance at EVERY de-intern site** with the genuine pre-wrap reference - LDC (if any de-intern remains there - but point 1 removes it), the 6 `valueOf` rewrites, and `deInternReturn`. A missed site breaks a `root` chain -> divergence. No single choke point today.
- **Map:** `MapMaker().weakKeys().weakValues()` (identity keys, like `JVMHeap`); `root(x)` returns `x` on a miss (collected or never-recorded) and collapses chains. Weak keys alone strongly pin the originals (leak); weak values + root-on-miss=self is leak-safe AND correct.
- **Bytecode:** restructure `deInternReturn` to keep the original across the wrap (String: extra DUP / a local; boxed: store the boxed original, not just the primitive), null-guard-correct, and tested with `checkClassAdapter=true` (sv-comp.cfg has it off). Compounds the 64KB method-size watch-item.
- **Deletions span 3 languages:** Java (`recordReferenceSemanticChange` + `userDeInterned` + the `ObjectsInvocation` block + the TraceDTO/SymbolicTrace field), the explorer (`SVCompDriver.py` VIOLATION->UNKNOWN downgrade + DTO parse), and Groovy tests (`TraceObservation`, `BaseSymbolicInstructionProcessorSpec`). Coordinate, or leave the explorer field inert with a comment - don't leave a silently-dead downgrade.
- **Leave alone:** `UtilInstrumented`'s instrumented status + the RefEquality skip-set (entangled with `liftClass` + the regress guard).
- **Re-verify A1** "a constant String is never `putToHeap`'d" under the no-literal-de-intern assumption (it's what makes leaving literals interned safe), and add a regression test that two occurrences of the same literal don't merge in the heap.

### Round-2 questions
- Is "model `refEquals` as `root`-reference" the right mechanism, and does it actually avoid BOTH context-loss and `reference_semantic_change` (verify the executor doesn't flag a modeled reference comparison with a symbolic operand)?
- Is the corrected soundness argument airtight (genuine-pre-wrap-reference recording at all sites => `root` == real JVM identity => exact `==`; no VIOLATION->SAFE)?
- Is the atomic-package + record-at-all-sites + weakValues + 3-language-deletion plan complete and correctly sequenced?

---

## G3-B round-2 outcome (all 3 auditors): String converged + sound; mechanism corrected; boxed fork

- **Blocker RESOLVED.** Soundness auditor withdrew round-1 Finding C: recording `root(copy)=genuine pre-wrap object` => `root` carries real JVM identity => `root(a)==root(b)` is exact reference-`==` (incl. canonical hits). No VIOLATION->SAFE for String.
- **MECHANISM CORRECTED (decisive, feasibility auditor).** The design's "route `refEquals` in the invoke dispatch" is **DEAD CODE**: `UtilInstrumented` is force-instrumented, so the engine STEPS INTO `refEquals`'s body (the `getNextInst() instanceof INVOKEMETHOD_END` discriminator is false for instrumented callees) and never models the call -> `StaticInvocation` routing is never consulted. The two flags fire from INSIDE the stepped body: `symbolicContextLoss` from `Util.shouldUseValueEquality(s,..)` (un-instrumented `common/Util`, NOT in IGNORED, symbolic arg) and `referenceSemanticChange` from the inner `Objects.equals`. **Correct mechanism:** rewrite `refEquals`'s BODY to `shouldUseValueEquality(a,b) ? Provenance.root(a)==Provenance.root(b) : a==b`, and add `Provenance.root` + `Util.shouldUseValueEquality` to `IGNORED_INVOCATIONS` (so those inner calls concretize without firing context-loss). Keeps `UtilInstrumented` instrumented (no contradiction with leaving its status alone). Then `root(a)==root(b)` is an IF_ACMPEQ on concretized root objects -> a constant identity boolean -> no flags, exact.
- **HARD requirement:** collapse provenance chains at INSERTION (store the fully-resolved root at `record`), NOT lazy lookup -> eliminates the GC'd-intermediate-link VIOLATION->SAFE that `weakValues()`+lazy-walk would introduce.
- **BOXED FORK (real, new).** The Integer cache makes boxed `==` harder than String: `Integer.valueOf(100)==Integer.valueOf(100)` is TRUE in real Java (cache), and today's value-equality `refEquals` gets it right. But the existing `valueOf` de-intern turns both into distinct `new Integer(100)`, so `root`=self -> FALSE -> with the flag deleted that is a **false SAFE (regression)**. We CANNOT "stop de-interning boxed like literals" because `valueOf` can take a symbolic value (cache-range collisions), unlike always-constant String literals. Two sound options:
  - **(X) Exact boxed:** at each `valueOf` de-intern site, also call the real `valueOf` to get the cached canonical and `record(freshBox, canonical)`. Then `root(a)==root(b)` is exact for boxed too (cache-hit -> same canonical -> TRUE; non-cache -> distinct -> FALSE). Cost: one extra `valueOf` call per de-intern site. Flag fully deleted; consistent with String.
  - **(Z) Hybrid:** String exact via provenance (no String flag); boxed keeps value-equality + a conservative flag (the original A2-gap fix) -> sound, boxed `==`-divergence -> UNKNOWN. Keeps a flag for the rare boxed case.
- **Doc corrections:** the identityHashCode/IdentityHashMap/argument-identity residual is **unsound w.r.t. real Java** (accepted, no backstop) - not merely "rare/precision."

---

## G3-B CORRECTION — literals DO turn symbolic; de-intern ALL (no "stop de-interning literals")

Empirically confirmed (ran a `@Symbolic String s = "seed"` target under the agent): the literal `"seed"`
becomes a **symbolic String input** (`inputNames=[java/lang/String_0]`, a branch references it). So a literal
is NOT "always a constant" - it can be made symbolic (the designated input is a literal; more generally any
string may acquire a symbolic shadow). Therefore it needs a **distinct identity** so that (a) it is registered
and recoverable through untracked space, and (b) it does not collide with a same-valued *plain* literal that
shares the interned object. **=> "Stop de-interning literals" (REVISED point 1) is WITHDRAWN as unsound.**

**Corrected model = UNIFORM (the user's original assumption): de-intern ALL value types + provenance everywhere.**
- Keep the existing de-intern at ALL sites (LDC literals + boxed `valueOf` + un-instrumented returns). No
  special-casing.
- Record provenance `copy -> root(genuine canonical/original)` at EVERY de-intern site, collapsed-at-insert:
  - **LDC literal:** the interned literal (the LDC value, on the stack before the wrap; DUP it). Two `"abc"`
    literals -> same interned canonical -> `root` equal -> `==` true (exact). A literal-that-turns-symbolic ->
    distinct de-interned identity, registered, recoverable; `==` vs a plain same-value literal -> both root to
    the interned canonical -> true (exact, matches real JVM).
  - **boxed `valueOf`:** the cached canonical (extra real `valueOf` call - the rewrite replaces valueOf before
    it runs). Cache-hit -> same canonical -> true; non-cache -> distinct -> false (exact).
  - **un-instrumented return:** the genuine pre-wrap returned object.
- `refEquals` body -> `shouldUseValueEquality(a,b) ? root(a)==root(b) : a==b`; `Provenance.root` +
  `Util.shouldUseValueEquality` in IGNORED_INVOCATIONS (suppress context-loss); delete the flag.

This is simpler (no literal special-casing, no "is it safe to stop de-interning literals" analysis, no
heap-isolation-for-interned-literals concern) and is exactly uniform across String + boxed: every value type is
de-interned and carries provenance to its canonical/original. Cost: a `record` call per de-intern site (incl.
per literal - bytecode/64KB watch), plus the extra `valueOf` at boxed sites.
