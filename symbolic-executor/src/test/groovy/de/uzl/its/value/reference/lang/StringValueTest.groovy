package de.uzl.its.value.reference.lang

import de.uzl.its.swat.symbolic.invoke.java.lang.StringCallBranches
import de.uzl.its.swat.symbolic.value.Value
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.BooleanValue
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue
import de.uzl.its.swat.thread.ThreadHandler
import org.objectweb.asm.Type
import org.sosy_lab.java_smt.api.*
import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Tag

import static java.lang.Thread.currentThread

class StringValueTest extends Specification {

    SolverContext context
    ProverEnvironment prover
    FormulaManager fmgr
    BooleanFormulaManager bmgr
    IntegerFormulaManager imgr
    StringFormulaManager smgr
    FloatingPointFormulaManager fpmgr

    // keep descriptor identical to original to avoid surprises
    private static final Type[] STRING_DESC = [Type.getType("Ljava/lang/String")] as Type[]

    def setup() {
        if (ThreadHandler.hasThreadContext(currentThread().id)) {
            ThreadHandler.removeThreadContext(currentThread().id)
        }
        ThreadHandler.addThreadContext(currentThread().id, "Test-Thread", -2)
        context = ThreadHandler.getSolverContext(currentThread().id)
        prover  = context.newProverEnvironment(SolverContext.ProverOptions.GENERATE_MODELS)
        fmgr    = context.formulaManager
        bmgr    = fmgr.booleanFormulaManager
        imgr    = fmgr.integerFormulaManager
        smgr    = fmgr.stringFormulaManager
        fpmgr   = fmgr.floatingPointFormulaManager
    }

    def cleanup() {
        prover.close()
        context.close()
        ThreadHandler.removeThreadContext(currentThread().id)
    }

    // ---- helpers ----

    private void addUFConstraintsFromTrace() {
        // UF-defining constraints are recorded on the symbolic trace (see
        // StringValue.invokeEqualsIgnoreCase); the trace is fresh per test
        // because setup() recreates the thread context.
        ThreadHandler.getSymbolicTraceHandler(currentThread().id).getConstraints().each { prover.addConstraint(it) }
    }

    /** Force the SMT result to disagree with the concrete Java result. */
    private void forceMismatch(BooleanFormula smtResult, boolean expectedJava) {
        if (expectedJava) {
            prover.addConstraint(bmgr.not(smtResult))
        } else {
            prover.addConstraint(smtResult)
        }
    }

    /**
     * Evaluates both string formulas in the current model. Unlike scraping the
     * model dump, Model.evaluate also assigns don't-care variables that the
     * solver omits from its partial model.
     */
    private List<String> evaluateModel(def f1, def f2) {
        prover.getModel().withCloseable { model ->
            [model.evaluate(f1), model.evaluate(f2)]
        }
    }

    // ---- tests ----

    @Unroll
    def "equalsIgnoreCase(String anotherString)" (String s1, String s2, boolean expected) {
        setup:
        prover.push()
        def lhs  = new StringValue(context, s1, smgr.makeVariable("s1"), -1)
        def rhs  = new StringValue(context, s2, -1)
        def args = [rhs] as Value<?, ?>[]

        when:
        def result = (BooleanValue) lhs.invokeMethod("equalsIgnoreCase", STRING_DESC, args)
        addUFConstraintsFromTrace()
        forceMismatch(result.formula, expected)

        then:
        !prover.isUnsat()
        result.concrete == expected
        noExceptionThrown()

        and:
        def (s1Model, _) = evaluateModel(lhs.formula, rhs.formula)
        assert s1Model != null
        assert (s1Model.equalsIgnoreCase(s2)) != expected

        cleanup:
        prover.pop()

        where:
        s1       | s2       | expected
        "hello"  | "HELLO"  | true
        "Hello"  | "hello"  | true
        "test"   | "TEST"   | true
        "abc"    | "xyz"    | false
        "short"  | "longer" | false
        "same"   | "same"   | true
        ""       | ""       | true
        "a"      | ""       | false
        "Case"   | "case"   | true
    }

