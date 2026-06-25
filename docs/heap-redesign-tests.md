# Shadow Heap Redesign — Test Specification (behavioral)

Harness-agnostic. Each case is **a scenario + the property that must hold** — *what* we want to verify, not *how* to observe or run it. Setting up the harness/levels for these is a separate task.

Notation: a *symbolic value* carries a formula over program inputs; a *concrete value* is the observed runtime value; "shadow object" = the symbolic model of a reference object; "flag" = a soundness flag (context-loss / reference-semantic-change); "unmodeled call" = a method SWAT has no model for; "boundary" = an instrumented call site invoking unmodeled code; "re-entry" = a reference returning from unmodeled code. v1/v2 mark staged expectations.

**Policy — model only what a test needs.** The default for an unmodeled method is concrete + context-loss flag (V-1/V-8); we simply *document it as missing*. We model a method only when a test needs its deeper semantics — those go on an explicit **methods-to-model** list (in the plan doc). `new String(String)` (V-5) is the first committed entry. Most methods will stay unmodeled, and that is fine and expected.

---

## G1 — object identity / canonical heap (stateful objects)

- **O-1 Aliasing through locals/fields.** *Scenario:* one object reachable via two references; a field written through one. *Expect:* the write is visible through the other; exactly one shadow object represents the concrete object.
- **O-2 Re-entry recovers the same object.** *Scenario:* a tracked object passes into unmodeled code and the same concrete object returns. *Expect:* the recovered shadow object is the same instance (its field state preserved), not a fresh empty duplicate.
- **O-3 Object born in unmodeled code.** *Scenario:* unmodeled code returns a never-before-seen object. *Expect:* a fresh shadow object with unknown (not fabricated) fields; subsequent sightings of that same concrete object recover the same shadow object.
- **O-4 Reference-equality correctness.** *Scenario:* `==` between (i) two references to one object, (ii) two references to distinct objects — for objects born locally and objects re-entered from unmodeled code. *Expect:* (i) true, (ii) false, in all entry combinations.
- **O-5 Distinct objects sharing an identity hash.** *Scenario:* two distinct live objects with a colliding identity hash. *Expect:* distinct shadow objects, no field cross-contamination, `==` between them false.
- **O-6 Identity reuse after death.** *Scenario:* a tracked object becomes unreachable; a new object later reuses its identity hash. *Expect:* the new object does not recover the dead object's shadow.

## G2 — value-typed recovery (String + boxed wrappers + primitives)

- **V-1 `this`-return must not alias receiver (the core bug).** *Scenario:* symbolic string `s`, concretely already-lowercase, receiver of an unmodeled `toLowerCase()` that returns `this`. *Expect (v1):* the result does not carry `s`'s symbolic value (it does not depend on `s`); it is concrete; a context-loss flag is raised; its concrete value equals the real result. *(v2: may be a UF of `s` if whitelisted — see U-5.)*
- **V-2 New-object transform is consistent with V-1.** *Scenario:* same as V-1 but `s` is concretely upper-case, so the call returns a *new* object. *Expect (v1):* identical outcome to V-1 (concrete + flag, no aliasing). The this-return vs new-object distinction must not change the symbolic result.
- **V-3 Single-formula round-trip preserved.** *Scenario:* symbolic string `s` stored into unmodeled space and retrieved unchanged, only ever associated with one formula. *Expect:* recovery yields `s`'s formula (precision kept), not a collapse to concrete.
- **V-4 Conflicting formulas collapse to concrete.** *Scenario:* one identity becomes associated with two or more distinct formulas (aliased φs). *Expect:* the multiplicity is *realized/recorded* (the φs are retained, per the data model), but recovery sticks to concrete + flag — we do **not** branch over the formulas. (Branching is a possible future direction, explicitly out of scope now.)
- **V-5 Program-made copy keeps the value (committed model).** *Scenario:* `t = new String(s)` for symbolic `s`. *Expect:* `t` carries `s`'s formula (a content copy is the same value). The copy constructor is one we **commit to modeling** → it goes on the methods-to-model list. (Contrast with the general policy above: most unmodeled methods are documented-as-missing, not modeled.)
- **V-6 Interned-literal reuse.** *Scenario:* the same literal used at several sites. *Expect:* every occurrence carries the same constant formula; no spurious aliasing or conflict.
- **V-7 Boxed-primitive analogue.** *Scenario:* symbolic `Integer`/`Long`; unmodeled method on it, and reuse of an autobox-cached instance. *Expect:* same rules as strings — the box cache / a this-return never transfers a wrong formula.
- **V-8 Primitive context loss.** *Scenario:* unmodeled method returns an `int` derived from a symbolic input. *Expect (v1):* concrete + flag — neither falsely symbolic nor falsely treated as independent.
- **V-9 Concrete grounding (universal).** *Scenario:* any recovery, any case above. *Expect:* the recovered value's concrete component always equals the real runtime value.

