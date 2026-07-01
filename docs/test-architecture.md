# SWAT Test Architecture — Multi-Level Setup

A reusable test architecture for the `symbolic-executor` module. It defines **four
abstraction levels**, the **fixtures/seams** each needs, and the **oracle rules** that keep
tests robust against representation churn. The heap and value-tracking tests are its primary
consumer; see [`heap-tracking.md`](heap-tracking.md) for that subsystem's design.

## Why levels (the core insight)

A behavioral case is *"a program + a property."* The same property can be checked at very
different altitudes, and **they are not interchangeable** — a bug lives in a specific contract,
and only a test at that contract's altitude actually exercises it. Picking the *lowest faithful*
altitude per case gives fast, non-flaky tests; picking too low makes the test circular, too high
makes it slow and coarse.

```
L0  value & shadow-structure unit    in-JVM, no instrumentation     fast, CI
L1  shadow-interpreter / processor    in-JVM, synthetic instructions fast, CI
L2  real-instrumentation single-run   forked JVM, real agent         slower, opt-in CI
L3  end-to-end / verdict              agent + Python explorer        nightly / manual
```

## Oracle rules (apply at every level)

The legacy `de/uzl/its/value/**` suite is brittle because it asserts on `.formula` via
sort-specific managers (`bvmgr.equal`), so it breaks en masse on representation changes.
**Never do that.** Assert only on:

- `Value.concrete` (the observed runtime value);
- `Value.isSymbolic()`;
- symbolic **variable names** via `solverContext.getFormulaManager().extractVariables(f).keySet()`;
- boolean results of `IF_ACMPEQ` / `IF_ACMPNE` / `equals`;
- soundness **flags** (`symbolicContextLoss`, `symbolicPrecisionLoss`);
- `Frame.operandStack` / `locals` / `ret` contents;
- structured `TraceDTO` fields (L2/L3);
- for SMT/UF agreement only: feed the formula to a real `ProverEnvironment` and assert
  **SAT/UNSAT agreement** with the concrete result — never inspect the formula's sort.

**Expected-red mechanism (Spock `@PendingFeature`).** This Spock version (2.2-M1-groovy-4.0) has
**no `spock.lang.Tag`**. Mark each red case
`@PendingFeature(reason = "<behavior> not yet implemented; …")`: the feature runs and asserts the
*desired* behavior, is reported as **pending (skipped), not a failure** while red — and **forces a
build failure the moment it starts passing** (the unambiguous "fix landed, remove the annotation"
signal). The test level is conveyed by the package and class name.

