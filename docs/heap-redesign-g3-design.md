# G3 — Output-boundary de-interning: Problem & Proposed Solution

Phase G3. Draft for review (not yet implemented). Builds on G1 (`67505c4`) + G2 (`e84b46e`) + G_oob (`5b35699`) + G4 (`48763bc`/`6d39904`/`58c3932`).

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
