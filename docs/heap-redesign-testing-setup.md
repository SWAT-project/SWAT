# Shadow Heap Redesign — Testing Setup Analysis

Initial analysis of *how* to test the behavioral cases in `heap-redesign-tests.md` (the *what*). Produced as a starting point; the human will refine/develop tests from here. **Tests are pre-development (red-first/TDD): many SHOULD fail against current code — that is the expected signal that the fix is pending.**

## Environment (verified)

- Current branch is **`dev`**. `stats.json` (consolidated per-testcase stats) exists **only on `feat/svcomp-testcase-metadata`**, not `dev`. ⇒ For in-process tests, observe soundness flags directly off `SymbolicTrace`/`TraceDTO` rather than depending on `stats.json`.
- Z3 native libs present (`libs/java-library-path/`); `copyNativeLibs` already run.
- `sv-benchmarks` is checked out (R-1 candidates: `securibench/.../sanitizers/Sanitizers5.java`, `autostub/String_..._toLowerCase`).
- Tests = **Spock/Groovy only** (`symbolic-executor/src/test/groovy`, 15 specs). CI runs exactly `./gradlew copyNativeLibs` then `./gradlew test`. The `build` job uses `build -x test`. Avoid `spotless*`.
- Test JVM sets `exitOnError=false` (SWAT errors surface as catchable exceptions, not JVM halt) — `symbolic-executor/build.gradle:49-52`.

## Robust model to emulate / anti-pattern to avoid

- **Emulate:** the processor/shadow-state specs (`symbolic/processor/*Spec.groovy` + `BaseSymbolicInstructionProcessorSpec.groovy`). They drive the REAL `SymbolicInstructionProcessor.processInstruction()` over a constructed `ShadowContext` and assert on `Frame.operandStack`/`locals`/`ret`, `Value.concrete`, and symbolic VARIABLE NAMES via `extractVariables(formula).keySet()` (`InternalInvocationSpec.groovy:68`). `setupTestContext()` (`:54`) uniques method names per test — new specs MUST use it.
- **Avoid:** `de/uzl/its/value/**` (~1670 rows) assert on `.formula` via sort-specific managers (`bvmgr.equal`) → break en masse on representation churn.
- **Oracle rule:** assert on `Value.concrete`, `isSymbolic()`, variable *names*, boolean soundness flags, `Frame` contents, `IF_ACMPEQ`/`equals` booleans — NEVER on formula SMT sorts.

## Where the bugs live (code under test)

- Recovery cache `shadow/JVMHeap.java` — bare `HashMap<Integer,Value>`, only `put/get(hashCode)`, **no map/size/iteration getter, ZERO tests**. Via `ShadowContext.putToHeap/getFromHeap` (`:48-54`).
- **V-1 core bug path:** `SymbolicInstructionVisitor.visitGETVALUE_Object` (`:1265-1340`) — unmodeled result returns as `PlaceHolder`; `tmp = stack.getFromHeap(inst.address)` (`:1270`); if non-null, pushes the cached `Value` (`:1284`). `invokeToLowerCase` returns `PlaceHolder.instance` (`StringValue.java:1125`); `toLowerCase()` returns `this` for already-lowercase input ⇒ result shares receiver identity ⇒ recovers receiver's `StringValue` ⇒ aliases `s`.
- **`==` wrapper bug (O-4):** `ObjectValue.IF_ACMPEQ` uses `this == o2` on the wrapper (`~:153`); `equals()` compares `address == other.address && fields.length == other.fields.length` (`:131`). Duplicate wrappers ⇒ `IF_ACMPEQ` wrongly false; `equals` comparing only `fields.length` is a latent O-5 smell.
- **Soundness flags:** `trace/SymbolicTrace.java:26,29` booleans `symbolicContextLoss`/`referenceSemanticChange`; set via `SymbolicTraceHandler.recordSymbolicContextLoss()` (`:206`)/`recordReferenceSemanticChange()` (`:216`); mirrored on `TraceDTO.java:16,20`.
- **Flag-decision logic (F-1..F-4):** `invoke/InvocationHandler.invoke()` (`:38-121`) — `containsSymbolicArgument` (`:53-63`); records context-loss only when result is unmodeled placeholder AND a symbolic arg present (`:107-117`); missing invocations via `recordMissingInvocation` (`:99`).
- **E2E verdict:** scraped from STDOUT `[VERDICT <category>]` (`target_execution.py:116`).

## Recommended architecture — three levels (stay on Spock for A/B)

