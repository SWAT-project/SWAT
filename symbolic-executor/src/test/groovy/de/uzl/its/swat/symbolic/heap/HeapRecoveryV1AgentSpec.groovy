package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.testsupport.agent.AgentRun
import de.uzl.its.swat.testsupport.agent.TraceObservation
import spock.lang.See
import spock.lang.Specification

/**
 * V-1 / R-1 at Level L2: the REAL agent runs a real {@code @Symbolic} toLowerCase program
 * (ToLowerCaseTarget) and we observe the emitted TraceDTO. This is the faithful altitude — the
 * identity hash, the this-return and the soundness flags come from the real JVM + instrumenter, not
 * a fabricated instruction stream.
 *
 * Naming: {@code *AgentSpec} → run by the opt-in {@code agentTest} Gradle task, excluded from
 * {@code test}. See docs/test-architecture.md (Level L2).
 */
class HeapRecoveryV1AgentSpec extends Specification {

    @See("docs/test-architecture.md")
    def "L2 soundness anchor: real toLowerCase on a symbolic string flags context loss"() {
        when:
        TraceObservation obs = AgentRun.run("targets/ToLowerCaseTarget.java", "ToLowerCaseTarget")

        then: "the symbolic input is designated and the soundness backstop (context loss) fires"
        obs.inputNames.any { it.startsWith("java/lang/String") }
        obs.symbolicContextLoss
    }

    @See("docs/heap-redesign-tests.md")
    def "V-1 (L2): a branch after unmodeled toLowerCase must not reference the symbolic input"() {
        when:
        TraceObservation obs = AgentRun.run("targets/ToLowerCaseTarget.java", "ToLowerCaseTarget")
        String inputVar = obs.inputNames.find { it.startsWith("java/lang/String") }

        then: "post-G2 the result is concrete, so no branch constraint mentions the input (RED until G2)"
        inputVar != null
        !obs.anyBranchReferences(inputVar)
    }
}
