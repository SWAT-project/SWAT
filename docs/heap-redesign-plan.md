# Shadow Heap Redesign — High-Level Plan

Status: design in progress (no code yet). This doc captures *what* we want, *in what order*, and *what to check along the way*. Failure-point enumeration and test design come next, in separate sections.

## Purpose

Redesign the shadow heap so it stops mis-tracking reference objects. The visible symptom is `String.toLowerCase()` returning `this` for no-op inputs, which causes the unmodeled result to be re-bound to its symbolic receiver's formula. A Phase-0 audit (below) showed this is one of a *cluster* of confirmed soundness bugs in the current identity-hash-keyed recovery cache.

## Problem (Phase-0 audit, confirmed)

The heap is `Map<System.identityHashCode, Value>` used as a *recovery cache*, not a canonical store; frames hold direct `ObjectValue` references. Confirmed latent bugs:

- **(a) Duplicate `ShadowObject` per concrete object** — `visitNEW` / `visitLDC_String` push without registering; canonical registration is incidental. **Kicker:** object `IF_ACMPEQ/NE` use `this == o2` on the *wrapper* (not the address), so a duplicate wrapper makes reference comparison **wrong** → canonical-heap is a *correctness* fix, not cleanup.
- **(b) Field-map desync on escape** — no havoc/invalidate exists; a tracked object mutated in unmodeled code keeps stale shadow fields.
- **(c) `identityHashCode` collision + no eviction** — 31-bit key, plain `HashMap`, never cleared → distinct objects merge; dead-hash reuse.
- **(d) Unregistered objects** — `NEW`/arrays/`LDC_String` register only incidentally.

Performance: the GETVALUE/heap path is very hot (probe after every ref load/field-read/ref-returning-invoke); the common path does **no** heap lookup today. ⇒ Full frame-indirection is a **non-goal** (it would add a map lookup per access). No perf baseline exists in the repo.

## Goals

- **G1 — Canonical registry + cached pointers.** One `ShadowObject` per concrete identity via find-or-create at every introduce/recover site; a faithful, collision-free, GC-evicting key; register-on-create. Frames keep direct references as cached pointers (no per-access lookup). Fixes (a)(c)(d) + the `==` bug.
- **G_oob — Out-of-band change detection (no havoc in v1).** Detect when a tracked object's concrete state diverges from its shadow (e.g. mutated in unmodeled code) and flag it, **in all execution modes**. v1 detects only — no havoc/repair. Addresses (b).
- **G2 — Value-type boundary recovery (dealiasing model).** Each cell stores the *set* of formulas an identity has been associated with + the concrete. Recovery policy is swappable: **v1 = collapse** (single → use; multiple/none → concrete + context-loss flag); **v2 = branch over candidates** (needs explorer disjunction). Fixes the `toLowerCase` aliasing while preserving the legit single-φ round-trip.
- **G3 — Output-boundary de-interning.** De-intern value-typed method *returns* (not just literals) to shrink candidate-set sizes and remove this-return aliasing at instrumented call sites.
- **G4 — Purity whitelist + UF (feature).**
  - **G4a** — a whitelist of stateless/deterministic library methods, used to (i) skip escape-havoc for pure calls and (ii) refine leak / context-loss detection.
  - **G4b** — apply an (uninterpreted) function model for whitelisted pure methods at the boundary → a single precise, relational formula instead of concrete.

**Removed from scope:** havoc as a recovery tier (boundary recovery is {modeled / UF-if-whitelisted / concrete} only); and havoc-on-escape (v1 *detects* out-of-band changes, does not havoc/repair); and branching over aliased φs (v1 realizes/records them but recovers concrete).

### Methods-to-model policy

Default for an unmodeled method = concrete + context-loss flag, **documented as missing — not modeled.** We model a method only when a test needs its deeper semantics. Committed methods-to-model list (grows only as deeper tests demand):
- `java.lang.String.<init>(String)` — the copy constructor (test V-5): a content copy must keep the source's symbolic value.

## Order of work + per-phase checks

| Phase | Work | Checks before moving on |
|---|---|---|
| 0 ✓ | Audit current implicit handling | 4 bugs confirmed; perf characterized |
| 1 | **G1** canonical registry, faithful key, register-on-create | one wrapper per identity (assert); object `==` correctness; **establish perf baseline + measure delta**; existing suite green |
| 2 | **G2** cell stores φ-set; **v1 collapse** recovery | `toLowerCase` result no longer aliases receiver φ; single-φ round-trip still recovers; context-loss flagged on collapse; **log ambiguity frequency + candidates** (feeds v2 go/no-go) |
| 3 | **G_oob** out-of-band change detection (no havoc) | mutation in unmodeled code → divergence detected + flagged on next read, all modes; pure call → no false detection |
| 4 | **G4a** purity whitelist → wire into escape + leak/context-loss | pure calls don't over-flag / don't trigger escape-havoc; impure-with-symbolic arms contamination |
| 5 | **G3** output-boundary de-interning | candidate-set sizes shrink (measure); monitor `reference_semantic_change` cost |
| 6 | **G4b** UF application for whitelisted methods | whitelisted method recovers precise UF formula; UF-soundness tests (agreement / no-over-constraint / partiality) |
| v2 | explorer disjunction → branch-over-candidate recovery | decided by Phase-2 ambiguity measurements |

## Cross-cutting checks

- **Perf:** establish a per-instruction overhead baseline at Phase 1 (none exists); gate each phase on no material regression.
- **Soundness invariant:** every reachable shadow value's concrete component matches reality and its formula soundly abstracts it (over-approximate where flagged); context-loss is flagged whenever recovery is non-exact.
- **Faithful-key impl** (weak `IdentityHashMap` vs injected shadow-id field vs JVMTI tag): decided **within this PR**; default to weak `IdentityHashMap` unless perf dictates otherwise.

## Notation (shared vocabulary)

- `v = (c(v), φ(v))` — shadow value: concrete + formula (`φ = ⊥` ⇒ concrete-only).
- `id(r)` — concrete object identity (faithful key, not the 31-bit hash).
- `H : Id ⇀ Cell` — canonical heap. Cell content: stateful object = a `ShadowObject` (field map); value type = the φ-set + concrete.
- "structural" recovery = value still in stack/locals/field-map (exact, no heap); "boundary" = mirrored invoke of an unmodeled method; "re-entry" = a reference returns from unmodeled code as a placeholder.

## Open questions

- Faithful-key implementation choice (in-PR decision).
- De-intern aggressiveness vs the `reference_semantic_change` soundness cost.
- Whether/when to build v2 candidate-branching (informed by Phase-2 measurements).

## Next

1. Enumerate all failure points & possible failure points (the case taxonomy: object cases O1–O5, value cases V1–V7, escape/boundary cases M1–M9) and map each to a phase.
2. Design tests against the finalized notation (incl. the UF-soundness category).
