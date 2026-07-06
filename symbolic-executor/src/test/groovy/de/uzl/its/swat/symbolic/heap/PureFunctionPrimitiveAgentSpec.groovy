package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.testsupport.agent.AgentRun
import de.uzl.its.swat.testsupport.agent.TraceObservation
import spock.lang.Specification

/**
 * End-to-end check that pure, unmodeled, primitive-returning java.lang methods
 * (Math.floorDiv/floorMod/cbrt, Float.intBitsToFloat, Character.toLowerCase/isDigit) on symbolic
 * inputs are each modeled as a generic {@code pure_<sig>} UF over their inputs, across the
 * int/long/double/float/char/boolean return sorts. The run preserves SAFE: no context loss, no
 * precision loss, and the UFs ride into the branch constraints.
 */
class PureFunctionPrimitiveAgentSpec extends Specification {

    def "pure primitive-returning methods are modeled as UFs (no context or precision loss)"() {
        when: "the agent runs a program branching on pure unmodeled primitive returns over symbolic inputs"
        TraceObservation obs = AgentRun.run("targets/PurePrimReturnTarget.java", "PurePrimReturnTarget")

        then: "the run completed and a symbolic input was designated"
        obs != null
        !obs.inputNames.isEmpty()

        and: "modeling the pure returns as UFs loses neither context nor precision"
        !obs.symbolicContextLoss
        !obs.symbolicPrecisionLoss

        and: "the generic UFs actually entered the branch constraints (precision genuinely preserved)"
        obs.anyBranchReferences("pure_Math_floorDiv_int_int")   // int return
        obs.anyBranchReferences("pure_Math_floorDiv_long_long") // long return
        obs.anyBranchReferences("pure_Math_cbrt_double")        // double return
        obs.anyBranchReferences("pure_Float_intBitsToFloat_int") // float return
        obs.anyBranchReferences("pure_Character_toLowerCase_char") // char return
        obs.anyBranchReferences("pure_Character_isDigit_char")  // boolean return
    }

    def "a shadow sort deviating from the descriptor concretizes instead of corrupting the UF declaration"() {
        when: "a char-sorted shadow flows into an int-slot whitelisted call, then a genuine int does"
        TraceObservation obs = AgentRun.run("targets/CharWideningTarget.java", "CharWideningTarget")

        then: "the run completes (no UF signature clash) and the deviating call records context loss"
        obs != null
        obs.symbolicContextLoss

        and: "the descriptor-conforming call is still modeled as the UF"
        obs.anyBranchReferences("pure_Character_isWhitespace_int")
    }
}
