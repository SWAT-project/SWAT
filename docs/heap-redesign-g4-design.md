# G4 — Purity whitelist + generic UF for unmodeled pure returns: Problem & Proposed Solution

Phase G4 (G4a whitelist + G4b generic UF, designed as one phase). **Status: G4-full, converged after 2 design-review rounds; awaiting user sign-off.** Builds on G1 (`67505c4`) + G2 (`e84b46e`) + G_oob (`5b35699`).

## Round 2 — CONVERGED (exemption validated) + must-fixes

Reviewer verified the `precision_loss` exemption is sound: (a) pruning — UNSAT-under-free-UF ⇒ real UNSAT ⇒ SAFE sound; (b) coverage — branch *enumeration* is concrete-driven (`SymbolicTraceHandler.checkAndSetBranch` records the real direction), so an ungrounded UF can't hide a reachable error branch, only affect flip-feasibility (where it's sound); (c) VIOLATION stays replay-witnessed. Scoping correct (bespoke UFs non-exempt; a `pure_` over a non-input var still downgrades — transitivity confirmed: `FormulaCreator.VariableAndUFExtractor.visitFunction` returns CONTINUE, descending into UF args).

**Must-fixes (mechanical, fold into implementation):**
1. **Implement the exemption with a `FormulaVisitor` via `fmgr.visitRecursively`, checking `functionDeclaration.getKind() == FunctionDeclarationKind.UF` — NOT the keys-only `extractVariablesAndUFs` map.** The flat name→formula map can't distinguish a UF symbol from a variable, so the obvious implementation would be silently wrong. One pass collects `(name, isUF)`; apply input-regex to variables, `pure_` rule to UFs.
2. **Apply the exemption at BOTH sites** — `DTOBuilder.java:84-87` and the `InterruptedException` fallback `:100-102`.
3. **Extract the precision-loss predicate into a testable method** (the predicate currently lives inline in package-private `DTOBuilder`) so the negative control (exemption-not-too-broad) is a real L1 unit test; L2 (`TraceObservation` already parses `symbolicPrecisionLoss`) anchors it end-to-end.