## G_oob — out-of-band change detection (no havoc in v1)

- **E-1 Detect out-of-band mutation.** *Scenario:* a tracked mutable object is passed to unmodeled code that mutates one of its fields; the field is then read in instrumented code. *Expect (v1):* the system **detects** the divergence (the observed concrete value no longer matches the shadow's stored value) and flags it — **in all execution modes**. It does not silently trust the stale shadow. We detect, not havoc or repair.
- **E-2 Pure call triggers no false detection.** *Scenario:* a tracked object passed to a pure (whitelisted) unmodeled method (no mutation). *Expect:* no out-of-band change is detected; no false flag.

## G4a — context-loss / leak flagging precision

- **F-1 Clean state, no symbolic input → no flag.** *Scenario:* unmodeled call with no symbolic receiver/args, nothing symbolic previously leaked. *Expect:* no context-loss flag (the result is genuinely concrete).
- **F-2 Symbolic input → flag.** *Scenario:* unmodeled call with a symbolic argument whose result is used. *Expect:* context-loss flagged at the point the lost value is used.
- **F-3 Leak then retrieve.** *Scenario:* a symbolic value passed into unmodeled space via a call with no usable result (leak); later a different unmodeled call retrieves a value from that space. *Expect:* no flag at the leak; flag at the retrieval. If no retrieval ever occurs, no flag at all.
- **F-4 Pure symbolic call → no leak.** *Scenario:* a pure whitelisted call with a symbolic argument. *Expect:* no contamination is armed; unrelated later retrievals are not flagged on its account.

## G3 — output-boundary de-interning

- **D-1 Fresh identity on produced values.** *Scenario:* an unmodeled value-returning call (or literal) at an instrumented site. *Expect:* the produced value has a fresh identity distinct from the inputs, so a this-return cannot alias the receiver there; its candidate set stays a singleton.
- **D-2 De-interning is verdict-neutral.** *Scenario:* a program run with and without output de-interning. *Expect:* the same verdict (de-interning never changes the soundness of the result).
- **D-3 Reference-semantic-change only when warranted.** *Scenario:* `==` on de-interned values. *Expect:* the reference-semantic-change flag fires only when de-interned `==` actually diverges from real reference semantics, not otherwise.

## G4b — uninterpreted-function modeling + soundness

- **U-1 Agreement.** *Scenario:* a UF-modeled method on sampled concrete inputs in its axiomatized domain. *Expect:* the UF result equals the real method's result for every sample.
- **U-2 No over-constraint.** *Scenario:* any input the real method admits. *Expect:* the path condition plus the UF's axioms stays satisfiable for it — the UF never excludes a real behavior.
- **U-3 Partiality outside the domain.** *Scenario:* an input outside the axiomatized domain (e.g. non-ASCII for case folding). *Expect:* the UF is unconstrained there — a model may choose a value differing from the real one (over-approximation, not a false pin).
- **U-4 Relational consistency.** *Scenario:* the same method applied twice to equal symbolic inputs. *Expect:* equal results (determinism captured) — the property concrete recovery would lose.
- **U-5 Modeled-result precision.** *Scenario:* a whitelisted method with a symbolic input. *Expect:* the result depends symbolically on that input (carries the UF), not collapsed to concrete.

## Whole-program regression

- **R-1 Original failing case.** *Scenario:* the `toLowerCase` securibench valid-assert case. *Expect:* correct verdict with context loss flagged — never a confident wrong verdict.
- **R-2 No regressions.** *Scenario:* a set of golden whole-program cases. *Expect:* verdicts unchanged by the redesign.

---

## Coverage map (case → goal/phase)

| Goal / phase | Cases |
|---|---|
| G1 canonical heap | O-1 … O-6 |
| G2 value recovery | V-1 … V-9 |
| G_escape | E-1, E-2 |
| G4a flag policy | F-1 … F-4 |
| G3 de-interning | D-1 … D-3 |
| G4b UF + soundness | U-1 … U-5 |
| regression | R-1, R-2 |

Resolved: **V-5** — model the copy constructor (added to the methods-to-model list); general policy = document-as-missing for the rest. **E-1** — detect out-of-band changes (no havoc), in all modes. **V-4** — realize/record the multiple φs but recover concrete; no branching.

Living artifact: the **methods-to-model list** (plan doc) grows only as deeper tests demand specific semantics; the default stays document-as-missing.
