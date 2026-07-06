package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.testsupport.agent.AgentRun
import de.uzl.its.swat.testsupport.agent.TraceObservation
import spock.lang.Specification

/**
 * End-to-end check that the agent runs a {@code @Symbolic} program calling a whitelisted pure
 * unmodeled method ({@code String.trim()}) and branching on its result. The emitted trace shows the
 * relationship preserved (the branch references the input through the {@code pure_String_trim} UF)
 * and neither soundness flag set, so a SAFE verdict survives the call.
 */
class PureFunctionUFAgentSpec extends Specification {

    def "a whitelisted pure trim into a branch preserves SAFE (no context or precision loss)"() {
        when:
        TraceObservation obs = AgentRun.run("targets/TrimTarget.java", "TrimTarget")
        String inputVar = obs.inputNames.find { it.startsWith("java/lang/String") }

        then: "the symbolic input is designated"
        inputVar != null

        and: "the relationship is preserved - the branch references the input through the UF"
        obs.anyBranchReferences(inputVar)
        obs.anyBranchReferences("pure_String_trim")

        and: "neither SAFE downgrade fires: context loss is skipped because modeled, and the pure_ UF is exempt"
        !obs.symbolicContextLoss
        !obs.symbolicPrecisionLoss
    }

    def "a whitelisted pure repeat (arg-taking, mixed-sort UF) into a branch preserves SAFE"() {
        when:
        TraceObservation obs = AgentRun.run("targets/RepeatTarget.java", "RepeatTarget")
        String inputVar = obs.inputNames.find { it.startsWith("java/lang/String") }

        then: "the symbolic input is designated"
        inputVar != null

        and: "the branch references the input through the mixed-sort UF pure_String_repeat_int"
        obs.anyBranchReferences(inputVar)
        obs.anyBranchReferences("pure_String_repeat")

        and: "neither SAFE downgrade fires (an arg-taking whitelisted method stays sound)"
        !obs.symbolicContextLoss
        !obs.symbolicPrecisionLoss
    }
}
