package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.testsupport.agent.AgentRun
import de.uzl.its.swat.testsupport.agent.TraceObservation
import spock.lang.See
import spock.lang.Specification

/**
 * G3-A2 end-to-end anchor (Level L2): the REAL agent runs a program that drives the boxed de-intern
 * instrumentation through both emitted-bytecode paths - the unbox+rebox de-intern wrap on
 * un-instrumented valueOf(String) returns for Integer (category-1) and Long (category-2 wide-local),
 * and the valueOf(primitive)->new rewrite via autoboxing for ALL SIX wrappers (Integer/Long/Short/Byte/
 * Character/Boolean), the path whose per-wrapper descriptors are DERIVED from the Boxed enum. The run
 * completing (AgentRun asserts exit == 0 plus a parsed TraceDTO) is the bytecode-validity oracle: a
 * malformed wrap or rewrite would VerifyError. Object-identity (the actual de-intern effect) is pinned
 * at L1 by OutputDeInternSpec, the boxed-cache == semantics at L0 by ProvenanceRefEqualsSpec; this
 * anchors that the boxed bytecode is real-JVM-valid for every wrapper. Mirrors PureFunctionUFAgentSpec.
 *
 * Naming: {@code *AgentSpec} -> run by the opt-in {@code agentTest} task. See docs/test-architecture.md.
 */
class BoxedDeInternAgentSpec extends Specification {

    @See("docs/heap-redesign-g3-design.md")
    def "G3-A2 (L2): all six wrappers' valueOf-rewrite (+ Integer/Long de-intern) load, verify, and run"() {
        when: "the agent runs a program driving the boxed de-intern bytecode paths"
        TraceObservation obs = AgentRun.run("targets/BoxedReturnTarget.java", "BoxedReturnTarget")

        then: "the run completed (exit 0 + parsed trace) - the unbox+rebox wrap is verifier-valid"
        obs != null

        and: "the symbolic input was designated and traced"
        !obs.inputNames.isEmpty()
    }
}
