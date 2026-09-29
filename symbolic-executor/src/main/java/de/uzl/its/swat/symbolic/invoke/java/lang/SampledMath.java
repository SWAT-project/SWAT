package de.uzl.its.swat.symbolic.invoke.java.lang;

import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

/**
 * Approximate symbolic models of transcendental Math functions (tan, log, pow, ...), which have no
 * exact encoding in floating-point SMT.
 *
 * <p>The model is a lookup table: exact at a fixed set of sample inputs, and the concrete result of
 * this execution everywhere else. Where the result was concrete before, it still is, so the model
 * never constrains a path more than concretization did; but the solver can now pick a sample
 * input to reach a branch that needs a particular result. Samples are "nice" inputs plus inputs at
 * which the function hits "nice" outputs, found through its inverse.
 *
 * <p>Using a model records precision loss: an exhausted search is then no proof of safety.
 */
final class SampledMath {

    private static final double[] NICE_INPUTS = {
        0.0, -0.0, 0.5, -0.5, 1, -1, 2, -2, 3, -3, 4, -4, 5, -5, 10, -10, 100, -100, 1000, 0.1, -0.1,
        Math.PI, -Math.PI, Math.PI / 2, -Math.PI / 2, Math.PI / 3, Math.PI / 4, -Math.PI / 4, Math.PI / 6, Math.E
    };

    private static final double[] NICE_OUTPUTS = {0.0, 0.5, -0.5, 1, -1, 2, -2, 3, -3, 5, -5, 10, -10, 100};

    /** Grid for functions of two symbolic arguments. */
    private static final double[] GRID = {-2, -1, -0.5, 0, 0.5, 1, 2, 3, 4, 10};

    private record Unary(DoubleUnaryOperator f, DoubleUnaryOperator strictF, DoubleUnaryOperator inverse) {}

    private static final Map<String, Unary> UNARY = Map.of(
            "tan", new Unary(Math::tan, StrictMath::tan, Math::atan),
            "atan", new Unary(Math::atan, StrictMath::atan, Math::tan),
            "asin", new Unary(Math::asin, StrictMath::asin, Math::sin),
            "acos", new Unary(Math::acos, StrictMath::acos, Math::cos),
            "log", new Unary(Math::log, StrictMath::log, Math::exp),
            "log10", new Unary(Math::log10, StrictMath::log10, t -> Math.pow(10, t)),
            "log1p", new Unary(Math::log1p, StrictMath::log1p, Math::expm1),
            "exp", new Unary(Math::exp, StrictMath::exp, Math::log),
            "expm1", new Unary(Math::expm1, StrictMath::expm1, Math::log1p),
            "cbrt", new Unary(Math::cbrt, StrictMath::cbrt, t -> t * t * t));

    private static final Map<String, double[]> SAMPLES = new ConcurrentHashMap<>();

    private SampledMath() {}

    static Value<?, ?> unary(String name, boolean strict, Value<?, ?>[] args, SymbolicTraceHandler sth) throws NotImplementedException {
        Unary u = UNARY.get(name);
        if (u == null || args.length != 1 || !(args[0] instanceof DoubleValue x)) return PlaceHolder.instance;
        DoubleUnaryOperator f = strict ? u.strictF() : u.f();
        double result = f.applyAsDouble(x.concrete);
        if (!x.isSymbolic()) return new DoubleValue(x.context, result);
        double[] samples = SAMPLES.computeIfAbsent(name + (strict ? "/strict" : ""), k -> samples(f, u.inverse()));
        sth.recordSymbolicPrecisionLoss();
        return new DoubleValue(x.context, result, table(x.context, x.formula, f, samples, result));
    }

