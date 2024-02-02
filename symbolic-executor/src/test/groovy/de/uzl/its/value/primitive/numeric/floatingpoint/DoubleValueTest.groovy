package de.uzl.its.value.primitive.numeric.floatingpoint

import de.uzl.its.symbolic.value.primitive.numeric.floatingpoint.DoubleValue
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
import de.uzl.its.symbolic.value.primitive.numeric.floatingpoint.FloatValue

class DoubleValueTest extends Specification {

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
    ArrayList<Double> testValues = [
            Double.MIN_VALUE,
            (Double.MIN_VALUE + 1.1d),
            -9679678.23d,
            -7.01d,
            -2.0d,
            -1.23456d,
            0.0d,
            11.23456d,
            2.0d,
            7.01d,
            9786967.23d,
            (Double.MAX_VALUE - 1.1d),
            Double.MAX_VALUE
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


    def "DADD"(double c1, double c2, double res) {


        given:
        def d1 = new DoubleValue(context, c1)
        def d2 = new DoubleValue(context, c2)

        when:

        def d3 = d1.DADD(d2)
        prover.addConstraint(fpmgr.equalWithFPSemantics(d3.formula, fpmgr.makeNumber(res, DoubleValue.precision)))
        then:
        !prover.isUnsat()
        noExceptionThrown()
        d3.concrete == res


        where:
        c1 << leftAssignment
        c2 << rightAssignment
        res << [
                leftAssignment,
                rightAssignment
        ].transpose().collect { it[0] + it[1] }
    }

    def "DSUB"(double c1, double c2, double res) {


        given:
        def d1 = new DoubleValue(context, c1)
        def d2 = new DoubleValue(context, c2)

        when:

        def d3 = d1.DSUB(d2)
        prover.addConstraint(fpmgr.equalWithFPSemantics(d3.formula, fpmgr.makeNumber(res, DoubleValue.precision)))
        then:
        !prover.isUnsat()
        noExceptionThrown()
        d3.concrete == res


        where:
        c1 << leftAssignment
        c2 << rightAssignment
        res << [
                leftAssignment,
                rightAssignment
        ].transpose().collect { it[0] - it[1] }
    }

    def "DMUL"(double c1, double c2, double res) {


        given:
        def d1 = new DoubleValue(context, c1)
        def d2 = new DoubleValue(context, c2)

        when:

        def d3 = d1.DMUL(d2)
        prover.addConstraint(fpmgr.equalWithFPSemantics(d3.formula, fpmgr.makeNumber(res, DoubleValue.precision)))
        then:
        !prover.isUnsat()
        noExceptionThrown()
        d3.concrete == res


        where:
        c1 << leftAssignment
        c2 << rightAssignment
        res << [
                leftAssignment,
                rightAssignment
        ].transpose().collect { it[0] * it[1] }
    }

    def "DDIV"(double c1, double c2, double res) {


        given:
        def d1 = new DoubleValue(context, c1)
        def d2 = new DoubleValue(context, c2)

        when:
        def d3 = d1.DDIV(d2)
        prover.addConstraint(fpmgr.equalWithFPSemantics(d3.formula, fpmgr.makeNumber(res, DoubleValue.precision)))
        println fmgr.dumpFormula(fpmgr.equalWithFPSemantics(d3.formula, fpmgr.makeNumber(res, DoubleValue.precision)))
        println c1
        println c2
        println res


        then:
        if(Double.isNaN(res)){
            assert prover.isUnsat()
        } else {
            assert !prover.isUnsat()
        }
        d3.concrete == res

        where:
        c1 << leftAssignment
        c2 << rightAssignment
        res << [
                leftAssignment,
                rightAssignment
        ].transpose().collect { it[0] / it[1] }
    }


    def "DREM"(double c1, double c2, double res) {


        given:
        def d1 = new DoubleValue(context, c1)
        def d2 = new DoubleValue(context, c2)

        when:
            def d3 = d1.DREM(d2)
            prover.addConstraint(fpmgr.equalWithFPSemantics(d3.formula, fpmgr.makeNumber(res, DoubleValue.precision)))
            println fmgr.dumpFormula(fpmgr.equalWithFPSemantics(d3.formula, fpmgr.makeVariable('float', DoubleValue.precision)))
            println fmgr.dumpFormula(fpmgr.equalWithFPSemantics(d3.formula, fpmgr.makeNumber(res, DoubleValue.precision)))


        then:
            if(Double.isNaN(res)){
                assert prover.isUnsat()
            } else {
                assert !prover.isUnsat()
            }
            d3.concrete == res

        where:
        c1 << leftAssignment
        c2 << rightAssignment
        res << [
                leftAssignment,
                rightAssignment
        ].transpose().collect { it[0] % it[1] }
    }

    def "DNEG"(double c1, double res) {


        given:
        def d1 = new DoubleValue(context, c1)

        when:

        def d2 = d1.DNEG()
        prover.addConstraint(fpmgr.equalWithFPSemantics(d2.formula, fpmgr.makeNumber(res, DoubleValue.precision)))
        then:
        !prover.isUnsat()
        noExceptionThrown()
        d2.concrete == res


        where:
        c1 << testValues
        res << testValues.collect {-it}
    }


    def "DCMPG"(double c1, double c2, int res) {


        given:
        def d1 = new DoubleValue(context, c1)
        def d2 = new DoubleValue(context, c2)

        when:

        def i1 = d1.DCMPG(d2)
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
        ].transpose().collect { it[0] == it[1] ? 0 : (it[0] > it[1] ? 1 : -1) }
    }

    def "DCMPL"(double c1, double c2, int res) {


        given:
        def d1 = new DoubleValue(context, c1)
        def d2 = new DoubleValue(context, c2)

        when:

        def i1 = d1.DCMPL(d2)
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
        ].transpose().collect { it[0] == it[1] ? 0 : (it[0] > it[1] ? 1 : -1) }
    }

    def "D2I"(long c1, int res) {


        given:
        def d1 = new DoubleValue(context, c1)

        when:
        def i1 = d1.D2I()
        prover.addConstraint(imgr.equal(i1.formula, imgr.makeNumber(res)))
        then:
        !prover.isUnsat()
        i1.concrete == res


        where:
        c1 << testValues
        res << testValues.collect { (int) it }
    }
    def "D2F"(double c1, float res) {


        given:
        def d1 = new DoubleValue(context, c1)

        when:
        def f1 = d1.D2F()
        prover.addConstraint(fpmgr.equalWithFPSemantics(f1.formula, fpmgr.makeNumber(res, FloatValue.precision)))
        then:
        !prover.isUnsat()
        f1.concrete == res


        where:
        c1 << testValues
        res << testValues.collect { (float) it }
    }


    def "D2L"(long c1, long res) {


        given:
        def d1 = new DoubleValue(context, c1)

        when:
        def l1 = d1.D2L()
        prover.addConstraint(imgr.equal(l1.formula, imgr.makeNumber(res)))
        then:
        !prover.isUnsat()
        l1.concrete == res


        where:
        c1 << testValues
        res << testValues.collect { (long) it }
    }
}
