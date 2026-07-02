package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.testsupport.agent.AgentRun
import de.uzl.its.swat.testsupport.agent.TraceObservation
import spock.lang.Specification

/**
 * End-to-end check that pure String methods which are unmodeled stubs in StringValue (so the generic
 * pure_&lt;sig&gt; UF fires), across String, int and boolean returns, are each modeled as a UF on a
 * symbolic String receiver. The run preserves SAFE (no context loss, no precision loss) with the UFs
 * in the branch constraints. The modeled String methods (substring, charAt, length, ...) are not
 * whitelisted; they stay precise and are exercised elsewhere.
 */
class StringWhitelistAgentSpec extends Specification {

    def "unmodeled pure String methods are modeled as UFs (no context or precision loss)"() {
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
