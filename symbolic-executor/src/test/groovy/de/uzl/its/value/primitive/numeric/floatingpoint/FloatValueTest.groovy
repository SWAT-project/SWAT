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
}