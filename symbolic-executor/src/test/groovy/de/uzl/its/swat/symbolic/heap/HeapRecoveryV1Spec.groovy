package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.common.Util
import de.uzl.its.swat.symbolic.processor.BaseSymbolicInstructionProcessorSpec
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue
import org.sosy_lab.java_smt.api.Formula
import spock.lang.PendingFeature
import spock.lang.See

/**
 * V-1 — the core toLowerCase bug. Level L1 (boundary recovery), Phase 2 (G2).
 *
 * Scenario: a symbolic, already-lowercase String is the receiver of an unmodeled {@code toLowerCase()}
 * that returns {@code this}. Expected: the result does NOT depend on the receiver's symbolic value;
 * it is concrete; a context-loss flag is raised; its concrete value equals the real result.
 *
 * Today the identity-keyed recovery (visitGETVALUE_Object) re-binds the placeholder result to the
 * receiver's StringValue, so the result aliases the receiver's symbolic formula — the RED assertion.
 * The preconditions (context-loss flagged, concrete correct) already hold and guard infra breakage.
 */
class HeapRecoveryV1Spec extends BaseSymbolicInstructionProcessorSpec {

    @See("docs/heap-redesign-tests.md")
    @PendingFeature(reason = "G2 value-type boundary recovery not yet implemented; result still aliases the receiver's formula")
    def "V-1: toLowerCase this-return must not alias the receiver's symbolic formula"() {
        given: "a symbolic, already-lowercase String receiver registered on the heap"
        String testClassName = Util.formatClassName("de.uzl.its.swat.test.TestClass")
        setupTestContext(testClassName, "main")
        int receiverAddress = 0x1000
        String concrete = "abc" // already lowercase: the real toLowerCase() returns `this`
        StringValue receiver = new StringValue(solverContext, concrete, receiverAddress)
        receiver.MAKE_SYMBOLIC()
        def receiverVars = solverContext.getFormulaManager()
                .extractVariables(receiver.formula as Formula).keySet()

        and: "precondition: the receiver really is symbolic"
        assert receiverVars.size() == 1

        when: "toLowerCase() is invoked (unmodeled) and the this-return is recovered"
        def result = executeBoundaryRecovery(
                receiver,
                Util.formatClassName("java.lang.String"),
                "toLowerCase",
                "()Ljava/lang/String;",
                concrete,
                receiverAddress) // this-return: result address == receiver address

        then: "preconditions that already hold today: context loss is flagged, concrete is correct"
        result.contextLoss
        result.recovered.concrete == concrete

        and: "the recovered value must NOT carry the receiver's symbolic formula (RED until G2)"
        def recoveredVars = solverContext.getFormulaManager()
                .extractVariables(result.recovered.formula as Formula).keySet()
        recoveredVars.disjoint(receiverVars)
    }
}
