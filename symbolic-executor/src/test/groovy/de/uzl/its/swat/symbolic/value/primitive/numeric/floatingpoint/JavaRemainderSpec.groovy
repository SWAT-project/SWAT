package de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint

import org.sosy_lab.common.ShutdownManager
import org.sosy_lab.common.configuration.Configuration
import org.sosy_lab.common.log.BasicLogManager
import org.sosy_lab.java_smt.SolverContextFactory
import org.sosy_lab.java_smt.api.BooleanFormulaManager
import org.sosy_lab.java_smt.api.FloatingPointFormula
import org.sosy_lab.java_smt.api.FloatingPointFormulaManager
import org.sosy_lab.java_smt.api.FormulaType
import org.sosy_lab.java_smt.api.ProverEnvironment
import org.sosy_lab.java_smt.api.SolverContext
import spock.lang.Shared
import spock.lang.Specification

/**
 * The model of Java's % (JavaRemainder.exactPieces) against Java itself: where the quotient is at
 * most 1 in magnitude it must equal Java's result bit for bit (so -0.0 is not 0.0), elsewhere it
 * must be the fallback it is given.
 */
class JavaRemainderSpec extends Specification {

    @Shared SolverContext ctx
    @Shared FloatingPointFormulaManager fp
    @Shared BooleanFormulaManager bm

    static final List<Double> VALUES = [0.0d, -0.0d, 1.0d, -1.0d, 0.5d, -0.5d, 2.5d, -2.5d, 3.0d, -3.0d, 7.0d, -7.0d,
                                        360.0d, -360.0d, 359.9d, -359.9d, 719.9d, -719.9d, 720.0d, -1.0e-3d,
                                        1.0e300d, -1.0e300d, 1.0e-300d, 9.0e307d, 1.0e308d,
                                        Double.MIN_VALUE, -Double.MIN_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE,
                                        Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN, 5.5d, -5.5d, 1.5d]

    def setupSpec() {
        ctx = SolverContextFactory.createSolverContext(Configuration.defaultConfiguration(),
                BasicLogManager.create(Configuration.defaultConfiguration()), ShutdownManager.create().getNotifier(),
                SolverContextFactory.Solvers.Z3)
        fp = ctx.getFormulaManager().getFloatingPointFormulaManager()
        bm = ctx.getFormulaManager().getBooleanFormulaManager()
    }

    def cleanupSpec() {
        ctx.close()
    }

    /** Whether f equals g (bit for bit) in every model. */
    boolean always(FloatingPointFormula f, FloatingPointFormula g) {
        try (ProverEnvironment prover = ctx.newProverEnvironment()) {
            prover.addConstraint(bm.not(fp.assignment(f, g)))
            return prover.isUnsat()
        }
    }

    /** null if right, else a description of what is wrong. */
    String check(double x, double y, FormulaType.FloatingPointType t, double expected) {
        def other = fp.makeVariable("otherwise", t)
        def number = { double v -> Double.isNaN(v) ? fp.makeNaN(t) : fp.makeNumber(v, t) }
        def model = JavaRemainder.exactPieces(ctx, number(x), number(y), other)
        double ax = Math.abs(x), ay = Math.abs(y)
        boolean exact = !Double.isNaN(x) && !Double.isNaN(y) && (ax < ay || ax < 2 * ay)
        if (exact) {
            return always(model, number(expected)) ? null : "$x % $y should be $expected"
        }
        return always(model, other) ? null : "$x % $y should fall back"
    }

    def "double: Java's remainder where the quotient is at most 1, the fallback elsewhere"() {
        expect:
        def t = FormulaType.getDoublePrecisionFloatingPointType()
        def wrong = []
        for (double x : VALUES) for (double y : VALUES) {
            def w = check(x, y, t, x % y)
            if (w != null) wrong << w
        }
        wrong == []
    }

    def "float: Java's remainder where the quotient is at most 1, the fallback elsewhere"() {
        expect:
        def t = FormulaType.getSinglePrecisionFloatingPointType()
        def floats = (VALUES.collect { it as float } + [Float.MIN_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE]).unique()
        def wrong = []
        for (float x : floats) for (float y : floats) {
            def w = check(x as double, y as double, t, (x % y) as double)
            if (w != null) wrong << w
        }
        wrong == []
    }

    def "the remainder is symbolic: a solver finds an angle whose remainder is below -180"() {
        // sv-benchmarks PieSegment: cAngle = angle % 360 must be able to be negative.
        given:
        def t = FormulaType.getDoublePrecisionFloatingPointType()
        def angle = fp.makeVariable("angle", t)
        def r = JavaRemainder.exactPieces(ctx, angle, fp.makeNumber(360.0d, t), fp.makeNumber(17.0d, t))

        when:
        double found
        try (ProverEnvironment prover = ctx.newProverEnvironment(SolverContext.ProverOptions.GENERATE_MODELS)) {
            prover.addConstraint(fp.lessThan(r, fp.makeNumber(-180.0d, t)))
            assert !prover.isUnsat()
            found = prover.getModel().evaluate(angle).doubleValue()
        }

        then:
        found % 360.0d < -180.0d
    }
}
