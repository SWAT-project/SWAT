package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.common.Util
import de.uzl.its.swat.symbolic.processor.BaseSymbolicInstructionProcessorSpec
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue
import org.sosy_lab.java_smt.api.Formula
import spock.lang.PendingFeature
import spock.lang.See

/**
 * Value-typed boundary recovery at Level L1 (the workhorse fixture). An unmodeled value-returning
 * call must not let its result alias the receiver's symbolic value - whether the call returns
 * {@code this} (V-1) or a fresh object (V-2). The only difference between the two below is the
 * result's object identity (the receiver's own object for a this-return vs a distinct object),
 * which isolates the reference-keyed recovery as the defect. See docs/heap-redesign-tests.md.
 */
class ValueRecoverySpec extends BaseSymbolicInstructionProcessorSpec {

    private static final String STRING = "java/lang/String"
    private static final String TO_LOWER = "()Ljava/lang/String;"

    private Set<String> varsOf(value) {
        return solverContext.getFormulaManager().extractVariables(value.formula as Formula).keySet()
    }

    @See("docs/heap-redesign-tests.md")
    @PendingFeature(reason = "G2 value-type boundary recovery not yet implemented; result still aliases the receiver's formula")
    def "V-1: toLowerCase this-return must not alias the receiver's symbolic formula"() {
        given: "a symbolic, already-lowercase String receiver registered on the heap"
        setupTestContext(Util.formatClassName("de.uzl.its.swat.test.TestClass"), "main")
        int receiverAddress = 0x1000
        String concrete = "abc" // already lowercase: the real toLowerCase() returns `this`
        StringValue receiver = new StringValue(solverContext, concrete, receiverAddress)
        receiver.MAKE_SYMBOLIC()
        def receiverVars = varsOf(receiver)

        and: "precondition: the receiver really is symbolic"
        assert receiverVars.size() == 1

        when: "toLowerCase() is invoked (unmodeled) and the this-return is recovered"
        def result = executeBoundaryRecovery(receiver, STRING, "toLowerCase", TO_LOWER,
                concrete) // this-return: the result object IS the receiver's concrete object

        then: "preconditions that hold today: context loss flagged, concrete correct"
        result.contextLoss
        result.recovered.concrete == concrete

        and: "the recovered value must NOT carry the receiver's symbolic formula (RED until G2)"
        varsOf(result.recovered).disjoint(receiverVars)
    }

    @See("docs/heap-redesign-tests.md")
    def "V-2: a new-object transform return does not alias the receiver (consistent with V-1)"() {
        given: "a symbolic, concretely upper-case String receiver (toLowerCase returns a NEW object)"
        setupTestContext(Util.formatClassName("de.uzl.its.swat.test.TestClass"), "main")
        int receiverAddress = 0x1000
        StringValue receiver = new StringValue(solverContext, "ABC", receiverAddress)
        receiver.MAKE_SYMBOLIC()
        def receiverVars = varsOf(receiver)

        when: "the result returns at a fresh address (a distinct object), not the receiver's"
        def result = executeBoundaryRecovery(receiver, STRING, "toLowerCase", TO_LOWER,
                new String("abc")) // distinct object => new-object return, not a this-return

        then: "context loss is flagged, the concrete is the real result, and there is no aliasing"
        result.contextLoss
        result.recovered.concrete == "abc"
        varsOf(result.recovered).disjoint(receiverVars)
    }
}
