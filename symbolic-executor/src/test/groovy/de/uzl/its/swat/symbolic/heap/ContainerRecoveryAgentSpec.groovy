package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.testsupport.agent.AgentRun
import de.uzl.its.swat.testsupport.agent.TraceObservation
import spock.lang.Specification

/**
 * End-to-end check that a value retrieved from an unmodeled container keeps its symbolic value. The
 * agent stores a symbolic String in a HashMap and reads it back via a concrete key; recovering the
 * stored shadow (rather than concretizing) means a branch on the retrieved value still references the
 * symbolic input, so the solver can drive it.
 */
class ContainerRecoveryAgentSpec extends Specification {

    def "a value retrieved from an unmodeled container keeps its symbolic formula"() {
        when:
        TraceObservation obs = AgentRun.run("targets/ContainerRecoveryTarget.java", "ContainerRecoveryTarget")
        String inputVar = obs.inputNames.find { it.startsWith("java/lang/String") }

        then: "the symbolic input is designated"
        inputVar != null

        and: "the branch on the retrieved value references the symbolic input (recovered, not concretized)"
        obs.anyBranchReferences(inputVar)
    }
}
