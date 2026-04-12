package de.uzl.its.value.reference.lang

import de.uzl.its.swat.symbolic.value.Value
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.BooleanValue
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue
import de.uzl.its.swat.thread.ThreadHandler
import org.objectweb.asm.Type
import org.sosy_lab.java_smt.api.*
import spock.lang.Specification
import spock.lang.Unroll

import java.util.regex.Pattern

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

    // regexes reused across tests
    private static final Pattern DEF_S1 = Pattern.compile("\\(define-fun\\s+s1\\s*\\(\\)\\s*String\\s*\"([^\"]*)\"", Pattern.DOTALL)
    private static final Pattern DEF_S2 = Pattern.compile("\\(define-fun\\s+s2\\s*\\(\\)\\s*String\\s*\"([^\"]*)\"", Pattern.DOTALL)

    def setup() {
        ThreadHandler.init()
        ThreadHandler.addThreadContext(currentThread().id)
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
        ThreadHandler.getSymbolicTraceHandler(currentThread().id).getUFs().each { prover.addConstraint(it) }
    }

    /** Force the SMT result to disagree with the concrete Java result. */
    private void forceMismatch(BooleanFormula smtResult, boolean expectedJava) {
        if (expectedJava) {
            prover.addConstraint(bmgr.not(smtResult))
        } else {
            prover.addConstraint(smtResult)
        }
    }

    private static String modelString(ProverEnvironment p) {
        // Only convert to String right here, never let Groovy stringify the Model implicitly.
        return p.getModel().toString()
    }

    private static String extractOrNull(Pattern pat, String text) {
        def m = pat.matcher(text)
        return m.find() ? m.group(1) : null
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
        def model = modelString(prover)
        def s1Model = extractOrNull(DEF_S1, model)
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
        def model = modelString(prover)
        def s1Model = extractOrNull(DEF_S1, model)
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
        def model = modelString(prover)
        def s1Model = extractOrNull(DEF_S1, model)
        def s2Model = extractOrNull(DEF_S2, model) ?: s2
        assert s1Model != null
        assert (s1Model.equalsIgnoreCase(s2Model)) != expected

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
    def "equalsIgnoreCase(String anotherString) - Two symbolic strings - enforced length" (String s1, String s2, boolean expected) {
        setup:
        prover.push()
        def lhs  = new StringValue(context, s1, smgr.makeVariable("s1"), -1)
        def rhs  = new StringValue(context, s2, smgr.makeVariable("s2"), -1)
        def args = [rhs] as Value<?, ?>[]

        when:
        def result = (BooleanValue) lhs.invokeMethod("equalsIgnoreCase", STRING_DESC, args)
        addUFConstraintsFromTrace()
        forceMismatch(result.formula, expected)
        // |s1| = 10
        prover.addConstraint(imgr.equal(smgr.length(lhs.formula), imgr.makeNumber(10)))

        then:
        !prover.isUnsat()
        result.concrete == expected
        noExceptionThrown()

        and:
        def model = modelString(prover)
        def s1Model = extractOrNull(DEF_S1, model)
        def s2Model = extractOrNull(DEF_S2, model) ?: s2
        assert s1Model != null
        assert s1Model.length() == 10
        assert (s1Model.equalsIgnoreCase(s2Model)) != expected

        cleanup:
        prover.pop()

        where:
        s1      | s2      | expected
        "hello" | "HELLO" | true
        "Hello" | "bar"   | false
    }
}
