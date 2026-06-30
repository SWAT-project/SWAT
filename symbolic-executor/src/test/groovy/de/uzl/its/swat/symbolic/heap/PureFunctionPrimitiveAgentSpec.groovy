package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.testsupport.agent.AgentRun
import de.uzl.its.swat.testsupport.agent.TraceObservation
import spock.lang.See
import spock.lang.Specification

/**
 * G4 primitive-return end-to-end anchor (Level L2): the REAL agent runs a program that takes pure,
 * unmodeled, PRIMITIVE-returning java.lang methods (Math.floorDiv/floorMod/cbrt, Float.intBitsToFloat,
 * Character.toLowerCase/isDigit) on symbolic inputs and branches on each result. Each result must be
 * modeled as a generic {@code pure_<sig>} UF over its inputs (the int/long/double/float/char/boolean
 * return sorts), so the run preserves SAFE: NO context loss, NO precision loss, and the UFs actually
 * ride into the branch constraints. Mirrors PureFunctionUFAgentSpec (the String-return analogue).
 *
 * Naming: {@code *AgentSpec} -> run by the opt-in {@code agentTest} task. See docs/test-architecture.md.
 */
class PureFunctionPrimitiveAgentSpec extends Specification {

    @See("docs/heap-redesign-g4-whitelist-survey.md")
    def "G4 (L2): pure primitive-returning methods are modeled as UFs (no context/precision loss)"() {
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
}
