package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.testsupport.agent.AgentRun
import de.uzl.its.swat.testsupport.agent.TraceObservation
import spock.lang.Specification

/**
 * End-to-end check that pure java.lang/util methods across eight owners (Math, StrictMath, Character,
 * Integer, Byte, Float, Double, Objects) and every return sort, including the String-to-float parse
 * bridge, are each modeled as a generic pure_&lt;sig&gt; UF. The run preserves SAFE: no context loss,
 * no precision loss, and the UFs ride into the branch constraints.
 */
class WhitelistAuditAgentSpec extends Specification {

    def "pure methods across all whitelisted classes are modeled as UFs (no context or precision loss)"() {
        when: "the agent runs a program calling pure unmodeled JDK methods on symbolic inputs"
        TraceObservation obs = AgentRun.run("targets/WhitelistAuditAgentTarget.java", "WhitelistAuditAgentTarget")

        then: "the run completed and symbolic inputs were designated"
        obs != null
        !obs.inputNames.isEmpty()

        and: "modeling the pure results as UFs loses neither context nor precision"
        !obs.symbolicContextLoss
        !obs.symbolicPrecisionLoss

        and: "the generic UFs entered the branch constraints, one representative per owner and sort"
        obs.anyBranchReferences("pure_Math_pow_double_double")          // Math, double
        obs.anyBranchReferences("pure_StrictMath_cbrt_double")          // StrictMath, double
        obs.anyBranchReferences("pure_Math_addExact_int_int")           // Math, int
        obs.anyBranchReferences("pure_Integer_bitCount_int")            // Integer, int
        obs.anyBranchReferences("pure_Byte_hashCode_byte")              // Byte, int
        obs.anyBranchReferences("pure_Double_doubleToLongBits_double")  // Double, long
        obs.anyBranchReferences("pure_Character_isLetter_char")         // Character, boolean
        obs.anyBranchReferences("pure_Objects_checkIndex_int_int")      // Objects, int
        obs.anyBranchReferences("pure_Float_parseFloat_String")         // Float, String->float bridge
    }
}