**An expected-red test MUST fail on a specific assertion** (e.g. "result vars must not contain
`S_x`"), never on a setup exception — otherwise `@PendingFeature` would mask infra breakage as
"pending". Enforce this with the **precondition-first / guard-feature convention**: assert
currently-true preconditions first (or as a *separate non-pending* feature, e.g. "distinct objects
compare unequal"), so an infra break surfaces as a real failure while the pending feature isolates
only the not-yet-implemented behavior.

---

## L0 — Value & Shadow-Structure Unit

**Scope.** `Value` subclasses (`StringValue`, `IntValue`, boxed wrappers, `ObjectValue`,
arrays) and the shadow data structures (`JVMHeap`, `Frame`, `ShadowContext`). No instrumentation,
no instruction stream — construct objects and call methods directly.

**Tests well.** Value semantics: `IF_ACMPEQ`/`equals`, `invokeMethod`/`invokeInit` (e.g. the
`new String(String)` copy ctor, `StringValue.java:217`), `MAKE_SYMBOLIC`, heap `put/get` and key
behavior, UF-defining-constraint SMT agreement. For example, construct two distinct objects with a
colliding identity hash and assert they compare unequal, and assert that a single identity yields a
single wrapper.

**Driver.** A shared `BaseValueSpec`:
`ThreadHandler.init()` → `addThreadContext(currentThread().id, "Test-Thread", -2)` →
`getSolverContext` → expose `fmgr/bmgr/smgr/...` and a fresh `ProverEnvironment`; `cleanup`
closes the prover/context and removes the thread context. For UF agreement, pull
UF-defining constraints from the trace
(`ThreadHandler.getSymbolicTraceHandler(id).getConstraints()`) into the prover — see
`StringValueTest.addUFConstraintsFromTrace`.

**Oracle.** Per the rules above. Exemplar in repo: `StringValueTest.groovy`.

---

## L1 — Shadow-Interpreter / Processor (the workhorse)

**Scope.** `SymbolicInstructionProcessor.processInstruction()` driven over a constructed
`ShadowContext`. Feed a synthetic `Instruction` sequence; assert on how the operand stack /
locals / frame / heap / trace evolve.

**Tests well.** The reaction of the shadow interpreter to instruction sequences: lifting a
symbolic input, the **invoke → recovery** path (`INVOKEVIRTUAL`(unmodeled) → `INVOKEMETHOD_END`
→ `GETVALUE_Object`), branches, field/array ops.

**Driver.** `BaseSymbolicInstructionProcessorSpec` provides `setupTestContext` (unique method
names per test — required), the `push*Operand` helpers, and `executeLiftInsnSeq`
(introduce a symbolic input). The **boundary-recovery fixture** has the highest leverage and is
shared by all recovery cases:

```
executeBoundaryRecovery(receiver, owner, name, desc, concreteResult, resultAddress)
  // 1. register `receiver` on the heap at its address (stack.putToHeap)
  // 2. push receiver as the invoke operand
  // 3. process: INVOKEVIRTUAL(owner,name,desc) ; INVOKEMETHOD_END ;
  //             GETVALUE_Object(resultAddress, concreteResult, i)
  // 4. return { recovered: peekOperand, contextLoss, heap snapshot }
```

This drives the *real* bug path: an unmodeled invoke returns `PlaceHolder`
(`InvocationHandler.invoke`, `StringValue.invokeToLowerCase:1126`), `INVOKEMETHOD_END` pushes it
(`SymbolicInstructionVisitor:3406`), and `visitGETVALUE_Object` (`:1265`) recovers from the
identity-keyed heap — the aliasing defect.

**Honest caveat.** L1 *fabricates* the instruction stream, including the
`resultAddress == receiver.address` collision that a real `this`-return produces. So an L1
recovery test pins the **interpreter's recovery logic**, not the instrumentation→processor
contract, and bakes in an assumption about the emitted sequence. Therefore: pair a flagship
recovery case with an **L2 anchor** that gets the collision from the real JVM.

**Seams required (see Cross-cutting).** Heap-inspection view on `ShadowContext`; flag getters on
`SymbolicTraceHandler`.

**Oracle.** L0 rules + `Frame`/heap-view/flags. Exemplars: `InternalInvocationSpec`,
`INVOKEVIRTUALSpec`, `IADDSpec`.

---

## L2 — Real-Instrumentation / Single-Run (forked JVM, structured TraceDTO)

**The faithful altitude for recovery/identity bugs.** The real ASM agent runs a real tiny target
program; identity hashes, the `this`-return, the GETVALUE sequence, and the soundness flags all
come from the real JVM + instrumenter — nothing is hand-fabricated.

**Key enabler.** `solver.mode=PRINT` makes `Intrinsics.terminate()` do
`System.out.println(getTraceDTO())` (`Intrinsics.java`, terminate() PRINT branch) — the full
`TraceDTO` JSON on stdout, **no Python explorer needed**. `LOCAL` mode (default) instead solves
in-JVM with Z3 via `LocalSolver.solve()`. `SolverMode = {LOCAL, HTTP, PRINT, NONE}`.

**Driver — `AgentRunFixture`:**

1. Build the agent jar first: `./gradlew :symbolic-executor:copyJar`
   → `symbolic-executor/lib/symbolic-executor.jar` (build with `copyJar`, never `spotlessApply`).
2. Target programs live in `src/test/resources/targets/<Name>.java` (tiny, hand-written).
   Designate symbolic inputs with `Verifier.nondetInt(long id)` / `nondetString(id)` / …
   (SV_COMP transformer) — e.g. `String s = Verifier.nondetString(0);`.
3. Fork: `java -javaagent:symbolic-executor.jar -Dsolver.mode=PRINT
   -Dconfig.path=<test.cfg> -Dswat.input.<name>=<concrete> -cp <target> <Main>`
   with a minimal `test.cfg` (`instrumentation.transformer=SV_COMP`, `solver.mode=PRINT`,
   `exitOnError=false`). `swat.input.*` pins the concrete path so the run is deterministic.
4. Capture stdout, parse the TraceDTO JSON (Jackson) into a typed
   `TraceObservation { inputs[], branches[], ufs[], symbolicContextLoss,
   symbolicPrecisionLoss }`.

**Why forked, not in-process:** the agent attaches at premain and `ThreadHandler`/`Config` are
process-global singletons set at startup; resetting them mid-JVM is hacky and fragile. A forked
JVM gives clean isolation per case.

**Oracle (structured, no log-scrape).** TraceDTO fields + which input variables appear in branch
constraints. For example, plant `if (r.equals("abc"))` on an unmodeled `toLowerCase` result and
assert `symbolicContextLoss == true` **and** that the branch over `r` does **not** reference the
symbolic input variable (so no confident wrong SAFE can be derived).

**CI.** Slower + needs the jar; gate behind a separate Gradle task (`agentTest`), not the default
`test`.

---

## L3 — End-to-End / Verdict (explorer in the loop)

**Scope.** Full pipeline: agent (`HTTP` mode) + Python explorer + iterative path exploration +
final SV-COMP verdict, including the downgrade rules (SAFE+contextLoss→UNKNOWN, etc.). Tests the
*verdict*, not just the trace.

**Driver.** The sv-comp driver / `targets/` harness. Structured per-testcase observation
(`{verdict, contextLoss}`) becomes clean once the consolidated `stats.json` work lands — until
then it is STDOUT/log scraping (`[VERDICT <category>]`).

**CI.** Not gated; nightly / manual regression lane.

---

## Cross-cutting infrastructure

The durable seams that back the levels, reused well beyond any single subsystem:

1. **Flag-observation seam.** Getters on `SymbolicTraceHandler`:
   `isSymbolicContextLoss()`, `isReferenceSemanticChange()` (the `SymbolicTrace` fields are
   package-private; `processor`-package specs can't see them). Used by L1 + every flag
   assertion.
2. **Heap/registry inspection view.** A small read-only view on `ShadowContext` (every spec
   already gets it via `visitor.getStack()`, so no new wiring):
   `int heapSize()`, `int heapDistinctIdentities()`, `Value<?,?> heapLookup(long id)`,
   `Collection<Value<?,?>> heapEntries()`. Assertions phrased against this view don't change as
   the backing structure evolves; they flip red→green as the behavior is fixed (e.g. two distinct
   objects with a colliding hash → `heapDistinctIdentities()` should be 2). Chosen over raw
   `JVMHeap` getters, which would couple tests to a structure under active change, and over
   future-API-only tests, which wouldn't compile and so give no running red signal.
3. **L1 boundary-recovery fixture** (see L1). Highest leverage; shared by all recovery cases.
4. **L0 `BaseValueSpec`** — a shared prover + solver context for all value-semantics specs.
5. **L2 `AgentRunFixture` + `TraceObservation`** (see L2). Forked-JVM + PRINT-mode + JSON parse.

## Framework

**Spock for L0/L1/L2-harness** (the suite is already Spock; `where:` tables fit tabular case sets
well). L3 stays in the Python sv-comp harness.
