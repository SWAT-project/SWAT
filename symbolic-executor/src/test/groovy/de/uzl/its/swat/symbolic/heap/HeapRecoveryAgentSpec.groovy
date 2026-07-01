package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.testsupport.agent.AgentRun
import de.uzl.its.swat.testsupport.agent.TraceObservation
import spock.lang.Specification

/**
 * End-to-end recovery: the agent runs a real {@code @Symbolic} toLowerCase program and the emitted
 * TraceDTO is observed. The identity hash, the this-return and the soundness flags come from the
 * real JVM and instrumenter rather than a fabricated instruction stream.
 */
class HeapRecoveryAgentSpec extends Specification {

    def "real toLowerCase on a symbolic string flags context loss"() {
        when:
        TraceObservation obs = AgentRun.run("targets/ToLowerCaseTarget.java", "ToLowerCaseTarget")

        then: "the symbolic input is designated and the soundness backstop (context loss) fires"
        obs.inputNames.any { it.startsWith("java/lang/String") }
        obs.symbolicContextLoss
    }

    def "a branch after unmodeled toLowerCase does not reference the symbolic input"() {
        when:
        TraceObservation obs = AgentRun.run("targets/ToLowerCaseTarget.java", "ToLowerCaseTarget")
        String inputVar = obs.inputNames.find { it.startsWith("java/lang/String") }

        then: "the result is concrete, so no branch constraint mentions the input"
        inputVar != null
        !obs.anyBranchReferences(inputVar)
    }
}
