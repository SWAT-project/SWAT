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

class ShortValueTest extends Specification {

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

    // ========== Short → Byte Tests ==========

    def "Short->Byte: basic value #shortVal -> #expectedByte"() {
        given:
        def s1 = new ShortValue(context, (short) shortVal)

        when:
        def b1 = s1.asByteValue()

        then:
        b1.concrete == (byte) expectedByte
        noExceptionThrown()

        where:
        shortVal | expectedByte
        0        | 0
        1        | 1
        127      | 127         // Max byte
        128      | -128        // Wraps to min byte
        255      | -1          // Wraps to -1
        256      | 0           // Wraps to 0
        -1       | -1
        -128     | -128        // Min byte
        -129     | 127         // Wraps to max byte
        32767    | -1          // Max short wraps
        -32768   | 0           // Min short wraps
    }

    def "Short->Byte: symbolic SAT test"() {
        given:
        def s1 = new ShortValue(context, (short) 42)
        s1.MAKE_SYMBOLIC("s2b_sat")

        when:
        def b1 = s1.asByteValue()
        // Constrain short: 10 <= s <= 50
        prover.addConstraint(imgr.greaterOrEquals(s1.formula, imgr.makeNumber(10)))
        prover.addConstraint(imgr.lessOrEquals(s1.formula, imgr.makeNumber(50)))
        // Require byte == 42 (possible!)
        prover.addConstraint(imgr.equal(b1.formula, imgr.makeNumber(42)))

        then:
        !prover.isUnsat()
        noExceptionThrown()
    }

    def "Short->Byte: symbolic UNSAT test"() {
        given:
        def s1 = new ShortValue(context, (short) 10)
        s1.MAKE_SYMBOLIC("s2b_unsat")

        when:
        def b1 = s1.asByteValue()
        // Constrain short: 10 <= s <= 20
        prover.addConstraint(imgr.greaterOrEquals(s1.formula, imgr.makeNumber(10)))
        prover.addConstraint(imgr.lessOrEquals(s1.formula, imgr.makeNumber(20)))
        // But require byte > 100 (impossible!)
        prover.addConstraint(imgr.greaterThan(b1.formula, imgr.makeNumber(100)))

        then:
        prover.isUnsat()
        noExceptionThrown()
    }

    // ========== Short → Char Tests ==========

    def "Short->Char: basic value #shortVal -> #expectedChar"() {
        given:
        def s1 = new ShortValue(context, (short) shortVal)

        when:
        def c1 = s1.asCharValue()

        then:
        c1.concrete == (char) expectedChar
        noExceptionThrown()

        where:
        shortVal | expectedChar
        0        | 0
        1        | 1
        100      | 100
        32767    | 32767       // Max short (positive)
        -1       | 65535       // Wraps to max char
        -100     | 65436       // Negative wraps to high char value
        -32768   | 32768       // Min short wraps to middle of char range
    }

    def "Short->Char: symbolic SAT test"() {
        given:
        def s1 = new ShortValue(context, (short) 1000)
        s1.MAKE_SYMBOLIC("s2c_sat")

        when:
        def c1 = s1.asCharValue()
        // Constrain short: 500 <= s <= 1500
        prover.addConstraint(imgr.greaterOrEquals(s1.formula, imgr.makeNumber(500)))
        prover.addConstraint(imgr.lessOrEquals(s1.formula, imgr.makeNumber(1500)))
        // Require char == 1000 (possible!)
        prover.addConstraint(imgr.equal(c1.formula, imgr.makeNumber(1000)))

        then:
        !prover.isUnsat()
        noExceptionThrown()
    }

    def "Short->Char: symbolic UNSAT test for negative short"() {
        given:
        def s1 = new ShortValue(context, (short) -100)
        s1.MAKE_SYMBOLIC("s2c_neg")

        when:
        def c1 = s1.asCharValue()
        // Constrain short to be negative: -200 <= s <= -50
        prover.addConstraint(imgr.greaterOrEquals(s1.formula, imgr.makeNumber(-200)))
        prover.addConstraint(imgr.lessOrEquals(s1.formula, imgr.makeNumber(-50)))
        // But require char < 1000 (impossible - negative shorts map to high chars!)
        prover.addConstraint(imgr.lessThan(c1.formula, imgr.makeNumber(1000)))

        then:
        prover.isUnsat()
        noExceptionThrown()
    }
}
