package de.uzl.its.value.primitive.numeric.floatingpoint

import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue
import org.sosy_lab.common.ShutdownManager
import org.sosy_lab.common.configuration.Configuration
import org.sosy_lab.common.log.BasicLogManager
import org.sosy_lab.java_smt.SolverContextFactory
import org.sosy_lab.java_smt.api.BooleanFormulaManager
import org.sosy_lab.java_smt.api.FloatingPointFormulaManager
import org.sosy_lab.java_smt.api.FormulaManager
import org.sosy_lab.java_smt.api.IntegerFormulaManager
import org.sosy_lab.java_smt.api.ProverEnvironment
import org.sosy_lab.java_smt.api.SolverContext
import spock.lang.Shared
import spock.lang.Specification

class FloatValueTest extends Specification {

    SolverContext context
    ProverEnvironment prover
    FormulaManager fmgr
    BooleanFormulaManager bmgr
    IntegerFormulaManager imgr
    FloatingPointFormulaManager fpmgr

    def setup() {

        context = SolverContextFactory.createSolverContext(
                Configuration.defaultConfiguration(),
                BasicLogManager.create(Configuration.defaultConfiguration()),
                ShutdownManager.create().getNotifier(),
                SolverContextFactory.Solvers.Z3)
        prover = context.newProverEnvironment(
                SolverContext.ProverOptions.GENERATE_MODELS)
        fmgr = context.getFormulaManager()
        bmgr = fmgr.getBooleanFormulaManager()
        imgr = fmgr.getIntegerFormulaManager()
        fpmgr = fmgr.getFloatingPointFormulaManager()
    }

    def cleanup() {
        prover.close()
        context.close()
    }

    @Shared
    ArrayList<Float> testValues = [
            Float.NEGATIVE_INFINITY,
            Float.MIN_VALUE,
            (Float.MIN_VALUE + 1.1f),
            -9679678.23f,
            -7.01f,
            -2.0f,
            -1.23456f,
            0.0f,
            Float.NaN,
            11.23456f,
            2.0f,
            7.01f,
            9786967.23f,
            (Float.MAX_VALUE - 1.1f),
            Float.MAX_VALUE,
            Float.POSITIVE_INFINITY
    ]
    @Shared
    ArrayList<Double> leftAssignment = testValues * testValues.size()
    @Shared
    ArrayList<Double> rightAssignment = []

    def setupSpec() {
        testValues.each {
            rightAssignment = rightAssignment + [it] * testValues.size()
        }
    }


    def "FADD"(float c1, float c2, float res) {

        given:
        def f1 = new FloatValue(context, c1)
        def f2 = new FloatValue(context, c2)

        when:

        def f3 = f1.FADD(f2)
        prover.addConstraint(fpmgr.equalWithFPSemantics(f3.formula, fpmgr.makeNumber(res, FloatValue.precision)))
        println fmgr.dumpFormula(fpmgr.equalWithFPSemantics(f3.formula, fpmgr.makeNumber(res, FloatValue.precision)))
        then:
        if(Float.isNaN(res)){
            assert prover.isUnsat()
        } else{
            assert !prover.isUnsat()
        }
        noExceptionThrown()
        f3.concrete == res


        where:
        c1 << leftAssignment
        c2 << rightAssignment
        res << [leftAssignment,rightAssignment].transpose().collect { it[0] + it[1] }
    }

    def "FSUB"(float c1, float c2, float res) {

        given:
        def f1 = new FloatValue(context, c1)
        def f2 = new FloatValue(context, c2)

        when:
        def f3 = f1.FSUB(f2)
        prover.addConstraint(fpmgr.equalWithFPSemantics(f3.formula, fpmgr.makeNumber(res, FloatValue.precision)))

        then:
        if(Float.isNaN(res)){
            assert prover.isUnsat()
        } else{
            assert !prover.isUnsat()
        }
        noExceptionThrown()
        f3.concrete == res

        where:
        c1 << leftAssignment
        c2 << rightAssignment
        res << [leftAssignment,rightAssignment].transpose().collect { it[0] - it[1] }
    }