**Note (soundness-load-bearing):** with the exemption active, **context-loss becomes the only remaining SAFE downgrade on the UF path** — so "keep context-loss flagging on the UF path" is part of the soundness story, not just hygiene. (v1 doesn't whitelist `toLowerCase`, so F-2 is untouched.) Also acceptable-but-conservative: a whitelisted result that flows through a *bespoke* UF before a branch still downgrades (the bespoke UF taints it).

## Round 3 — user steering: executor decides now, prepare the trace for a future explorer-side decision

Decision (user): keep the **executor** making the precision-loss decision for now (the `pure_` exemption stays executor-side), but **enrich the trace** so the explorer has what it needs to take over the decision later — specifically an explorer-side, **CFG-reachability-aware** precision-loss decision ("downgrade SAFE only if a precision loss is on a path that can reach an assert"). The static CFG doesn't exist yet — it's a separate block planned soon — so we design today to feed it, not build it.

Concretely (changes vs the Round-1/2 plan):
- **Per-branch classification.** Run the precision-loss classifier (the must-fix `FormulaVisitor`: input-regex for variables, `pure_` rule for UFs) **per `BranchElement`** in `DTOBuilder`, producing a per-branch boolean. The existing aggregate `symbolicPrecisionLoss` (TraceDTO) = OR of the per-branch flags → **verdict behavior is unchanged**; the explorer still downgrades on the aggregate, so the executor remains the authoritative decision-maker for now.
- **Trace contract.** `BranchDTO` gains a `precisionLoss` boolean (the executor's per-branch verdict), alongside its existing `iid`. The explorer parses it (forward-compatible) but does **not** consume it for the verdict yet. Rationale: a future CFG-aware decision keys each precision-loss branch by `iid` → CFG node → assert-reachability, with **no new trace contract** needed when that block lands.
- **Why per-branch + iid is sufficient prep:** the future decision needs only (a) *where* each precision loss occurred (the per-branch flag + `iid`, NEW) and (b) the static CFG (separate, future). Symbol-level detail, if ever needed, is recoverable from the per-branch constraint string already in `BranchDTO`. So this is minimal-yet-sufficient, not speculative schema.
- **Scope:** today only `precision_loss` gets the per-branch shape; `context_loss`/`reference_semantic_change` follow the same pattern later (same future arc).

Net G4-full deliverable: the UF mechanism (executor) + the executor-side per-branch precision-loss classifier with the `pure_` exemption (aggregate drives the verdict, unchanged) + the per-branch `BranchDTO.precisionLoss` trace field parsed by the explorer (staged for the upcoming explorer-side, CFG-aware decision) + the tiny starter whitelist.

**Scope (user decision):** purity whitelist + generic per-signature UF — the precision upgrade to G2. The escape-aware `DIFFERENTIATED` policy (deferred from G_oob) and StringBuilder/array/object divergence detectors are a *separate* follow-up that reuses this whitelist. Not G3 (output de-interning), which is unrelated.

## Problem (recap)

After G2, an unmodeled value-returning method's result is **concretized** (constant formula, non-symbolic) + context-loss-flagged — sound, but it drops the symbolic relationship to the inputs. For a method that is genuinely a *deterministic function of its inputs* (e.g. `String.toLowerCase()`), we can do better than "forget it": model the result as `result = f(inputs)` for an unknown-but-fixed `f`. That preserves the relational fact (equal inputs ⇒ equal outputs) and lets the explorer reason about / generate inputs through the call, without us knowing `f`'s definition.

This is **not** the bespoke axiomatized UFs (`ToLowerCaseUF` etc.) — those live inside symbolic *models* and the heap redesign doesn't touch them. G4 is a **generic** uninterpreted function: one fresh UF symbol *per method signature*, applied to the call's symbolic inputs, with **no hand axioms**.

## Why it's sound

A generic uninterpreted function is a valid over-approximation of *any* deterministic function: it asserts only `inputs₁ = inputs₂ ⇒ f(inputs₁) = f(inputs₂)` (referential transparency). So for a method that is a deterministic, side-effect-free function of its captured inputs, `result = UF_sig(inputs)` is sound **by construction** — there are no axioms to get wrong. The soundness precondition collapses to: **the whitelist contains only genuinely pure + deterministic methods** (the G4a job). (Concrete grounding still holds: the result's concrete = the real observed value; the UF is uninterpreted, so nothing forces `UF(concrete-inputs) = concrete-result`, and the solver's choices are replay-validated — no false verdicts.)

## Proposed solution

**Architecture (A): build the UF formula where the inputs are (InvocationHandler), materialize the value where the concrete is (GETVALUE).**

1. **Purity whitelist** (`Util` or a dedicated class): a set of method signatures (`owner/name:desc`) known to be pure + deterministic. Starter set kept *small* (the motivating cases, e.g. `java/lang/String.toLowerCase:()...`, `toUpperCase`, `trim`, `strip`), config-extensible. Default policy is "model only what a test needs" — most methods stay concretized (G2).
2. **At `InvocationHandler.invoke`** (the G2 tag site, where receiver+args are in hand): if the method is unmodeled (`retValue == PlaceHolder.instance`) **and** whitelisted-pure **and** ≥1 input is symbolic, build `Formula uf = ufmgr.callUF(declareUF(sig, returnType, argTypes…), inputFormulas…)` — argTypes from the input `Value`s' formula sorts, returnType from `Type.getReturnType(desc)`. Carry `uf` on the `UNMODELED_RETURN` placeholder (new optional `Formula recoveredFormula` field). Declarations cached per signature (a small registry, e.g. on `UFHandler`). If all inputs are concrete → no UF (fall to G2 concretize).
3. **At the `GETVALUE` UNMODELED_RETURN branch** (extends G2's concretize): if the placeholder carries a UF formula → materialize the result `Value` with `concrete = inst.val` (observed) and `formula = uf` (via the formula-taking ctor for the result type, e.g. `new StringValue(ctx, s, ufFormula, addr)`); else → G2 concretize (constant). No divergence check (built fresh from the observed concrete).

**v1 scope:** String-returning whitelisted methods first (the motivating `toLowerCase`/`trim` cases; aligns with V-1). The mechanism generalizes to boxed/primitive returns (extend the result-type → FormulaType mapping + materialization); those are fast-follows, not v1.

## Test interaction (expected)

Whitelisting `toLowerCase` changes **V-1**: today V-1 asserts the result is `disjoint` from the receiver's variables (the G2 concretize outcome). The UF outcome `UF_toLowerCase(s)` legitimately *depends on* `s` (just isn't *aliased* — `formula ≠ s.formula`). So V-1's assertion moves from "disjoint" to "**depends on `s` but is not `s`'s formula**", and a new **U-5** (modeled-result precision: a whitelisted pure call's result carries a UF over the input, equal inputs ⇒ equal results) is added. If `toLowerCase` is *not* in the v1 starter whitelist, V-1 stays as-is and a different whitelisted method drives the U tests — decide during impl.

## Acceptance tests

- **U-5 (precision):** a whitelisted pure unmodeled method with a symbolic input → result depends on the input (its var set ⊇ the input's), is not concrete, and not aliased.
- **U-4 (relational/determinism):** the same whitelisted method applied twice to equal symbolic inputs ⇒ equal result formulas (same UF, same args). (Via `extractVariables` / SAT-agreement, not formula sorts.)
- **U-soundness (no over-constraint):** the UF adds no constraint that excludes a real behavior (it's axiom-free; assert the PC stays SAT for inputs the real method admits).
- Stay green: V-2, V-3, F-1/F-2, V-5/V-6/V-9, O-4/O-5, G_oob E-1/E-2, processor specs, L2 anchor. V-1 updated (above).

## Risks / open questions for review

- **Architecture (A):** is building the UF at `InvocationHandler` and materializing at `GETVALUE` correct — is `inst.val` (concrete) reliably present at the recovery for a whitelisted return, and is carrying a `Formula` on `PlaceHolder` clean? Any path where the carried formula's sort won't match the materialized value's type?
- **Generic UF construction:** does `UFManager.declareUF`/`callUF` support heterogeneous arg sorts (String + bitvector + …)? Correct per-signature **caching** (re-`declareUF` with the same name — safe, or must cache the `FunctionDeclaration`?). Deriving arg `FormulaType`s from input `Value`s and the return `FormulaType` from the descriptor — complete for String (v1) and the extension types?
- **Determinism precondition:** the whitelist must exclude methods that are pure-but-nondeterministic across runs/JVMs (locale-dependent case mapping, `hashCode`, iteration order, identity). How do we vet entries? (Conservative: tiny, hand-audited starter set; document the bar.)
- **Symbolic-input gate:** only build a UF when ≥1 input is symbolic — confirm concrete-only inputs correctly fall to G2 concretize (a UF over constants is pointless).
- **Soundness of concrete vs UF:** the materialized value has `concrete = observed` and `formula = UF(inputs)`, with no constraint tying them. Confirm this can't yield a false verdict (it shouldn't — UF is uninterpreted, solver models are replay-validated; same argument as G2/G_oob).
- **V-1 interaction:** is updating V-1's assertion (disjoint → depends-but-not-aliased) the right call, and should `toLowerCase` be in the v1 whitelist (forcing that change) or deferred?
- **Whitelist home/form:** `Util` set vs dedicated class; signature format; config-extensibility — what's cleanest and consistent with existing config patterns.

## Review round 1 + decision: G4-FULL (UF + precision-preserving exemption)

Decision: **G4-full** — the generic UF *and* a `precision_loss` exemption so SAFE is no longer downgraded through whitelisted pure calls. (G4-minimal would only steer input-generation; SAFE stays downgraded.)

**Key review finding (B1, verified):** `DTOBuilder.java:84-87` sets `symbolic_precision_loss` if any branch-constraint symbol fails the input-var regex `[A-Z].*_[0-9].*`; `SVCompDriver.py:308-310` downgrades SAFE→UNKNOWN on it. A generic UF symbol fails the regex ⇒ SAFE always downgrades ⇒ a UF alone gives **no** verdict gain over G2. G4-full adds the exemption.

**The exemption (the soundness-critical, new part):** change the `precision_loss` test (`DTOBuilder.java:84-87` and the analogous `:100-102`) from "all symbols match the input regex" to: **fire precision_loss unless every *variable* matches the input regex AND every *UF* is a `pure_`-namespaced generic UF.** Rationale: an axiom-free UF over real inputs is a sound over-approximation of any deterministic function — if a path is UNSAT with the UF free, the real function (one interpretation) is also UNSAT ⇒ SAFE sound. Crucially: **the bespoke axiomatized UFs (ToLowerCaseUF/EqualsIgnoreCaseUF/SinCosUF) stay NON-exempt** (their partial axioms' soundness is uncertain — the "alpha" ones), and any non-input *variable* still downgrades. `extractVariablesAndUFs` already surfaces nested vars, so a `pure_` over a non-input variable still downgrades (transitivity handled). Must distinguish UF-vs-variable in the extracted map.

**Review fixes folded in:**
- **Do NOT whitelist `toLowerCase` in v1** — it's locale-dependent (Turkish-i) AND whitelisting it breaks V-2 and F-2 (both exercise `toLowerCase`), not just V-1. Lead with **`trim`/`strip`/`Math.abs`** (locale-independent, deterministic); leave V-1/V-2/F-1/F-2 untouched; drive U-4/U-5 with those.
- **Keep context-loss flagging on the UF path** — the UF is additive precision, never a license to remove a flag.
- **Value-typed receivers/args only** — `UF(receiver.formula, args)` is sound only when the result is a function of value-typed inputs; forbid whitelisting instance methods on stateful (non-value) receivers (result could depend on mutable fields not captured).
- **Descriptive, self-documenting UF names** `pure_<Class>_<method>[_<argTypes>]` ([A-Za-z0-9_] only) — e.g. `pure_String_trim`, `pure_Math_abs_int`, `pure_String_substring_int_int`. Reads as "the pure-function model of String.trim"; arg types disambiguate overloads; survives SMT dump/re-parse; cannot collide with the bespoke `toLowerCase`/etc. names. The `pure_` prefix is the exemption's recognizer (no internal phase jargon). Whitelist is curated (java.lang), so simple class names don't collide.
- **Cache per full signature** (`owner/name:desc`) on `UFHandler` (per-thread/solver-context); assert arg/return `FormulaType`s match on reuse.
- **Derive arg sorts via `fmgr.getFormulaType(formula)`** (not the descriptor) so they match the value's actual sort (avoids the Integer-theory-vs-Bitvector mismatch).
- **Gate on ≥1 symbolic input** via the existing `containsSymbolicArgument` — but read args BEFORE `arguments.add(0, instance)` mutates the list (N1).
- **Primitive-return path** (`visitGETVALUE_primitive`) intentionally ignores any carried UF formula in v1 (String returns only); assert/note it so it's not mistaken for a bug.

**Whitelist construction:** hand-audited tiny starter (trim/strip/Math.abs) to prove the mechanism + exemption soundness; then an agent survey of String/Integer/Long/Short/Byte/Character/Boolean/Float/Double/Math/StrictMath (hazard checklist: nondeterministic, env/property readers, locale-no-arg, arg-mutating, identity/intern, default Object) to populate the broad list. Format `owner/name:desc`, config-extensible.

**New acceptance test (G4-full):** a whitelisted pure call whose result feeds a branch ⇒ SAFE is **not** downgraded (precision_loss NOT set) when only `pure_`+input symbols appear; and a control where a non-input variable / non-`pure_` UF in a branch **still** sets precision_loss (the exemption isn't too broad). Plus U-4/U-5/U-soundness.

## Round 4 — naming + cross-run observed-pair aggregation (user steering)

**Naming (done above):** `pure_<Class>_<method>[_<argTypes>]`, prefix `pure_` is the exemption recognizer. No `g4uf_` jargon.

**Cross-run observed-pair aggregation (the thing that gives the UF teeth).** A bare generic UF is fully *free* (only the relational `equal-inputs⇒equal-outputs` fact). Each concolic run, however, observes a concrete `(inputs → output)` pair for a `pure_` call — ground truth about the real function. Asserting `pure_String_trim("  hi ") == "hi"` only *tightens* the over-approximation (the constraint is true of the real function), so it's **sound** (UNSAT-under-tightened ⇒ real UNSAT ⇒ SAFE still holds) and strictly more precise. Accumulating these across all runs of a testcase turns each UF into a growing partial lookup-table of observed behavior — better input generation and tighter reasoning.

**Where/how — reuses the existing UF-constraint plumbing (no new accumulation infra):**
- The bespoke UFs already ship their *defining constraints* via `symbolicTrace.getConstraints()` → `UFDTO` (`DTOBuilder.java:68-71`), and the explorer already accumulates UF definitions **per-testcase** in a `Set` (`Tree.ufs` + `Tree.record_ufs`, `Database.add_trace`). So a `pure_` UF emits its observed pair the same way: at the UF materialization, `addConstraint(fmgr.equal(callUF(decl, constant(concrete_in…)), constant(concrete_out)))` — a ground fact over *constant* inputs (NOT the symbolic input; that would be wrong). It travels via `UFDTO`, dedups into `Tree.ufs` across runs, and is injected when solving (verify the injection path `SolverHandler`/`ConstraintManager` reads `Tree.ufs`).
- Capture point: at `InvocationHandler` we have the concrete inputs (input `Value.concrete`) + the `decl`; at `GETVALUE` we have the concrete output (`inst.val`). So carry the constant-input UF application (or the concrete inputs + decl) on the placeholder and complete `== constant(out)` at materialization.
- Scope: per-testcase accumulation (matches "for a single testcase each UF gets more constraints"); the function is deterministic so pairs are valid, but per-testcase keeps it simple and bounded.

**Sequencing (two commits within G4):**
- **G4 step 1 (core):** UF mechanism (free UF) + the `pure_` exemption + per-branch precision-loss trace + tiny whitelist + U-4/U-5/U-soundness + exemption controls. Proves the mechanism + soundness + SAFE-precision. The UF is free here.
- **G4 step 2 (aggregation):** emit observed `(in→out)` pairs as `pure_` UF constraints; verify per-testcase accumulation + solver injection; tests that the constraint appears and accumulates. Makes the UF informative.
- **Then:** the `java.lang` whitelist survey agents to scale the list.

Each step runs the loop: implement → 3 independent reviews → commit.

## Step 2 — implementation design (for review)

**Status: G4 step 1 committed (48763bc) + L2 anchor (252ec5d). Step 2 EXECUTOR side implemented + committed; explorer side documented for the upcoming rework in `docs/heap-redesign-g4-step2-explorer-handoff.md` (user decision: prepare the executor, document the explorer work, move on to the whitelist survey).**

Goal: give the free generic UF teeth by accumulating observed `(input -> output)` ground facts across a testcase's runs. A run that calls `String.trim()` on concrete `"abc "` observing `"abc"` is ground truth: `pure_String_trim("abc ") == "abc"`. Asserting it only *tightens* the axiom-free UF (a true fact about the real function), so it stays sound (UNSAT-under-tightened => real UNSAT => SAFE holds; concrete-grounded => no false VIOLATION) while making the UF informative for input generation + reasoning.

**Emission (executor).** The concrete inputs are known at `InvocationHandler` but the concrete output only arrives at `GETVALUE`. So:
- At `InvocationHandler.buildPureUF`, in addition to the result UF `pure_sig(symbolic inputs)`, build a second application over the **constant** inputs `pure_sig(makeString(input.concrete), ...)` using the SAME cached declaration (so it's the same UF symbol). Carry it on the `UNMODELED_RETURN` placeholder (new field, e.g. `observedApplication`).
- At the `GETVALUE` materialization, emit `symbolicTraceHandler.addConstraint(equal(observedApplication, makeConstant(inst.val)))`.

**Accumulation (explorer) — reuses existing plumbing.** `addConstraint`-ed constraints travel via `symbolicTrace.getConstraints() -> UFDTO` (DTOBuilder:68-71), and the explorer accumulates UF definitions per-testcase in a `Set` (`Tree.ufs` + `Tree.record_ufs`, dedup by definition string). So each run's observed pair accrues across runs for that testcase, with no new accumulation infra.

v1 scope: String returns + String inputs only (constant building = `makeString`); other arg/return types arrive with the whitelist survey.

### Round-1 review (BLOCKER) — the accumulation path doesn't exist; step 2 must build it

The reviewer verified that the "rides existing accumulation, no new infra" premise is **false**:
- **`Tree.ufs` is dead code.** It is written by `Tree.record_ufs` (Tree.py:60) and **read nowhere** — it is never injected into the solver.
- **The live injection uses `Node.ufs`, which is frozen per-node.** `StrategyService.collect_uf_definitions` (StrategyService.py:78-82) injects `node.ufs` into `path_constraints` → `Z3Handler.solve` (SolverHandler.py:407-409). But `Node.ufs` is set only in the `Node` ctor (Node.py:51); `Tree.add_recursive` never merges incoming ufs into an EXISTING node. So a run's observed pair is injected only onto branches THAT run first created — flipping a pre-existing branch sees only the pairs frozen at its creation, NOT the accumulated table. The cross-run accumulation (step 2's whole point) does not happen.

**Revised design (the rework):** step 2 must ADD explorer-side injection of the accumulated per-testcase set. Chosen fix: in `StrategyService.collect_uf_definitions`/`solve_branch`, also union the live tree's accumulated `Tree.ufs` into `path_constraints` (this finally gives `Tree.ufs` a consumer; `get_tree` returns a deepcopy, so read the set that reflects all `add_trace` calls before this solve). The executor emission half (carry const-application, emit `addConstraint(equal(...))` at GETVALUE) is unchanged and was confirmed sound.

**C1 soundness wrinkle (must handle):** at solve time, if the injected UF-constraint set is UNSAT, `SVCompDriver` treats "no SAT branch" as **SAFE** (SVCompDriver.py:271-273) → a contradictory set yields a **false SAFE** for the whole testcase. All-true (deterministic) observed pairs can never be contradictory, so the whitelist's determinism bar prevents it — but step 2 elevates a bad whitelist entry from "imprecision" to "potential false SAFE." Mitigation: a contradiction guard at the dedup/accumulation point (same UF+inputs, different output ⇒ drop/skip, don't poison) + document the elevated stakes. (Confirmed clean by review: #2 getConstraints→UFDTO, #3 same cached decl for symbolic+constant applications, #4 ground-fact-only-tightens, #6 carry one const-application Formula on PlaceHolder, #7 emit at GETVALUE / inst.val faithful.)

**Tests:** executor-side emission at L1 (assert the pair is in `getConstraints()`); plus an explorer/L3 test that an accumulated pair from an EARLIER run actually constrains a LATER flip — the current L2 anchor would pass even if injection were inert, so it does not cover this.

### Round-2 review — design converged; step 2 is materially bigger than "ride existing plumbing"

Round-2 validated the revised injection approach (no staleness: the `select_branch` deepcopy reflects all prior `record_ufs`; format/idempotence clean; `collect_uf_definitions` is the universal chokepoint) but surfaced an implementation hole + sharpened the guard. Net: step 2 now spans the executor AND the explorer solve path AND verdict soundness. Four must-fixes:
1. **Injection wiring.** `collect_uf_definitions(node)` cannot reach `Tree.ufs` (Node has no tree ref; `endpoint_id` is None for 4/5 drivers). Thread the accumulated set through `select_branch` → `solve_branch` → `collect_uf_definitions` (select_branch already holds the post-`add_trace` deepcopy). NOT via `get_tree(endpoint_id)`. (StrategyService.py:16-23,78-96)
2. **C1 contradiction guard (structural, not string-Set identity).** In `Tree.record_ufs`, parse each `pure_*` observed pair, key on `(uf-name, args) → rhs`, drop on conflicting rhs. New code in the accumulation path; narrow to `pure_` names. (Tree.py:52-60)
3. **C1 solve-time backstop (verdict soundness).** Before any UNSAT→SAFE conclusion (SVCompDriver.py:271-273), if the accumulated `pure_` facts ALONE are UNSAT, downgrade SAFE→UNKNOWN. Converts a worst-case false SAFE (bad whitelist entry) into a conservative UNKNOWN. ~3 lines, gated on `pure_` ufs present.
4. **L3 explorer test.** Drive the explorer directly (`Database.add_trace` + `select_branch` + `solve_branch`), assert an accumulated pair from an EARLIER run flips a LATER solve SAT→UNSAT (reset Database + `clear_constraint_cache` between cases). The L2 anchor would pass even if injection were inert.

Confirmed clean: executor emission half (carry const-application, emit at GETVALUE, same cached decl); format match; idempotent double-add. Nit: the per-node `node.ufs` walk becomes redundant once the union lands.

**Scope observation (for the user):** must-fixes 1 + 3 are explorer **solve-path / verdict-logic** changes — they overlap the explorer rework (static CFG + explorer-side decisions) you said is coming soon, and #3 changes the SAFE/UNKNOWN logic. Step 1 already delivers G4's core (SAFE preserved through pure calls); cross-run accumulation is a precision/input-generation enhancement on top, not core soundness. So there's a real choice about whether to build the explorer machinery now or coordinate it with that rework.

Original open questions (now answered):
1. **Injection (critical) — ANSWERED: does NOT exist for accumulated facts; must be built (must-fix 1 above).**
2. **Carry design.** Is carrying the pre-built const-application (one `Formula`) cleaner/sounder than carrying the concrete inputs + decl and building at GETVALUE? Any hazard adding a second `Formula` field to `PlaceHolder`?
3. **Same-symbol sharing.** The result UF (over symbolic inputs) and the observed-pair UF (over constants) must be the SAME declaration/symbol (else the observed facts don't constrain the symbolic result). Confirm `PureFunctionUF.apply` returns the cached decl for both calls.
4. **Soundness.** Re-confirm asserting `pure_sig(const_in) == const_out` only tightens (never excludes a real behavior), and that a wrong/duplicate emission can't cause a false verdict. Dedup correctness across runs (definition-string identity).
5. **Determinism caveat.** If a whitelisted method were non-deterministic across runs, two runs could assert `f(x)==a` and `f(x)==b` with a!=b -> UNSAT (poisoning the whole testcase). The whitelist's determinism bar prevents this; flag that step 2 raises the stakes on it (a non-deterministic entry now corrupts solving, not just precision).
6. **Testing.** Executor-side: assert the observed-pair constraint is emitted (in `symbolicTrace.getConstraints()`); the cross-run accumulation itself is explorer/L3. Right altitude?

## Round 5 — step-1 post-implementation review fixes (two real blockers)

The first 3-review pass found step 1, as first implemented, did NOT actually deliver SAFE precision, for two reasons (both fixed):

- **Issue A — input detection was wrong for String/array inputs (also pre-existing).** The precision-loss "is this a real input?" test used only the regex `[A-Z].*_[0-9].*`. Real input names: primitives are `I_0` (match), but **String inputs are `java/lang/String_0`** and arrays `[I_0` (don't match). So (pre-existing) String/array-input branches always tripped precision_loss → SAFE always downgraded; and G4's exemption could never fire (the String input var itself failed the regex). **Fix:** detect inputs by exact **term identity** against the designated inputs (`symbolicTrace.getInputs() → value.formula`, collected into a `Set<Formula>`; JavaSMT formulas have value-based equals/hashCode) — correct for all types, no name pattern. The regex is **kept as an additive backstop** for symbolic variables that are grounded but NOT designated inputs — specifically values re-materialized by GETVALUE heap recovery, which call `MAKE_SYMBOLIC` with a fresh `I_n`-style name without registering an input (verified at SymbolicInstructionVisitor:1301,1318). Pure-term-only would conservatively (soundly) downgrade those → regression; the backstop avoids it. Net: term-identity is primary (fixes String/array designated inputs + the exemption); regex is a backstop. (Dropping the backstop later is safe once recovery-created vars are handled.)
- **Issue B — context-loss independently downgraded SAFE on the UF path, making the exemption pointless.** A whitelisted pure unmodeled call recorded context-loss (→ SAFE→UNKNOWN) regardless of the exemption. **Fix (supersedes the Round-2 "keep context-loss" note, which was self-defeating):** when a whitelisted pure call is **successfully modeled as a UF**, do NOT record context-loss for it. Sound because the whitelist guarantees purity/side-effect-freedom (nothing changed but the return) and the return is captured by the UF — so no context is actually lost. Non-whitelisted unmodeled calls (e.g. `toLowerCase`) still flag context-loss exactly as before (F-2 + the L2 anchor confirm). Both fixes rest entirely on whitelist correctness (purity), the standing G4 soundness precondition.

Tests added/updated: PrecisionLossExemptionSpec now uses realistic `java/lang/String_0` input terms + passes the input set (5 cases); PureFunctionUFSpec adds U-6 (a whitelisted pure call does NOT flag context-loss). All green: 42 heap+processor, 2 L2 (anchor still flags `toLowerCase`), 0 fail. Plus style nits (redundant import, em-dash, import order). Re-review (3 agents) then commit.
