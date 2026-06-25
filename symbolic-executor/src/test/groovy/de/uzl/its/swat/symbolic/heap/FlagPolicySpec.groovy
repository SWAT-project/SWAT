package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.common.Util
import de.uzl.its.swat.symbolic.processor.BaseSymbolicInstructionProcessorSpec
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue
import spock.lang.See

/**
 * Context-loss flag policy at Level L1: the flag fires iff symbolic data flowed into the unmodeled
 * call. F-1 (concrete receiver → no flag) and F-2 (symbolic receiver → flag) document the currently
 * correct behavior and guard it. See docs/heap-redesign-tests.md.
 */
class FlagPolicySpec extends BaseSymbolicInstructionProcessorSpec {

    private static final String STRING = "java/lang/String"
    private static final String TO_LOWER = "()Ljava/lang/String;"

    @See("docs/heap-redesign-tests.md")
    def "F-1: an unmodeled call with a concrete receiver raises no context-loss flag"() {
        given:
        setupTestContext(Util.formatClassName("de.uzl.its.swat.test.TestClass"), "main")
        StringValue receiver = new StringValue(solverContext, "abc", 0x1000) // concrete (not made symbolic)

        when:
        def result = executeBoundaryRecovery(receiver, STRING, "toLowerCase", TO_LOWER, "abc")

        then: "no symbolic data flowed into the black box, so no context loss"
        !result.contextLoss
    }

    @See("docs/heap-redesign-tests.md")
    def "F-2: an unmodeled call with a symbolic receiver raises a context-loss flag"() {
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