    def "FDIV"(float c1, float c2, float res) {

        given:
        def f1 = new FloatValue(context, c1)
        def f2 = new FloatValue(context, c2)

        when:
        def f3 = f1.FDIV(f2)
        prover.addConstraint(fpmgr.equalWithFPSemantics(f3.formula, fpmgr.makeNumber(res, FloatValue.precision)))

        then:
        if(Float.isNaN(res)){
            assert prover.isUnsat()
        } else{
            assert !prover.isUnsat()
        }
        noExceptionThrown()
        f3.concrete == res

        where:
        c1 << leftAssignment
        c2 << rightAssignment
        res << [leftAssignment,rightAssignment].transpose().collect { it[0] / it[1] }
    }

    def "FMUL"(float c1, float c2, float res) {

        given:
        def f1 = new FloatValue(context, c1)
        def f2 = new FloatValue(context, c2)

        when:
        def f3 = f1.FMUL(f2)
        prover.addConstraint(fpmgr.equalWithFPSemantics(f3.formula, fpmgr.makeNumber(res, FloatValue.precision)))

        then:
        if(Float.isNaN(res)){
            assert prover.isUnsat()
        } else{
            assert !prover.isUnsat()
        }
        noExceptionThrown()
        f3.concrete == res

        where:
        c1 << leftAssignment
        c2 << rightAssignment
        res << [leftAssignment,rightAssignment].transpose().collect { it[0] * it[1] }
    }

    def "FNEG"(float c1, float res) {

        given:
        def f1 = new FloatValue(context, c1)

        when:
        def f2 = f1.FNEG()
        prover.addConstraint(fpmgr.equalWithFPSemantics(f2.formula, fpmgr.makeNumber(res, FloatValue.precision)))

        then:
        if(Float.isNaN(res)){
            assert prover.isUnsat()
        } else{
            assert !prover.isUnsat()
        }
        noExceptionThrown()
        f2.concrete == res

        where:
        c1 << testValues
        res << testValues.collect { -it }
    }

    def "FREM"(float c1, float c2, float res) {

        given:
        def f1 = new FloatValue(context, c1)
        def f2 = new FloatValue(context, c2)

        when:
        def f3 = f1.FREM(f2)
        prover.addConstraint(fpmgr.equalWithFPSemantics(f3.formula, fpmgr.makeNumber(res, FloatValue.precision)))

        then:
        if(Float.isNaN(res)){
            assert prover.isUnsat()
        } else{
            assert !prover.isUnsat()
        }
        noExceptionThrown()
        f3.concrete == res

        where:
        c1 << leftAssignment
        c2 << rightAssignment
        res << [leftAssignment,rightAssignment].transpose().collect { it[0] % it[1] }
    }

    def "F2I"(float c1, int res) {

        given:
        def f1 = new FloatValue(context, c1)

        when:
        def i1 = f1.F2I()
        prover.addConstraint(imgr.equal(i1.formula, imgr.makeNumber(res)))

        then:
        !prover.isUnsat()
        noExceptionThrown()
        i1.concrete == res

        where:
        c1 << testValues
        res << testValues.collect { (int) it }
    }

    def "F2D"(float c1, double res) {

        given:
        def f1 = new FloatValue(context, c1)

        when:
        def d1 = f1.F2D()
        prover.addConstraint(fpmgr.equalWithFPSemantics(d1.formula, fpmgr.makeNumber(res, DoubleValue.precision)))

        then:
        if(Double.isNaN(res)){
            assert prover.isUnsat()
        } else{
            assert !prover.isUnsat()
        }
        noExceptionThrown()
        d1.concrete == res

        where:
        c1 << testValues
        res << testValues.collect { (double) it }
    }

