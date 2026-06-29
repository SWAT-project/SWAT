package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.testsupport.agent.AgentRun
import de.uzl.its.swat.testsupport.agent.TraceObservation
import spock.lang.See
import spock.lang.Specification

/**
 * G3-A2 end-to-end anchor (Level L2): the REAL agent runs a program that receives boxed wrappers from
 * un-instrumented java/lang methods (Integer.valueOf(String), Long.valueOf(String)), which trigger the
 * unbox+rebox de-intern wrap - including the category-2 (long) wide-local path. The run completing
 * (AgentRun asserts exit == 0 plus a parsed TraceDTO) is the bytecode-validity oracle: a malformed wrap
 * would VerifyError. Object-identity (the actual de-intern effect) is pinned at L1 by OutputDeInternSpec;
 * this anchors that the boxed bytecode is real-JVM-valid. Mirrors PureFunctionUFAgentSpec.
 *
 * Naming: {@code *AgentSpec} -> run by the opt-in {@code agentTest} task. See docs/test-architecture.md.
 */
class BoxedDeInternAgentSpec extends Specification {

    @See("docs/heap-redesign-g3-design.md")
    def "G3-A2 (L2): boxed returns (Integer + Long) de-intern, load, verify, and run"() {
        when: "the agent runs a program taking boxed wrappers from un-instrumented methods"
        TraceObservation obs = AgentRun.run("targets/BoxedReturnTarget.java", "BoxedReturnTarget")

        then: "the run completed (exit 0 + parsed trace) - the unbox+rebox wrap is verifier-valid"
        obs != null

        and: "the symbolic input was designated and traced"
        !obs.inputNames.isEmpty()
    }
}
