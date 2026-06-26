package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.testsupport.agent.AgentRun
import de.uzl.its.swat.testsupport.agent.TraceObservation
import spock.lang.See
import spock.lang.Specification

/**
 * G4 end-to-end anchor at Level L2: the REAL agent runs a {@code @Symbolic} program that calls a
 * whitelisted pure unmodeled method ({@code String.trim()}) and branches on its result
 * (TrimTarget). The emitted TraceDTO must show the relationship preserved (the branch references the
 * input through the {@code pure_String_trim} UF) AND neither soundness flag set - i.e. SAFE is
 * genuinely preserved through the call, the headline G4-full claim, verified with real instrumentation
 * rather than a fabricated instruction stream. Contrast with HeapRecoveryV1AgentSpec, where the
 * non-whitelisted {@code toLowerCase} still flags context loss.
 *
 * Naming: {@code *AgentSpec} -> run by the opt-in {@code agentTest} task. See docs/test-architecture.md.
 */
class PureFunctionUFAgentSpec extends Specification {

    @See("docs/heap-redesign-g4-design.md")
    def "G4 (L2): a whitelisted pure trim into a branch preserves SAFE (no context/precision loss)"() {
        when:
        TraceObservation obs = AgentRun.run("targets/TrimTarget.java", "TrimTarget")
        String inputVar = obs.inputNames.find { it.startsWith("java/lang/String") }

        then: "the symbolic input is designated"
        inputVar != null

        and: "the relationship is preserved - the branch references the input through the UF (unlike G2)"
        obs.anyBranchReferences(inputVar)

        and: "neither SAFE downgrade fires: context loss skipped (modeled) and the pure_ UF is exempt"
        !obs.symbolicContextLoss
        !obs.symbolicPrecisionLoss
    }

    @See("docs/heap-redesign-g4-whitelist-survey.md")
    def "G4 (L2): a whitelisted pure substring (arg-taking, mixed-sort UF) into a branch preserves SAFE"() {
        when:
        TraceObservation obs = AgentRun.run("targets/SubstringTarget.java", "SubstringTarget")
        String inputVar = obs.inputNames.find { it.startsWith("java/lang/String") }

        then: "the symbolic input is designated"
        inputVar != null

        and: "the branch references the input through the mixed-sort UF pure_String_substring_int"
        obs.anyBranchReferences(inputVar)

        and: "neither SAFE downgrade fires (whitelist expansion to an arg-taking method stays sound)"
        !obs.symbolicContextLoss
        !obs.symbolicPrecisionLoss
    }
}
