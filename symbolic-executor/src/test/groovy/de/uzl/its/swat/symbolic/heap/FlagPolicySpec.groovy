package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.common.Util
import de.uzl.its.swat.symbolic.processor.BaseSymbolicInstructionProcessorSpec
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue

/**
 * Context-loss flag policy: the flag fires if and only if symbolic data flowed into an unmodeled call.
 * A concrete receiver raises no flag; a symbolic receiver raises the flag.
 */
class FlagPolicySpec extends BaseSymbolicInstructionProcessorSpec {

    private static final String STRING = "java/lang/String"
    private static final String TO_LOWER = "()Ljava/lang/String;"

    def "an unmodeled call with a concrete receiver raises no context-loss flag"() {
        given:
        setupTestContext(Util.formatClassName("de.uzl.its.swat.test.TestClass"), "main")
        StringValue receiver = new StringValue(solverContext, "abc", 0x1000) // concrete (not made symbolic)

        when:
        def result = executeBoundaryRecovery(receiver, STRING, "toLowerCase", TO_LOWER, "abc")

        then: "no symbolic data flowed into the black box, so no context loss"
        !result.contextLoss
    }

    def "an unmodeled call with a symbolic receiver raises a context-loss flag"() {
        given:
        setupTestContext(Util.formatClassName("de.uzl.its.swat.test.TestClass"), "main")
        StringValue receiver = new StringValue(solverContext, "abc", 0x1000)
        receiver.MAKE_SYMBOLIC()

        when:
        def result = executeBoundaryRecovery(receiver, STRING, "toLowerCase", TO_LOWER, "abc")

        then: "symbolic data reached the unmodeled method, so context loss is flagged"
        result.contextLoss
    }
}