    def "F2L"(float c1, long res) {

        given:
        def f1 = new FloatValue(context, c1)

        when:
        def l1 = f1.F2L()
        prover.addConstraint(imgr.equal(l1.formula, imgr.makeNumber(res)))

        then:
        !prover.isUnsat()
        noExceptionThrown()
        l1.concrete == res

        where:
        c1 << testValues
        res << testValues.collect { (long) it }
    }
    def "FCMPG"(float c1, float c2, int res) {


        given:
        def f1 = new FloatValue(context, c1)
        def f2 = new FloatValue(context, c2)

        when:

        def i1 = f1.FCMPG(f2)
        prover.addConstraint(imgr.equal(i1.formula, imgr.makeNumber(res)))
        then:
        !prover.isUnsat()
        noExceptionThrown()
        i1.concrete == res


        where:
        c1 << leftAssignment
        c2 << rightAssignment
        res << [
                leftAssignment,
                rightAssignment
        ].transpose().collect { (it[0] == Float.NaN || it[1] == Float.NaN) ? 1 : (it[0] == it[1] ? 0 : (it[0] > it[1] ? 1 : -1)) }
    }

    def "FCMPL"(float c1, float c2, int res) {


        given:
        def f1 = new FloatValue(context, c1)
        def f2 = new FloatValue(context, c2)

        when:

        def i1 = f1.FCMPL(f2)
        prover.addConstraint(imgr.equal(i1.formula, imgr.makeNumber(res)))
        then:
        !prover.isUnsat()
        noExceptionThrown()
        i1.concrete == res


        where:
        c1 << leftAssignment
        c2 << rightAssignment
        res << [
                leftAssignment,
                rightAssignment
        ].transpose().collect { (it[0] == Float.NaN || it[1] == Float.NaN) ? -1 : (it[0] == it[1] ? 0 : (it[0] > it[1] ? 1 : -1)) }
    }

    // ========================================================================
    // Float Narrowing Conversions - Testing fpToIntFormula Fix
    // ========================================================================

    def "Float->Byte: basic values"(float c1, byte res) {
        given:
        def f1 = new FloatValue(context, c1)

        when:
        def b1 = f1.asByteValue()
        prover.addConstraint(imgr.equal(b1.formula, imgr.makeNumber(res)))

        then:
        !prover.isUnsat()
        b1.concrete == res

        where:
        c1       || res
        0.0f     || 0
        1.5f     || 1
        -1.5f    || -1
        42.7f    || 42
        -42.3f   || -42
        127.0f   || 127
        -128.0f  || -128
        255.0f   || -1  // Overflow wraps
        256.0f   || 0   // Overflow wraps
    }

    def "Float->Byte: special values"(float c1, byte res) {
        given:
        def f1 = new FloatValue(context, c1)

        when:
        def b1 = f1.asByteValue()
        prover.addConstraint(imgr.equal(b1.formula, imgr.makeNumber(res)))

        then:
        !prover.isUnsat()
        b1.concrete == res

        where:
        c1                          || res
        Float.NaN                   || 0
        Float.POSITIVE_INFINITY     || -1   // MAX_VALUE wraps to byte
        Float.NEGATIVE_INFINITY     || 0    // MIN_VALUE wraps to byte
    }

    def "Float->Byte: symbolic UNSAT test"() {
        given:
        def f1 = new FloatValue(context, 50.0f)
        f1.MAKE_SYMBOLIC("f2b_unsat")

        when:
        def b1 = f1.asByteValue()
        // Constrain float: 10.0 <= f <= 20.0 (truncates to integers 10-20, all fit in byte)
        prover.addConstraint(fpmgr.greaterOrEquals(f1.formula, fpmgr.makeNumber(10.0f, FloatValue.precision)))
        prover.addConstraint(fpmgr.lessOrEquals(f1.formula, fpmgr.makeNumber(20.0f, FloatValue.precision)))
        // But require byte > 100 (impossible!)
        prover.addConstraint(imgr.greaterThan(b1.formula, imgr.makeNumber(100)))

        then:
        prover.isUnsat()
    }

