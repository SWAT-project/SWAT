package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.testsupport.agent.AgentRun
import de.uzl.its.swat.testsupport.agent.TraceObservation
import spock.lang.See
import spock.lang.Specification

/**
 * G4 broadened-whitelist anchor (Level L2): the REAL agent runs a program exercising the java.lang/util
 * purity-audit additions across all eight audited owners (Math, StrictMath, Character, Integer, Byte,
 * Float, Double, Objects) and every return sort, including the String->float parse bridge. Every result
 * must be modeled as a generic pure_<sig> UF, so the run preserves SAFE: no context loss, no precision
 * loss, and the UFs ride into the branch constraints. Companion to PureFunctionPrimitiveAgentSpec.
 *
 * Naming: {@code *AgentSpec} -> run by the opt-in {@code agentTest} task. See docs/test-architecture.md.
 */
class WhitelistAuditAgentSpec extends Specification {

    @See("docs/heap-redesign-g4-whitelist-survey.md")
    def "G4 (L2): audited whitelist additions are modeled as UFs across all classes (no context/precision loss)"() {
        when: "the agent runs a program calling pure unmodeled JDK methods from the audit on symbolic inputs"
        TraceObservation obs = AgentRun.run("targets/WhitelistAuditAgentTarget.java", "WhitelistAuditAgentTarget")

        then: "the run completed and symbolic inputs were designated"
        obs != null
        !obs.inputNames.isEmpty()

        and: "modeling the pure results as UFs loses neither context nor precision"
        !obs.symbolicContextLoss
        !obs.symbolicPrecisionLoss

        and: "the generic UFs entered the branch constraints, one representative per audited owner + sort"
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
