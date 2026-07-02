---
name: swat-test
description: Create and run tests for the SWAT symbolic-executor at the right abstraction level (L0 value-unit, L1 processor, L2 forked-agent). Use when adding a test for shadow values, the shadow interpreter, recovery/heap behavior, soundness flags, or a whole-program agent run — or when deciding which level a behavior belongs at. Covers the fixtures, the oracle rules, the @PendingFeature red convention, and the run commands.
---

# Testing SWAT (symbolic-executor)

Full rationale: `docs/test-architecture.md`. This skill is the operational recipe.

## Pick the level (lowest faithful altitude)

| Level | Use when the behavior lives in… | Driver |
|---|---|---|
| **L0** value/structure unit | a `Value` method or shadow structure (`StringValue.IF_ACMPEQ`, `ObjectValue.equals`, `JVMHeap` key, copy-ctor, UF SMT-agreement) | construct objects directly |
| **L1** processor | how the shadow interpreter reacts to an instruction stream (lift, **invoke→recovery**, branches, fields) | drive `SymbolicInstructionProcessor` |
| **L2** forked-agent | the real instrumentation→trace contract (real identity hashes, this-return, soundness flags) | run a real target under the agent |
| L3 end-to-end | the SV-COMP verdict + downgrade rules | sv-comp driver (out of current scope) |

If unsure between L1 and L2: L1 *fabricates* the instruction stream (incl. address collisions), so it pins interpreter logic, not the instrumentation contract — use L2 to anchor anything whose bug depends on real JVM identity/this-return.

## Oracle rules (every level — non-negotiable)

Assert ONLY on: `Value.concrete`, `isSymbolic()`, symbolic **variable names**
(`extractVariables(f).keySet()`), `IF_ACMPEQ`/`equals` booleans, soundness flags, `Frame`
contents, structured `TraceDTO` fields, or **SAT/UNSAT agreement** via a real prover.
**Never** assert on a formula's SMT sort/representation (that is why the ~1670 `de/uzl/its/value/**`
rows break en masse).

## Expected-red convention (`@PendingFeature`)

There is no `spock.lang.Tag` in this Spock version. For a behavior not yet implemented (red-first
TDD), annotate the feature `@PendingFeature(reason = "…")`: it runs and asserts the *desired*
behavior, is reported **pending (skipped), not a failure**, and **fails the build the moment it
starts passing** (your "fix landed" signal). Put currently-true preconditions first, or as a
*separate non-pending* feature, so an infra break is a real failure, not masked as pending.

## How to add a test

**L0** — extend `de.uzl.its.swat.symbolic.heap.BaseValueSpec` (gives `context`, `fmgr`, and the
`isValid(BooleanFormula)` / `isUnsatisfiable(BooleanFormula)` boolean oracles). Construct values
with `context`. Example: `ObjectIdentitySpec`.

**L1** — extend `de.uzl.its.swat.symbolic.processor.BaseSymbolicInstructionProcessorSpec`. Call
`setupTestContext(className, method)` first. For an unmodeled-call recovery, use the fixture:
```groovy
def result = executeBoundaryRecovery(receiver, owner, name, desc, concreteResult, resultAddress)
// result.recovered (Value), result.contextLoss
// resultAddress == receiver.address  -> this-return;  fresh address -> new-object return
```
Example: `ValueRecoverySpec`.

**L2** — add a tiny target to `src/test/resources/targets/<Name>.java` using `@Symbolic` on a
**method parameter** (a local needs `-g` and may crash the annotation transformer). Name the spec
`*AgentSpec` and use the harness:
```groovy
TraceObservation obs = AgentRun.run("targets/<Name>.java", "<MainClass>")
// obs.symbolicContextLoss / symbolicPrecisionLoss
// obs.inputNames ; obs.anyBranchReferences(inputVar)
```
`AgentRun` compiles against the agent jar, forks a JVM with `solver.mode=PRINT`, and parses the
TraceDTO. Example: `HeapRecoveryAgentSpec`.

## Run

```bash
# L0 + L1 (fast, in-process, CI lane). Scope to your specs to skip the legacy value tests.
./gradlew :symbolic-executor:test --tests "de.uzl.its.swat.symbolic.heap.*"
./gradlew :symbolic-executor:test --tests "de.uzl.its.swat.symbolic.processor.*"

# L2 (forked agent; builds the jar first; *AgentSpec only; excluded from `test`)
./gradlew :symbolic-executor:agentTest
```

Build the agent jar (when needed) with `:symbolic-executor:copyJar` — **never** `spotlessApply`
(it reformats the whole module). CI runs `copyNativeLibs` then `test`.
