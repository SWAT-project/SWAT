package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.common.Util
import de.uzl.its.swat.symbolic.processor.BaseSymbolicInstructionProcessorSpec
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue
import de.uzl.its.swat.thread.ThreadHandler
import org.sosy_lab.java_smt.api.BooleanFormula
import org.sosy_lab.java_smt.api.Formula
import org.sosy_lab.java_smt.api.StringFormula

/**
 * G4 generic-UF mechanism (Level L1). A whitelisted, pure, UNMODELED value-returning call
 * ({@code String.trim}) on a symbolic input is modeled as the generic UF {@code pure_String_trim}
 * over that input, instead of being concretized (G2). This preserves the relational fact (equal
 * inputs =&gt; equal outputs) while staying sound (the UF is axiom-free). See
 * docs/heap-redesign-g4-design.md and docs/heap-redesign-tests.md.
 */
class PureFunctionUFSpec extends BaseSymbolicInstructionProcessorSpec {

    private static final String STRING = "java/lang/String"
    private static final String TRIM = "()Ljava/lang/String;"

    private Set<String> symbolsOf(value) {
        return solverContext.getFormulaManager().extractVariablesAndUFs(value.formula as Formula).keySet()
    }

    private Set<String> varsOf(value) {
        return solverContext.getFormulaManager().extractVariables(value.formula as Formula).keySet()
    }

    private boolean isValid(BooleanFormula f) {
        def p = solverContext.newProverEnvironment()
        try {
            p.addConstraint(solverContext.getFormulaManager().getBooleanFormulaManager().not(f))
            return p.isUnsat()
        } finally { p.close() }
    }

    private boolean isSat(BooleanFormula f) {
        def p = solverContext.newProverEnvironment()
        try {
            p.addConstraint(f)
            return !p.isUnsat()
        } finally { p.close() }
    }

    def "U-5: trim (whitelisted, unmodeled) models its result as a UF over the input"() {
        given: "a symbolic String receiver"
        setupTestContext(Util.formatClassName("de.uzl.its.swat.test.TestClass"), "main")
        StringValue receiver = new StringValue(solverContext, "abc", 0x1000)
        receiver.MAKE_SYMBOLIC()
        def receiverVars = varsOf(receiver)
        assert receiverVars.size() == 1

        when: "trim() is invoked (unmodeled, whitelisted) and recovered"
        def result = executeBoundaryRecovery(receiver, STRING, "trim", TRIM, "abc")

        then: "modeled as the generic UF over the input - depends on it, not concretized, not aliased"
        symbolsOf(result.recovered).contains("pure_String_trim")
        symbolsOf(result.recovered).containsAll(receiverVars)
        result.recovered.concrete == "abc"
    }

    def "U-4: equal inputs yield equal results (UF congruence / determinism)"() {
        given: "two independently symbolic String receivers"
        setupTestContext(Util.formatClassName("de.uzl.its.swat.test.TestClass"), "main")
        StringValue s1 = new StringValue(solverContext, "abc", 0x1000); s1.MAKE_SYMBOLIC()
        StringValue s2 = new StringValue(solverContext, "abc", 0x2000); s2.MAKE_SYMBOLIC()

        when: "trim() is recovered for each"
        def r1 = executeBoundaryRecovery(s1, STRING, "trim", TRIM, "abc")
        def r2 = executeBoundaryRecovery(s2, STRING, "trim", TRIM, "abc")

        then: "(s1 == s2) implies (trim(s1) == trim(s2)) is valid"
        def smgr = solverContext.getFormulaManager().getStringFormulaManager()
        def bmgr = solverContext.getFormulaManager().getBooleanFormulaManager()
        BooleanFormula premise = smgr.equal(s1.formula as StringFormula, s2.formula as StringFormula)
        BooleanFormula conclusion = smgr.equal(r1.recovered.formula as StringFormula, r2.recovered.formula as StringFormula)
        isValid(bmgr.implication(premise, conclusion))
    }

    def "U-soundness: the axiom-free UF excludes no real behavior (result can equal any value)"() {
        given:
        setupTestContext(Util.formatClassName("de.uzl.its.swat.test.TestClass"), "main")
        StringValue receiver = new StringValue(solverContext, "abc", 0x1000)
        receiver.MAKE_SYMBOLIC()

        when:
        def result = executeBoundaryRecovery(receiver, STRING, "trim", TRIM, "abc")

        then: "no hidden axiom forces the result - it may take an arbitrary value (SAT)"
        def smgr = solverContext.getFormulaManager().getStringFormulaManager()
        isSat(smgr.equal(result.recovered.formula as StringFormula, smgr.makeString("a totally different value")))
    }

    def "U-7: a whitelisted pure call emits its observed (input->output) pair as a ground UF constraint"() {
        given: "a symbolic String receiver with a known concrete value"
        setupTestContext(Util.formatClassName("de.uzl.its.swat.test.TestClass"), "main")
        StringValue receiver = new StringValue(solverContext, "abc", 0x1000)
        receiver.MAKE_SYMBOLIC()

        when: "trim() is invoked (whitelisted) and recovered, observing output 'abc'"
        executeBoundaryRecovery(receiver, STRING, "trim", TRIM, "abc")

        then: "an observed-pair constraint over the pure_String_trim UF was recorded for this run"
        def fm = solverContext.getFormulaManager()
        def constraints = ThreadHandler.getSymbolicTraceHandler(threadId).getConstraints()
        def pair = constraints.find { fm.extractVariablesAndUFs(it).keySet().contains("pure_String_trim") }
        pair != null

        and: "the pair is GROUND (over the constant input, not the symbolic variable)"
        fm.extractVariables(pair).isEmpty()
    }

    def "U-6: a whitelisted pure call is modeled (UF), so it does NOT flag context loss"() {
        given: "a symbolic String receiver"
        setupTestContext(Util.formatClassName("de.uzl.its.swat.test.TestClass"), "main")
        StringValue receiver = new StringValue(solverContext, "abc", 0x1000)
        receiver.MAKE_SYMBOLIC()

        when: "trim() (whitelisted, pure) is invoked and modeled as a UF"
        def result = executeBoundaryRecovery(receiver, STRING, "trim", TRIM, "abc")

        then: "context loss is NOT flagged (it is the only remaining SAFE downgrade, and we modeled it)"
        !result.contextLoss
    }
}
