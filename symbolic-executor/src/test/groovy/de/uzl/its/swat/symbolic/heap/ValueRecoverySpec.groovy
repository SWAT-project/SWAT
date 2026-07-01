package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.common.Util
import de.uzl.its.swat.symbolic.processor.BaseSymbolicInstructionProcessorSpec
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue
import de.uzl.its.swat.thread.ThreadHandler
import org.sosy_lab.java_smt.api.Formula

/**
 * Boundary recovery for value-returning calls. An unmodeled value-returning call must not let its
 * result alias the receiver's symbolic value, whether the call returns {@code this} or a fresh
 * object. The two cases differ only in the result's object identity (the receiver's own object for
 * a this-return versus a distinct object), isolating the reference-keyed recovery.
 */
class ValueRecoverySpec extends BaseSymbolicInstructionProcessorSpec {

    private static final String STRING = "java/lang/String"
    private static final String TO_LOWER = "()Ljava/lang/String;"

    private Set<String> varsOf(value) {
        return solverContext.getFormulaManager().extractVariables(value.formula as Formula).keySet()
    }

    def "a this-returning unmodeled call does not alias the receiver's symbolic formula"() {
        given: "a symbolic, already-lowercase String receiver registered on the heap"
        setupTestContext(Util.formatClassName("de.uzl.its.swat.test.TestClass"), "main")
        int receiverAddress = 0x1000
        String concrete = "abc" // already lowercase: the real toLowerCase() returns `this`
        StringValue receiver = new StringValue(solverContext, concrete, receiverAddress)
        receiver.MAKE_SYMBOLIC()
        def receiverVars = varsOf(receiver)

        and: "the receiver really is symbolic"
        assert receiverVars.size() == 1

        when: "toLowerCase() is invoked (unmodeled) and the this-return is recovered"
        def result = executeBoundaryRecovery(receiver, STRING, "toLowerCase", TO_LOWER,
                concrete) // this-return: the result object IS the receiver's concrete object

        then: "context loss is flagged and the concrete result is correct"
        result.contextLoss
        result.recovered.concrete == concrete

        and: "the recovered value does not carry the receiver's symbolic formula"
        varsOf(result.recovered).disjoint(receiverVars)
    }

    def "a fresh-object unmodeled return does not alias the receiver's symbolic formula"() {
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

    def "an unmodeled call returning a distinct tracked value recovers that value's shadow"() {
        given: "a symbolic receiver, and a DISTINCT symbolic value registered on the heap under its own concrete"
        setupTestContext(Util.formatClassName("de.uzl.its.swat.test.TestClass"), "main")
        StringValue receiver = new StringValue(solverContext, "receiver", 0x1000)
        receiver.MAKE_SYMBOLIC()
        def receiverVars = varsOf(receiver)

        String storedConcrete = "stored"
        StringValue stored = new StringValue(solverContext, storedConcrete, 0x2000)
        stored.MAKE_SYMBOLIC()
        def storedVars = varsOf(stored)
        ThreadHandler.getSymbolicVisitor(threadId).getStack().putToHeap(storedConcrete, stored)

        and: "receiver and stored carry different symbolic variables"
        assert !receiverVars.isEmpty() && !storedVars.isEmpty()
        assert receiverVars.disjoint(storedVars)

        when: "an unmodeled instance call returns the distinct stored object, not the receiver"
        // The fixture fabricates the returned identity (see its NOTE); a real example is map.get(k).
        def result = executeBoundaryRecovery(receiver, STRING, "toLowerCase", TO_LOWER, storedConcrete)

        then: "the stored value's shadow is recovered - it keeps its own symbolic formula"
        varsOf(result.recovered) == storedVars

        and: "so the result is neither concretized nor aliased to the receiver"
        !varsOf(result.recovered).isEmpty()
        varsOf(result.recovered).disjoint(receiverVars)
    }
}