- **Level A — pure unit** (build `ObjectValue`/`StringValue`/heap directly). Cases: O-4, O-5, D-3, V-5, V-6, V-9, the canonical "one wrapper per identity" invariant.
- **Level B — processor-driven shadow-state spec** (the workhorse). Cases: V-1, V-2, V-3, V-4, V-7, V-8, O-1, O-2, O-3, O-6, F-1..F-4, U-5, D-1. Richest source of expected-reds.
- **Level C — whole-program E2E** (NOT in CI; STDOUT-scraping today). Cases: R-1, R-2, D-2, E-1's "all modes" clause.

## New infrastructure to build (in order of blocking-ness)

1. **Heap/registry inspection seam (blocks G1 tests).** `JVMHeap`/the new registry needs size/iteration/identity-count. Recommendation: define on the NEW registry API; write O-tests as expected-reds against it so they go green when Phase 1 lands.
2. **Boundary-recovery fixture (highest leverage).** Generalize `executeLiftInsnSeq` (`BaseSymbolicInstructionProcessorSpec.groovy:214`) to the object/recovery path: push a (symbolic) receiver, run unmodeled-invoke → `INVOKEMETHOD_END` → `GETVALUE_Object` for a given concrete result + address, return recovered `Value` + flag snapshot. Reused by V-1/V-2/V-3/V-4/O-2/O-3/F-3.
3. **Thin programmatic E2E harness (only for R/D-2/E-1-all-modes).** Run one target through agent+explorer, return `{verdict, contextLoss, referenceSemanticChange}` structured. Heaviest lift; possibly Python; not CI-gated.

## Per-case mapping + expected status NOW

| Case | Level | New infra | Status now |
|---|---|---|---|
| O-1 aliasing/field write | B | fixture | likely RED |
| O-2 re-entry same object | B | fixture | RED |
| O-3 born-in-unmodeled | B | heap getter + fixture | RED |
| O-4 ref-equality | A | — | **RED** (`this==o2`) |
| O-5 hash collision | A/B | heap getter | RED |
| O-6 identity reuse after death | B | heap getter (eviction) | RED |
| V-1 this-return no-alias (core) | B | fixture | **RED** |
| V-2 new-object consistency | B | fixture | RED |
| V-3 single-φ round-trip | B | fixture | possibly GREEN (verify) |
| V-4 conflicting φ → concrete | B | φ-set (Phase 2) | RED |
| V-5 `new String(String)` keeps φ | A | — | likely **GREEN** (`invokeInit` copies, `StringValue.java:218`) |
| V-6 interned literal reuse | A | — | verify (likely GREEN) |
| V-7 boxed analogue | B | fixture | RED |
| V-8 primitive context loss | B | fixture | partially GREEN |
| V-9 concrete grounding | A/B | — | GREEN |
| E-1 out-of-band detect (all modes) | C (+B) | E2E harness | RED |
| E-2 pure call no false flag | B | purity whitelist (Phase 4) | N/A yet |
| F-1 no symbolic → no flag | B | — | likely GREEN |
| F-2 symbolic → flag | B | — | likely GREEN |
| F-3 leak-then-retrieve | B | fixture | RED (flags at call, not retrieval) |
| F-4 pure symbolic no leak | B | purity whitelist | N/A yet |
| D-1 fresh identity on produced | A/B | — | RED (Phase 5) |
| D-2 de-intern verdict-neutral | C | E2E harness | needs E2E |
| D-3 ref-semantic-change only-when-warranted | A | — | verify |
| U-1..U-4 UF soundness | A/B | UF (Phase 6) | N/A yet |
| U-5 modeled-result precision | B | whitelist+UF | N/A yet |
| R-1 original failing case | C | E2E harness | **RED** |
| R-2 golden no-regression | C | E2E harness + golden set | baseline-dependent |

Tag scheme: `@Tag` / naming `expected-red` vs `expected-green` + `phase-N`. Make expected-red tests fail on a SPECIFIC assertion (e.g. "result must not contain var X"), never on a setup exception — so expected-red is distinguishable from infra-broken.

## Decisions for the human (ordered)

1. Heap-inspection seam: add to legacy `JVMHeap` now, or define on the new registry API (recommended → write O-tests as expected-reds against it).
2. Build the boundary-recovery fixture before V-*/O-2/O-3/F-3 (shared by all).
3. E2E scope: build a structured harness vs defer Level C to a non-CI regression lane (recommended: keep CI to A/B).
4. Branch: observe flags in-process off `TraceDTO`/`SymbolicTrace` to avoid the `stats.json`/branch dependency (recommended).
5. Phase gating: author in-process expected-reds now (tagged by phase) vs defer until each phase begins (recommended: author now).

Suggested start: heap/registry inspection seam → boundary-recovery fixture → V-1 as the first concrete expected-red.
