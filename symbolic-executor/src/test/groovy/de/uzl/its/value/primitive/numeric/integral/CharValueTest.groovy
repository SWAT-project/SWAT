package de.uzl.its.value.primitive.numeric.integral

import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.ByteValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.CharValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.ShortValue
import org.sosy_lab.common.ShutdownManager
import org.sosy_lab.common.configuration.Configuration
import org.sosy_lab.common.log.BasicLogManager
import org.sosy_lab.java_smt.SolverContextFactory
import org.sosy_lab.java_smt.api.BooleanFormulaManager
import org.sosy_lab.java_smt.api.FormulaManager
import org.sosy_lab.java_smt.api.IntegerFormulaManager
import org.sosy_lab.java_smt.api.ProverEnvironment
import org.sosy_lab.java_smt.api.SolverContext
import spock.lang.Specification

class CharValueTest extends Specification {

    SolverContext context
    ProverEnvironment prover
    FormulaManager fmgr
    BooleanFormulaManager bmgr
    IntegerFormulaManager imgr

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
    }

    def cleanup() {
        prover.close()
        context.close()
    }

    // ========== Char → Byte Tests ==========

    def "Char->Byte: basic value #charVal -> #expectedByte"() {
        given:
        def c1 = new CharValue(context, (char) charVal)

        when:
        def b1 = c1.asByteValue()

        then:
        b1.concrete == (byte) expectedByte
        noExceptionThrown()

        where:
        charVal | expectedByte
        0       | 0
        1       | 1
        127     | 127         // Max byte
        128     | -128        // Wraps to min byte
        255     | -1          // Wraps to -1
        256     | 0           // Wraps to 0
        1000    | -24         // (1000 % 256 = 232, 232 - 256 = -24)
        32768   | 0           // Wraps
        65535   | -1          // Max char wraps to -1
    }

    def "Char->Byte: symbolic SAT test"() {
        given:
        def c1 = new CharValue(context, (char) 50)
        c1.MAKE_SYMBOLIC("c2b_sat")

        when:
        def b1 = c1.asByteValue()
        // Constrain char: 10 <= c <= 100
        prover.addConstraint(imgr.greaterOrEquals(c1.formula, imgr.makeNumber(10)))
        prover.addConstraint(imgr.lessOrEquals(c1.formula, imgr.makeNumber(100)))
        // Require byte == 50 (possible!)
        prover.addConstraint(imgr.equal(b1.formula, imgr.makeNumber(50)))

        then:
        !prover.isUnsat()
        noExceptionThrown()
    }

    def "Char->Byte: symbolic UNSAT test"() {
        given:
        def c1 = new CharValue(context, (char) 10)
        c1.MAKE_SYMBOLIC("c2b_unsat")

        when:
        def b1 = c1.asByteValue()
        // Constrain char: 10 <= c <= 20
        prover.addConstraint(imgr.greaterOrEquals(c1.formula, imgr.makeNumber(10)))
        prover.addConstraint(imgr.lessOrEquals(c1.formula, imgr.makeNumber(20)))
        // But require byte > 100 (impossible!)
        prover.addConstraint(imgr.greaterThan(b1.formula, imgr.makeNumber(100)))

        then:
        prover.isUnsat()
        noExceptionThrown()
    }

    // ========== Char → Short Tests ==========

    def "Char->Short: basic value #charVal -> #expectedShort"() {
        given:
        def c1 = new CharValue(context, (char) charVal)

        when:
        def s1 = c1.asShortValue()

        then:
        s1.concrete == (short) expectedShort
        noExceptionThrown()

        where:
        charVal | expectedShort
        0       | 0
        1       | 1
        100     | 100
        32767   | 32767       // Max short (positive)
        32768   | -32768      // Wraps to min short
        40000   | -25536      // (40000 - 65536 = -25536)
        65535   | -1          // Max char wraps to -1
    }

    def "Char->Short: symbolic SAT test"() {
        given:
        def c1 = new CharValue(context, (char) 1000)
        c1.MAKE_SYMBOLIC("c2s_sat")

        when:
        def s1 = c1.asShortValue()
        // Constrain char: 500 <= c <= 1500
        prover.addConstraint(imgr.greaterOrEquals(c1.formula, imgr.makeNumber(500)))
        prover.addConstraint(imgr.lessOrEquals(c1.formula, imgr.makeNumber(1500)))
        // Require short == 1000 (possible!)
        prover.addConstraint(imgr.equal(s1.formula, imgr.makeNumber(1000)))

        then:
        !prover.isUnsat()
        noExceptionThrown()
    }

    def "Char->Short: symbolic UNSAT test for high char values"() {
        given:
        def c1 = new CharValue(context, (char) 50000)
        c1.MAKE_SYMBOLIC("c2s_high")

        when:
        def s1 = c1.asShortValue()
        // Constrain char to high range: 40000 <= c <= 50000
        prover.addConstraint(imgr.greaterOrEquals(c1.formula, imgr.makeNumber(40000)))
        prover.addConstraint(imgr.lessOrEquals(c1.formula, imgr.makeNumber(50000)))
        // But require short > 0 (impossible - high chars map to negative shorts!)
        prover.addConstraint(imgr.greaterThan(s1.formula, imgr.makeNumber(0)))

        then:
        prover.isUnsat()
        noExceptionThrown()
    }

    def "Char->Short: symbolic SAT test for boundary"() {
        given:
        def c1 = new CharValue(context, (char) 32767)
        c1.MAKE_SYMBOLIC("c2s_boundary")

        when:
        def s1 = c1.asShortValue()
        // Constrain char: 32760 <= c <= 32770
        prover.addConstraint(imgr.greaterOrEquals(c1.formula, imgr.makeNumber(32760)))
        prover.addConstraint(imgr.lessOrEquals(c1.formula, imgr.makeNumber(32770)))
        // Require short in range that crosses the boundary
        prover.addConstraint(imgr.greaterOrEquals(s1.formula, imgr.makeNumber(-32768)))
        prover.addConstraint(imgr.lessOrEquals(s1.formula, imgr.makeNumber(32767)))

        then:
        !prover.isUnsat()
        noExceptionThrown()
    }
}