    @Unroll
    def "equalsIgnoreCase(String anotherString) - Enforce same length" (String s1, String s2, boolean expected) {
        setup:
        prover.push()
        def lhs  = new StringValue(context, s1, smgr.makeVariable("s1"), -1)
        def rhs  = new StringValue(context, s2, -1)
        def args = [rhs] as Value<?, ?>[]

        when:
        def result = (BooleanValue) lhs.invokeMethod("equalsIgnoreCase", STRING_DESC, args)
        addUFConstraintsFromTrace()
        forceMismatch(result.formula, expected)
        // exactly as in your original: enforce |lhs| == |rhs|
        prover.addConstraint(imgr.equal(smgr.length(lhs.formula), smgr.length(rhs.formula)))

        then:
        !prover.isUnsat()
        result.concrete == expected
        noExceptionThrown()

        and:
        def (s1Model, _) = evaluateModel(lhs.formula, rhs.formula)
        assert s1Model != null
        assert s1Model.length() == s2.length()
        assert (s1Model.equalsIgnoreCase(s2)) != expected

        cleanup:
        prover.pop()

        where:
        s1       | s2       | expected
        "hello"  | "HELLO"  | true
        "Hello"  | "hello"  | true
        "test"   | "TEST"   | true
        "abc"    | "xyz"    | false
        "short"  | "longer" | false
        "same"   | "same"   | true
        "a"      | ""       | false
        "Case"   | "case"   | true
    }

    @Tag("slow")
    @Unroll
    def "equalsIgnoreCase(String anotherString) - Two symbolic strings" (String s1, String s2, boolean expected) {
        setup:
        prover.push()
        def lhs  = new StringValue(context, s1, smgr.makeVariable("s1"), -1)
        def rhs  = new StringValue(context, s2, smgr.makeVariable("s2"), -1)
        def args = [rhs] as Value<?, ?>[]

        when:
        def result = (BooleanValue) lhs.invokeMethod("equalsIgnoreCase", STRING_DESC, args)
        addUFConstraintsFromTrace()
        forceMismatch(result.formula, expected)

        then:
        !prover.isUnsat()
        result.concrete == expected
        noExceptionThrown()

        and:
        def (s1Model, s2Model) = evaluateModel(lhs.formula, rhs.formula)
        assert s1Model != null
        assert (s1Model.equalsIgnoreCase(s2Model)) != expected

        cleanup:
        prover.pop()

        where:
        // With both strings symbolic the concrete values do not constrain the
        // solver, so every row is the same (expensive, minutes-long) query —
        // one row per expected value is full coverage.
        s1       | s2       | expected
        "hello"  | "HELLO"  | true
        "abc"    | "xyz"    | false
    }

    @Unroll
    def "equalsIgnoreCase(String anotherString) - Two symbolic strings - other strings of the pinned length" (String s1, String s2, boolean expected) {
        setup:
        prover.push()
        def lhs  = new StringValue(context, s1, smgr.makeVariable("s1"), -1)
        def rhs  = new StringValue(context, s2, smgr.makeVariable("s2"), -1)
        def args = [rhs] as Value<?, ?>[]

        when:
        def result = (BooleanValue) lhs.invokeMethod("equalsIgnoreCase", STRING_DESC, args)
        addUFConstraintsFromTrace()
        // The call's branches as they hold here: lengths, pinned length, ASCII strings
        StringCallBranches.of(lhs, "equalsIgnoreCase", "(Ljava/lang/String;)Z", args).each {
            prover.addConstraint(it.holds() ? it.condition() : bmgr.not(it.condition()))
        }
        // Other strings than the concrete ones, with the opposite result
        prover.addConstraint(bmgr.not(smgr.equal(lhs.formula, smgr.makeString(s1))))
        forceMismatch(result.formula, expected)

        then:
        result.concrete == expected
        noExceptionThrown()

        and:
        // Strings with the other result exist (of the same length), and the model gives it them
        !prover.isUnsat()
        def (s1Model, s2Model) = evaluateModel(lhs.formula, rhs.formula)
        s1Model.length() == s1.length()
        s1Model.equalsIgnoreCase(s2Model) != expected

        cleanup:
        prover.pop()

        where:
        s1      | s2      | expected
        "hello" | "HELLO" | true
        "Hello" | "World" | false
    }
}
