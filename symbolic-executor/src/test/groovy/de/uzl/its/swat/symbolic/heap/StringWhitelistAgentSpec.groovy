package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.testsupport.agent.AgentRun
import de.uzl.its.swat.testsupport.agent.TraceObservation
import spock.lang.Specification

/**
 * G4 String-whitelist anchor (Level L2): the REAL agent runs a program calling pure String methods that
 * the purity audit confirmed are UNMODELED stubs in StringValue (so the generic pure_<sig> UF fires) -
 * across String, int and boolean returns - on a symbolic String receiver. Each must be modeled as a UF,
 * preserving SAFE (no context loss, no precision loss) with the UFs in the branch constraints. The
 * modeled String methods (substring, charAt, length, ...) are intentionally NOT whitelisted; they stay
 * precise and are exercised by PureFunctionUFAgentSpec.
 *
 * Naming: {@code *AgentSpec} -> run by the opt-in {@code agentTest} task. See docs/test-architecture.md.
 */
class StringWhitelistAgentSpec extends Specification {

    def "G4 (L2): unmodeled pure String methods are modeled as UFs (no context/precision loss)"() {
        when: "the agent runs a program calling pure unmodeled String stubs on a symbolic receiver"
        TraceObservation obs = AgentRun.run("targets/StringWhitelistTarget.java", "StringWhitelistTarget")

        then: "the run completed and the symbolic input was designated"
        obs != null
        !obs.inputNames.isEmpty()

        and: "modeling the pure results as UFs loses neither context nor precision"
        !obs.symbolicContextLoss
        !obs.symbolicPrecisionLoss

        and: "the generic UFs entered the branch constraints, across return sorts"
        obs.anyBranchReferences("pure_String_hashCode")          // int
        obs.anyBranchReferences("pure_String_matches_String")    // boolean
        obs.anyBranchReferences("pure_String_compareTo_String")  // int
        obs.anyBranchReferences("pure_String_repeat_int")        // String
        obs.anyBranchReferences("pure_String_isBlank")           // boolean
    }
}
