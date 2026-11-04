package de.uzl.its.swat.symbolic.invoke

import de.uzl.its.swat.symbolic.invoke.java.lang.MathInvocation
import de.uzl.its.swat.symbolic.value.Value
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.LongValue
import org.sosy_lab.common.ShutdownManager
import org.sosy_lab.common.configuration.Configuration
import org.sosy_lab.common.log.BasicLogManager
import org.sosy_lab.java_smt.SolverContextFactory
import org.sosy_lab.java_smt.api.*
import spock.lang.Shared
import spock.lang.Specification

/**
 * The exact Math models agree with the JDK on every input: for symbolic arguments fixed to the
 * input, the model's result can only be the JDK's result (bit for bit, so NaN and -0.0 count).
 */
class MathInvocationSpec extends Specification {

    @Shared SolverContext context
    @Shared FloatingPointFormulaManager fpmgr
    @Shared BitvectorFormulaManager bvmgr

    static final List<Double> DOUBLES = [0.0d, -0.0d, 0.5d, -0.5d, 1.5d, -1.5d, 2.5d, -2.5d, 0.49999999999999994d,
            -0.49999999999999994d, 1e-300d, -1e-20d, 3.7d, -3.7d, 4503599627370495.5d, 4503599627370497d,
            9.223372036854775807E18d, -9.223372036854775808E18d, 1e300d, Double.MIN_VALUE, Double.MAX_VALUE,
            Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN]
    static final List<Float> FLOATS = [0.0f, -0.0f, 0.5f, -0.5f, 2.5f, -2.5f, 0.49999997f, -1e-20f, 3.7f, -3.7f,
            8388607.5f, 2.14748365E9f, -2.14748365E9f, Float.MIN_VALUE, Float.MAX_VALUE,
            Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NaN]
    static final List<Integer> INTS = [0, 1, -1, 7, -7, 3, -3, Integer.MAX_VALUE, Integer.MIN_VALUE]
    static final List<Long> LONGS = [0L, 1L, -1L, 7L, -7L, 3L, -3L, Long.MAX_VALUE, Long.MIN_VALUE]

    def setupSpec() {
        context = SolverContextFactory.createSolverContext(
                Configuration.defaultConfiguration(),
                BasicLogManager.create(Configuration.defaultConfiguration()),
                ShutdownManager.create().getNotifier(),
                SolverContextFactory.Solvers.Z3)
        fpmgr = context.getFormulaManager().getFloatingPointFormulaManager()
        bvmgr = context.getFormulaManager().getBitvectorFormulaManager()
    }

    def cleanupSpec() {
        context.close()
    }

    private int vars = 0

    /** A symbolic argument fixed to the value v by the returned constraint. */
    private List symbolic(Object v) {
        String name = "X" + (vars++)
        if (v instanceof Double) {
            def x = fpmgr.makeVariable(name, FormulaType.getDoublePrecisionFloatingPointType())
            return [new DoubleValue(context, v, x), fpmgr.assignment(x, constant(v))]
        } else if (v instanceof Float) {
            def x = fpmgr.makeVariable(name, FormulaType.getSinglePrecisionFloatingPointType())
            return [new FloatValue(context, v, x), fpmgr.assignment(x, constant(v))]
        } else if (v instanceof Long) {
            def x = bvmgr.makeVariable(64, name)
            return [new LongValue(context, v, x), bvmgr.equal(x, bvmgr.makeBitvector(64, v))]
        } else {
            def x = bvmgr.makeVariable(32, name)
            return [new IntValue(context, v as int, x), bvmgr.equal(x, bvmgr.makeBitvector(32, v as int))]
        }
    }

    private Formula constant(Object v) {
        if (v instanceof Double) return v.isNaN() ? fpmgr.makeNaN(FormulaType.getDoublePrecisionFloatingPointType()) : new DoubleValue(context, v).formula
        if (v instanceof Float) return v.isNaN() ? fpmgr.makeNaN(FormulaType.getSinglePrecisionFloatingPointType()) : new FloatValue(context, v).formula
        if (v instanceof Long) return bvmgr.makeBitvector(64, v)
        return bvmgr.makeBitvector(32, v as int)
    }