    /**
     * A function of two doubles. With one argument concrete, it is a function of the other, sampled
     * with the help of {@code inverseFirst} (the first argument giving a result, given the second)
     * or {@code inverseSecond}; with both symbolic, it is sampled on a grid.
     */
    static Value<?, ?> binary(String key, Value<?, ?>[] args, DoubleBinaryOperator f,
                              DoubleBinaryOperator inverseFirst, DoubleBinaryOperator inverseSecond,
                              SymbolicTraceHandler sth) throws NotImplementedException {
        if (args.length != 2 || !(args[0] instanceof DoubleValue a) || !(args[1] instanceof DoubleValue b)) return PlaceHolder.instance;
        SolverContext ctx = a.context;
        double result = f.applyAsDouble(a.concrete, b.concrete);
        if (!a.isSymbolic() && !b.isSymbolic()) return new DoubleValue(ctx, result);
        sth.recordSymbolicPrecisionLoss();
        if (!b.isSymbolic()) {
            double second = b.concrete;
            DoubleUnaryOperator g = v -> f.applyAsDouble(v, second);
            double[] samples = SAMPLES.computeIfAbsent(key + "/1/" + Double.doubleToRawLongBits(second),
                    k -> samples(g, t -> inverseFirst.applyAsDouble(t, second)));
            return new DoubleValue(ctx, result, table(ctx, a.formula, g, samples, result));
        }
        if (!a.isSymbolic()) {
            double first = a.concrete;
            DoubleUnaryOperator g = v -> f.applyAsDouble(first, v);
            double[] samples = SAMPLES.computeIfAbsent(key + "/2/" + Double.doubleToRawLongBits(first),
                    k -> samples(g, t -> inverseSecond.applyAsDouble(t, first)));
            return new DoubleValue(ctx, result, table(ctx, b.formula, g, samples, result));
        }
        FloatingPointFormulaManager fpfm = ctx.getFormulaManager().getFloatingPointFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        FloatingPointFormula table = constant(ctx, result);
        for (double x : GRID) {
            for (double y : GRID) {
                BooleanFormula at = bmgr.and(fpfm.assignment(a.formula, constant(ctx, x)), fpfm.assignment(b.formula, constant(ctx, y)));
                table = bmgr.ifThenElse(at, constant(ctx, f.applyAsDouble(x, y)), table);
            }
        }
        return new DoubleValue(ctx, result, table);
    }

    /** ite(x = s1, f(s1), ite(x = s2, f(s2), ... otherwise)), comparing bit patterns so -0.0 and NaN are their own samples. */
    private static FloatingPointFormula table(SolverContext ctx, FloatingPointFormula x, DoubleUnaryOperator f, double[] samples, double otherwise) {
        FloatingPointFormulaManager fpfm = ctx.getFormulaManager().getFloatingPointFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        FloatingPointFormula table = constant(ctx, otherwise);
        for (double s : samples) {
            table = bmgr.ifThenElse(fpfm.assignment(x, constant(ctx, s)), constant(ctx, f.applyAsDouble(s)), table);
        }
        return table;
    }

    private static FloatingPointFormula constant(SolverContext ctx, double v) {
        return new DoubleValue(ctx, v).formula;
    }

    /**
     * The nice inputs, and for each nice output t the input inverse(t), or a neighbour of it at
     * which f gives exactly t. Non-finite inputs are dropped.
     */
    static double[] samples(DoubleUnaryOperator f, DoubleUnaryOperator inverse) {
        Map<Long, Double> samples = new LinkedHashMap<>();
        for (double x : NICE_INPUTS) samples.put(Double.doubleToRawLongBits(x), x);
        for (double t : NICE_OUTPUTS) {
            double x = inverse.applyAsDouble(t);
            if (!Double.isFinite(x)) continue;
            double chosen = x;
            for (double c : new double[] {x, Math.nextUp(x), Math.nextDown(x), Math.nextUp(Math.nextUp(x)), Math.nextDown(Math.nextDown(x))}) {
                if (f.applyAsDouble(c) == t) {
                    chosen = c;
                    break;
                }
            }
            samples.put(Double.doubleToRawLongBits(chosen), chosen);
        }
        return samples.values().stream().mapToDouble(Double::doubleValue).toArray();
    }
}
