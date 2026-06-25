# G_oob — Out-of-band change detection (no havoc): Problem & Proposed Solution

Phase "G_oob" of the heap redesign. **Status: IMPLEMENTED on `fix/heap-design`** (1 design-review round + 3 independent post-impl reviews for style/refactoring/correctness). Builds on G1 (`67505c4`) + G2 (`e84b46e`). Addresses confirmed bug (b): a tracked object's concrete state diverging from its shadow (e.g. mutated inside unmodeled code) is not handled soundly.

**Landed scope (user decision: "configurable now, differentiate with G4a"):** a configurable `shadowDivergence` policy = **CRASH** (default; the original hard `SWATAssert`, for dev/CI bug-catching) | **FLAG** (graceful: record context loss → SAFE downgraded to UNKNOWN, adopt the observed concrete, continue; sound, recommended for production). Scoped to the **primitive `GETVALUE`** path only. **Deferred to G4a:** the `DIFFERENTIATED` (escape-aware) policy and the escape bit — so the escape set is built once, faithfully, with the purity whitelist; and StringBuilder/object-ref/array divergence detectors. Reuses the existing `symbolic_context_loss` flag (already SAFE→UNKNOWN) rather than wiring a new `outOfBandChange` flag (a clean fast-follow if per-stat distinction is wanted). Acceptance: E-1/E-2 green (L1, `GoobDetectionSpec`); all heap+processor+agentTest green; default CRASH = byte-for-byte prior behavior.

## Problem (recap, grounded)