    def "Float->Short: basic values"(float c1, short res) {
        given:
        def f1 = new FloatValue(context, c1)

        when:
        def s1 = f1.asShortValue()
        prover.addConstraint(imgr.equal(s1.formula, imgr.makeNumber(res)))

        then:
        !prover.isUnsat()
        s1.concrete == res

        where:
        c1          || res
        0.0f        || 0
        1.5f        || 1
        -1.5f       || -1
        1000.5f     || 1000
        -1000.5f    || -1000
        32767.0f    || 32767
        -32768.0f   || -32768
        65535.0f    || -1      // Overflow wraps
        65536.0f    || 0       // Overflow wraps
    }

    def "Float->Short: special values"(float c1, short res) {
        given:
        def f1 = new FloatValue(context, c1)

        when:
        def s1 = f1.asShortValue()
        prover.addConstraint(imgr.equal(s1.formula, imgr.makeNumber(res)))

        then:
        !prover.isUnsat()
        s1.concrete == res

        where:
        c1                          || res
        Float.NaN                   || 0
        Float.POSITIVE_INFINITY     || -1   // MAX_VALUE wraps to short
        Float.NEGATIVE_INFINITY     || 0    // MIN_VALUE wraps to short
    }

    def "Float->Short: symbolic UNSAT test"() {
        given:
        def f1 = new FloatValue(context, 500.0f)
        f1.MAKE_SYMBOLIC("f2s_unsat")

        when:
        def s1 = f1.asShortValue()
        // Constrain float: 100.0 <= f <= 200.0
        prover.addConstraint(fpmgr.greaterOrEquals(f1.formula, fpmgr.makeNumber(100.0f, FloatValue.precision)))
        prover.addConstraint(fpmgr.lessOrEquals(f1.formula, fpmgr.makeNumber(200.0f, FloatValue.precision)))
        // But require short > 10000 (impossible!)
        prover.addConstraint(imgr.greaterThan(s1.formula, imgr.makeNumber(10000)))

        then:
        prover.isUnsat()
    }

    def "Float->Char: basic values"(float c1, char res) {
        given:
        def f1 = new FloatValue(context, c1)

        when:
        def ch1 = f1.asCharValue()
        prover.addConstraint(imgr.equal(ch1.formula, imgr.makeNumber((int)res)))

        then:
        !prover.isUnsat()
        ch1.concrete == res

        where:
        c1          || res
        0.0f        || '\u0000'
        65.0f       || 'A'
        90.5f       || 'Z'
        1000.5f     || '\u03E8'
        65535.0f    || '\uFFFF'
        65536.0f    || '\u0000'  // Overflow wraps (unsigned)
        -1.0f       || '\uFFFF'  // Negative wraps to unsigned
    }

    def "Float->Char: special values"(float c1, char res) {
        given:
        def f1 = new FloatValue(context, c1)

        when:
        def ch1 = f1.asCharValue()
        prover.addConstraint(imgr.equal(ch1.formula, imgr.makeNumber((int)res)))

        then:
        !prover.isUnsat()
        ch1.concrete == res

        where:
        c1                          || res
        Float.NaN                   || '\u0000'
        Float.POSITIVE_INFINITY     || '\uFFFF'  // MAX_VALUE wraps to char
        Float.NEGATIVE_INFINITY     || '\u0000'  // MIN_VALUE wraps to char
    }

    def "Float->Char: symbolic SAT test"() {
        given:
        def f1 = new FloatValue(context, 65.0f)
        f1.MAKE_SYMBOLIC("f2c_sat")

        when:
        def ch1 = f1.asCharValue()
        // Constrain float: 65.0 <= f <= 90.0 (ASCII 'A' to 'Z')
        prover.addConstraint(fpmgr.greaterOrEquals(f1.formula, fpmgr.makeNumber(65.0f, FloatValue.precision)))
        prover.addConstraint(fpmgr.lessOrEquals(f1.formula, fpmgr.makeNumber(90.0f, FloatValue.precision)))
        // Require char in valid range (65-90, 'A'-'Z')
        prover.addConstraint(imgr.greaterOrEquals(ch1.formula, imgr.makeNumber(65)))
        prover.addConstraint(imgr.lessOrEquals(ch1.formula, imgr.makeNumber(90)))

        then:
        !prover.isUnsat()
    }
}