package de.uzl.its.value.primitive.numeric.integral

import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.ByteValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.CharValue
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

class ByteValueTest extends Specification {

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

    // ========== Byte → Char Tests ==========

    def "Byte->Char: basic value #byteVal -> #expectedChar"() {
        given:
        def b1 = new ByteValue(context, (byte) byteVal)

        when:
        def c1 = b1.asCharValue()

        then:
        c1.concrete == (char) expectedChar
        noExceptionThrown()

        where:
        byteVal | expectedChar
        0       | 0
        1       | 1
        127     | 127         // Max byte
        -1      | 65535       // -1 wraps to max char
        -128    | 65408       // Min byte wraps to high char value
        50      | 50
        -50     | 65486       // Negative bytes wrap to high char values
    }

    def "Byte->Char: symbolic SAT test for positive byte"() {
        given:
        def b1 = new ByteValue(context, (byte) 50)
        b1.MAKE_SYMBOLIC("b2c_pos")

        when:
        def c1 = b1.asCharValue()
        // Constrain byte: 10 <= b <= 100
        prover.addConstraint(imgr.greaterOrEquals(b1.formula, imgr.makeNumber(10)))
        prover.addConstraint(imgr.lessOrEquals(b1.formula, imgr.makeNumber(100)))
        // Require char == 50 (possible!)
        prover.addConstraint(imgr.equal(c1.formula, imgr.makeNumber(50)))

        then:
        !prover.isUnsat()
        noExceptionThrown()
    }

    def "Byte->Char: symbolic SAT test for negative byte"() {
        given:
        def b1 = new ByteValue(context, (byte) -50)
        b1.MAKE_SYMBOLIC("b2c_neg")

        when:
        def c1 = b1.asCharValue()
        // Constrain byte: -100 <= b <= -10
        prover.addConstraint(imgr.greaterOrEquals(b1.formula, imgr.makeNumber(-100)))
        prover.addConstraint(imgr.lessOrEquals(b1.formula, imgr.makeNumber(-10)))
        // Negative bytes should map to high char values (> 65000)
        prover.addConstraint(imgr.greaterThan(c1.formula, imgr.makeNumber(65000)))

        then:
        !prover.isUnsat()
        noExceptionThrown()
    }

    def "Byte->Char: symbolic UNSAT test"() {
        given:
        def b1 = new ByteValue(context, (byte) -50)
        b1.MAKE_SYMBOLIC("b2c_unsat")

        when:
        def c1 = b1.asCharValue()
        // Constrain byte to be negative: -100 <= b <= -10
        prover.addConstraint(imgr.greaterOrEquals(b1.formula, imgr.makeNumber(-100)))
        prover.addConstraint(imgr.lessOrEquals(b1.formula, imgr.makeNumber(-10)))
        // But require char < 1000 (impossible - negative bytes map to high chars!)
        prover.addConstraint(imgr.lessThan(c1.formula, imgr.makeNumber(1000)))

        then:
        prover.isUnsat()
        noExceptionThrown()
    }

    def "Byte->Char: verifies concrete values match symbolic formula"() {
        given:
        def b1 = new ByteValue(context, (byte) byteVal)

        when:
        def c1 = b1.asCharValue()
        // Add constraint that formula equals the concrete value (cast char to int for makeNumber)
        prover.addConstraint(imgr.equal(c1.formula, imgr.makeNumber((int) ((char) c1.concrete))))

        then:
        !prover.isUnsat()
        noExceptionThrown()

        where:
        byteVal << [0, 1, 127, -1, -128, 50, -50, 100, -100]
    }
}