When a tracked object is passed to unmodeled code that mutates it, the shadow `fields[]` go stale. The divergence surfaces at the next `GETVALUE` concrete-sync (the shadow value's concrete vs the real observed value). Today that divergence is **detected but handled as a hard failure**, not a graceful soundness signal:

- **Primitives:** `visitGETVALUE_primitive` (`SymbolicInstructionVisitor.java:3631-3641`): on mismatch (`!checkEquality(peek.concrete, inst.v)`) it executes `SWATAssert.check(false, "[GETVALUE_primitive]: Value on stack does not match expected value!...")` — a *literal-false* assert that always fails when reached, so it throws/halts (depending on `exitOnError`). The recovery right after it (pop stale, push a fresh concrete from `inst.v`, `:3635-3641`) is therefore **dead code** — unreachable past the always-failing assert.
- **String / StringBuilder:** the placeholder/`ADDRESS_UNKNOWN` recovery has the same shape — `SWATAssert.check(inst.val.equals(peek.concrete), "Concrete value of the object does not match...")` (`:1334`) and the StringBuilder variant (`:1296`).

So an out-of-band change is **a crash** (or, with `exitOnError=false`, a thrown `SymbolicInstructionException`), in every mode — never a recorded, recoverable soundness loss. (The earlier audit's "stale value silently trusted" framing was for paths where no `GETVALUE` re-syncs; where a `GETVALUE` does fire, it currently crashes.)

## Proposed solution (detect, no havoc, all modes)

**Convert the hard divergence-assert into a graceful, flagged detection.** At each `GETVALUE` concrete-sync, when the observed concrete diverges from the shadow's concrete:

1. **Record an out-of-band-change soundness flag** (see flag choice below) — the value's symbolic state is now unsound, so downstream verdicts can be downgraded.
2. **Adopt the observed concrete** — replace the stale shadow value with a fresh **non-symbolic** value built from the observed concrete (`ValueFactory.createNumericalValue(type, inst.v)` / `createObjectValue`), restoring the concrete-grounding invariant. This is the recovery that already sits (dead) after the assert.
3. **Continue** — no crash, no havoc of the wider object graph (v1 is *detect-only*: we flag the one diverged value and re-ground it; we do not invalidate other fields or attempt repair).

This is **mode-independent**: `GETVALUE` is part of the shadow interpreter and runs under every `solver.mode` (LOCAL/HTTP/PRINT/NONE), so detection fires "in all modes."

Concretely: at `:3631-3641` replace the `SWATAssert.check(false, …)` with `record-flag` + the (now-live) adopt-observed recovery; do the same at the String/StringBuilder asserts (`:1296`, `:1334`).

## Flag choice (open — leaning new flag)

`SymbolicTrace` has `symbolicContextLoss` and `referenceSemanticChange`. An out-of-band change is neither exactly. Proposed: add a dedicated trace flag (e.g. `outOfBandChange`) with getter/recorder mirroring the existing two, surfaced on `TraceDTO`, and downgrade verdicts like context loss does (SAFE→UNKNOWN). VIOLATION needs no downgrade (replay-witnessed, as established in G2). Alternative: reuse `referenceSemanticChange` (avoids new wiring) — but it muddies that flag's meaning. Reviewer to weigh in.

## Scope / non-goals

- **Detect-only.** No havoc of the object's other fields, no attempt to re-symbolize. Just flag + re-ground the diverged value.
- **No escape-tracking.** We do not (yet) distinguish "legitimate out-of-band mutation (object escaped to unmodeled code)" from "executor-internal desync bug." That distinction needs escape/leak tracking (G4a territory) and is deferred.

## The key risk (open question for review)

The divergence-asserts currently serve **two** purposes: catching legitimate out-of-band changes (which we now want to flag gracefully) **and** catching executor-internal desync **bugs** (which we want to keep catching loudly). Blanket flag-and-continue would **mask real executor bugs** as benign out-of-band changes — a regression in bug-detection sharpness. Options:
- **(a)** Flag-and-continue for all divergences (simplest; matches "detect in all modes"; loses dev-time bug catching).
- **(b)** Config-gated: keep the hard assert under a strict/debug flag (e.g. tie to `exitOnError` or a new `strictShadow`), flag-and-continue otherwise. Preserves dev bug-catching, graceful in real runs. *(Recommended.)*
- **(c)** Only flag-and-continue when the object is known to have escaped (needs escape-tracking → defer).

## Acceptance tests

- **E-1 (detection):** a tracked value whose concrete diverges from the observed concrete is flagged (not crashed) and the observed concrete is adopted. Cleanest at **L1** (fabricate the divergence: push a tracked `IntValue` concrete=10, run `GETVALUE_int` with `inst.v=20` → assert the out-of-band flag is set, no exception, value re-grounded to 20). Plus an **L2** variant if a real escape-then-mutate target is constructible (mutating mock), to satisfy "in all modes."
- **E-2 (no false positive):** a pure (no-mutation) call leaves no divergence → no flag.

## Open questions for review

- Flag choice (new `outOfBandChange` vs reuse `referenceSemanticChange`) + the verdict-downgrade rule.
- The masking-executor-bugs risk — option (a) vs (b) vs (c); is (b) the right call and what gates it?
- Are the three sync points (primitive, String, StringBuilder) the complete set, or are there other `GETVALUE`/reconcile divergence points (object-ref identity, arrays) that should detect too?
- Is adopting the observed concrete (dropping the stale symbolic value) the right v1 behavior, or should the diverged value become a fresh symbol (havoc)? (Plan says no havoc → concrete; confirm.)
- Clean up the now-dead post-assert recovery code as part of this.

## Review round 1 + user steering — REVISED design

Round-1 review (and the user) confirmed the draft's grounding was partly wrong and the existing handling is incomplete — so we do NOT trust it:

- **Not three uniform sync points.** Only the *primitive* path (`:3631-3641`) is "assert + (dead) adopt-observed recovery." The StringBuilder assert (`:1296`) has NO recovery (falls through pushing the stale value); object-ref divergence (`:1377`) and arrays (no per-element check) are uncovered. ⇒ **v1 scopes to the primitive path only**; StringBuilder/object-ref/arrays are explicit follow-ups (each needs a *constructed* recovery, not un-commenting).
- **`SWATAssert.check(false,…)` is config-dependent, not "crash in all modes."** `useAssertions=false` → logs-and-continues (recovery live); `exitOnError=true` (default) → halt; `exitOnError=false` (tests) → throws. And today the crash / `[SWAT Exception]` log / halt is *exactly what forces the verdict to UNKNOWN* (`SVCompDriver.py` ERROR + `[SWAT Exception]` line handling). So a silent flag MUST replace that downgrade or it's a soundness regression.
- **Verdict downgrade must be SAFE→UNKNOWN.** Reusing `referenceSemanticChange` is UNSOUND (it only downgrades VIOLATION, `SVCompDriver.py:316`). The new `outOfBandChange` flag must downgrade SAFE→UNKNOWN like `symbolic_context_loss` (`:304`). The "reuse" option is dropped. (VIOLATION stays replay-witnessed → no false VIOLATION.)
- Flag wiring is ~10 sites (SymbolicTrace field/recorder/getter → DTOBuilder → TraceDTO positional ctor → explorer DataTransferObjects/ConstraintController/ConstraintService/Database/Tree → SVCompDriver downgrade). Enumerated so it's not discovered mid-impl.

**User steering — the core design change:**

1. **Configurable policy** `shadowDivergence` = `DIFFERENTIATED` (default) | `FLAG` | `CRASH`. Not gated on `exitOnError` (a test/unrelated knob); a dedicated config (read live via `Config.instance()`, not a static-init snapshot).
2. **Escape-aware differentiation** (the principled "know when out-of-band is possible"):
   - Mark a tracked `ObjectValue` `escaped` when it is passed as receiver/arg to an **unmodeled** method (conservative v1: any unmodeled call; G4a's purity whitelist later refines — a *pure* call can't mutate, so it won't mark escape).
   - At a divergence under `DIFFERENTIATED`: if attributable to an **escaped** object → legitimate out-of-band → record `outOfBandChange` + adopt observed concrete + continue; if **not escaped** → genuine executor desync → **crash** (preserve the bug-catching net the assert was for). Only the actually-diverged value is re-grounded (not full havoc; non-diverged fields keep their symbolic value).
   - `FLAG` = always graceful (production runs); `CRASH` = always strict (dev/CI).

## Round-2 resolution (converged, pending scope decision)

Reviewer verified the revised design; key correction + the landable cut:

- **Correction:** `GETVALUE_primitive` is injected after EVERY value-producing opcode (ILOAD/ARRAYLENGTH/field-read/return), not just GETFIELD. So at the `:3631` divergence there is often **no container** (e.g. a stale local copied out of an escaped object, then ILOAD'd). The escape-aware bridge only covers the **direct field-read-of-escaped-object** case; ILOAD/array/transitive cases fall to the `not-escaped → crash` path. ⇒ the v1 escape set is an **under-approximation** (the *unsafe* direction for differentiation: a legitimate-but-unmarked out-of-band change crashes). `shadowDivergence=FLAG` is the fully-sound production escape hatch (every divergence → flag → SAFE→UNKNOWN).
- **Plumbing:** option (i) thread-local, set in `visitGETFIELD` when `ref.escaped`, **cleared at the start of every `visitGETVALUE_primitive`** (mandatory — else the bit leaks onto a later non-GETFIELD divergence and masks a real executor desync), read at the `:3631` branch. Reject (iii) (partial havoc, breaks V-3 on escaped-unmutated fields).
- **Marking site:** `InvocationHandler.invoke`, inside the existing `retValue instanceof PlaceHolder && !IGNORED` unmodeled block, after `arguments.add(0, instance)` (receiver+args unified); `instanceof ObjectValue` filter; at minimum mark array elements. Conservative "mark on all unmodeled calls" is sound for the flag direction (over-flag → more UNKNOWN, never false SAFE). Transitive closure + purity-refinement deferred to G4a.
- **Production policy:** until G4a makes the escape set faithful, SV-COMP/production should default to **FLAG** (fully sound); DIFFERENTIATED is for dev/CI (catches executor desync, gracefully handles the clean GETFIELD-escaped case).

**Scope decision (for the user):** the escape-aware differentiation is landable now but partial (above); the faithful version rides with G4a, and production runs FLAG either way. So: (A) land configurable policy + new flag/SAFE-downgrade + shallow escape-aware differentiation now; or (B) land just the configurable policy (CRASH/FLAG) + flag/SAFE-downgrade now, and land escape-aware faithfully with G4a.

---

### (superseded) Open for round-2 review: plumbing the container's `escaped` status to the primitive `GETVALUE` divergence point. The divergence is detected in `visitGETVALUE_primitive`, but `escaped` lives on the *container*, known at the immediately-preceding `GETFIELD` (which pops `ref`). Options: (i) a thread-local "last field-read was from an escaped container" set by `visitGETFIELD`, read+cleared by the next `GETVALUE`; (ii) tag the pushed field value transiently; (iii) `visitGETFIELD`, when `ref.escaped`, pushes a placeholder so `GETVALUE` re-grounds (but that re-grounds even unmutated fields ⇒ partial havoc). Reviewer to assess feasibility + pick the cleanest, and to sanity-check the escape-marking site (which invoke handling, and that it covers receiver + ref args).