    /** Whether the model of name on the arguments agrees with expected, symbolically and concretely. */
    private boolean agrees(String name, List args, Object expected) {
        def fixed = args.collect { symbolic(it) }
        Value<?, ?> result = MathInvocation.invokeStaticMethod(name, fixed.collect { it[0] } as Value[], null, null, false)
        def bmgr = context.getFormulaManager().getBooleanFormulaManager()
        def differs = result.formula instanceof FloatingPointFormula
                ? bmgr.not(fpmgr.assignment(result.formula, constant(expected)))
                : bmgr.not(bvmgr.equal(result.formula, constant(expected)))
        def prover = context.newProverEnvironment()
        try {
            fixed.each { prover.addConstraint(it[1]) }
            prover.addConstraint(differs)
            boolean concreteOk = (expected instanceof Double || expected instanceof Float)
                    ? Double.compare(((Number) result.concrete).doubleValue(), ((Number) expected).doubleValue()) == 0
                    : result.concrete == expected
            return prover.isUnsat() && concreteOk
        } finally {
            prover.close()
        }
    }

    def "round(double) rounds ties toward positive infinity and saturates"() {
        expect:
        DOUBLES.every { agrees("round", [it], Math.round(it)) }
    }

    def "round(float) rounds ties toward positive infinity and saturates"() {
        expect:
        FLOATS.every { agrees("round", [it], Math.round(it)) }
    }

    def "max and min of doubles keep NaN and order the zeros"() {
        expect:
        [DOUBLES, DOUBLES].combinations().every { a, b ->
            agrees("max", [a, b], Math.max(a, b)) && agrees("min", [a, b], Math.min(a, b))
        }
    }

    def "max and min of floats keep NaN and order the zeros"() {
        expect:
        [FLOATS, FLOATS].combinations().every { a, b ->
            agrees("max", [a, b], Math.max(a, b)) && agrees("min", [a, b], Math.min(a, b))
        }
    }

    def "signum"() {
        expect:
        DOUBLES.every { agrees("signum", [it], Math.signum(it)) }
        FLOATS.every { agrees("signum", [it], Math.signum(it)) }
    }

    def "floorDiv and floorMod for a concrete divisor"() {
        expect:
        [INTS, INTS.findAll { it != 0 }].combinations().every { a, b ->
            agreesWithConcreteDivisor("floorDiv", a, b, Math.floorDiv(a, b)) && agreesWithConcreteDivisor("floorMod", a, b, Math.floorMod(a, b))
        }
        [LONGS, LONGS.findAll { it != 0 }].combinations().every { a, b ->
            agreesWithConcreteDivisor("floorDiv", a, b, Math.floorDiv(a, b)) && agreesWithConcreteDivisor("floorMod", a, b, Math.floorMod(a, b))
        }
    }

    private boolean agreesWithConcreteDivisor(String name, Object a, Object b, Object expected) {
        def (x, fixed) = symbolic(a)
        def y = b instanceof Long ? new LongValue(context, b) : new IntValue(context, b as int)
        Value<?, ?> result = MathInvocation.invokeStaticMethod(name, [x, y] as Value[], null, null, false)
        def prover = context.newProverEnvironment()
        try {
            prover.addConstraint(fixed)
            prover.addConstraint(context.getFormulaManager().getBooleanFormulaManager().not(bvmgr.equal(result.formula, constant(expected))))
            return prover.isUnsat() && result.concrete == expected
        } finally {
            prover.close()
        }
    }

    def "floorDiv with a symbolic divisor stays unmodelled"() {
        given:
        def (x, fx) = symbolic(7)
        def (y, fy) = symbolic(2)

        expect:
        !(MathInvocation.invokeStaticMethod("floorDiv", [x, y] as Value[], null, null, false) instanceof IntValue)
    }
}
