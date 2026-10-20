package de.uzl.its.swat.symbolic.invoke.java.lang;

import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.symbolic.trace.SymbolRegistry;
import de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormulaManager;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.SolverContext;

/**
 * Approximate symbolic models of transcendental Math functions (sin, tan, log, pow, ...), which have
 * no exact encoding in floating-point SMT.
 *
 * <p>Each call is an application of its own uninterpreted function, {@code math_<name>_<n>(x)},
 * defined by a trace constraint as a lookup table: exact at a fixed set of sample inputs, and the
 * concrete result of this execution everywhere else. Where the result was concrete before, it
 * still is, so the model never constrains a path more than concretization did; but the solver can
 * now pick a sample input to reach a branch that needs a particular result. Samples are "nice"
 * inputs plus inputs at which the function hits "nice" outputs, found through its inverse.
 *
 * <p>The function application keeps the approximation visible: a branch constraint that contains
 * one is reported as precision loss (see DTOBuilder), so an exhausted search proves nothing, and
 * the explorer can tell which queries depend on a guess. A result that never reaches a branch
 * leaves the explored paths exact, so it costs no precision. Every call has its own function, so
 * definitions never constrain each other: each one can always be satisfied by its own function
 * value, whatever the inputs, and leaves the queries that do not mention it unchanged.
 */
final class SampledMath {

    private static final double[] NICE_INPUTS = {
        0.0, -0.0, 0.5, -0.5, 1, -1, 2, -2, 3, -3, 4, -4, 5, -5, 10, -10, 100, -100, 1000, 0.1, -0.1,
        Math.PI, -Math.PI, Math.PI / 2, -Math.PI / 2, Math.PI / 3, Math.PI / 4, -Math.PI / 4, Math.PI / 6, Math.E,
        1.5 * Math.PI, -1.5 * Math.PI, 2 * Math.PI, -2 * Math.PI
    };

    private static final double[] NICE_OUTPUTS = {0.0, 0.5, -0.5, 1, -1, 2, -2, 3, -3, 5, -5, 10, -10, 100};

    /** Grid for functions of two symbolic arguments. */
    private static final double[] GRID = {-2, -1, -0.5, 0, 0.5, 1, 2, 3, 4, 10};

    private record Unary(DoubleUnaryOperator f, DoubleUnaryOperator strictF, DoubleUnaryOperator inverse) {}

    private static final Map<String, Unary> UNARY = Map.ofEntries(
            Map.entry("sin", new Unary(Math::sin, StrictMath::sin, Math::asin)),
            Map.entry("cos", new Unary(Math::cos, StrictMath::cos, Math::acos)),
            Map.entry("tan", new Unary(Math::tan, StrictMath::tan, Math::atan)),
            Map.entry("atan", new Unary(Math::atan, StrictMath::atan, Math::tan)),
            Map.entry("asin", new Unary(Math::asin, StrictMath::asin, Math::sin)),
            Map.entry("acos", new Unary(Math::acos, StrictMath::acos, Math::cos)),
            Map.entry("log", new Unary(Math::log, StrictMath::log, Math::exp)),
            Map.entry("log10", new Unary(Math::log10, StrictMath::log10, t -> Math.pow(10, t))),
            Map.entry("log1p", new Unary(Math::log1p, StrictMath::log1p, Math::expm1)),
            Map.entry("exp", new Unary(Math::exp, StrictMath::exp, Math::log)),
            Map.entry("expm1", new Unary(Math::expm1, StrictMath::expm1, Math::log1p)),
            Map.entry("cbrt", new Unary(Math::cbrt, StrictMath::cbrt, t -> t * t * t)));

    private static final Map<String, double[]> SAMPLES = new ConcurrentHashMap<>();

    /** Prefix of the functions the models are built from; the explorer recognises them by it. */
    static final String UF_PREFIX = "math_";

    private static final FormulaType.FloatingPointType DOUBLE = FormulaType.getDoublePrecisionFloatingPointType();

    /** Numbers the calls of this execution, so that every call has its own function. */
    private static final AtomicInteger CALLS = new AtomicInteger();

    private SampledMath() {}

    static Value<?, ?> unary(String name, boolean strict, Value<?, ?>[] args, SymbolicTraceHandler sth) throws NotImplementedException {
        Unary u = UNARY.get(name);
        if (u == null || args.length != 1 || !(args[0] instanceof DoubleValue x)) return PlaceHolder.instance;
        DoubleUnaryOperator f = strict ? u.strictF() : u.f();
        double result = f.applyAsDouble(x.concrete);
        if (!x.isSymbolic()) return new DoubleValue(x.context, result);
        String key = name + (strict ? "_strict" : "");
        double[] samples = SAMPLES.computeIfAbsent(key, k -> samples(f, u.inverse()));
        return new DoubleValue(x.context, result, define(x.context, key, sth, table(x.context, x.formula, f, samples, result), x.formula));
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
        FloatingPointFormula table;
        if (!b.isSymbolic()) {
            double second = b.concrete;
            DoubleUnaryOperator g = v -> f.applyAsDouble(v, second);
            double[] samples = SAMPLES.computeIfAbsent(key + "/1/" + Double.doubleToRawLongBits(second),
                    k -> samples(g, t -> inverseFirst.applyAsDouble(t, second)));
            table = table(ctx, a.formula, g, samples, result);
        } else if (!a.isSymbolic()) {
            double first = a.concrete;
            DoubleUnaryOperator g = v -> f.applyAsDouble(first, v);
            double[] samples = SAMPLES.computeIfAbsent(key + "/2/" + Double.doubleToRawLongBits(first),
                    k -> samples(g, t -> inverseSecond.applyAsDouble(t, first)));
            table = table(ctx, b.formula, g, samples, result);
        } else {
            FloatingPointFormulaManager fpfm = ctx.getFormulaManager().getFloatingPointFormulaManager();
            BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
            table = constant(ctx, result);
            for (double x : GRID) {
                for (double y : GRID) {
                    BooleanFormula at = bmgr.and(fpfm.assignment(a.formula, constant(ctx, x)), fpfm.assignment(b.formula, constant(ctx, y)));
                    table = bmgr.ifThenElse(at, constant(ctx, f.applyAsDouble(x, y)), table);
                }
            }
        }
        return new DoubleValue(ctx, result, define(ctx, key, sth, table, a.formula, b.formula));
    }

    /**
     * A fresh function {@code math_<key>_<n>} applied to {@code args}, defined as {@code value} by a
     * trace constraint.
     */
    private static FloatingPointFormula define(SolverContext ctx, String key, SymbolicTraceHandler sth,
                                               FloatingPointFormula value, FloatingPointFormula... args) {
        FormulaManager fmgr = ctx.getFormulaManager();
        List<FormulaType<?>> argTypes = new ArrayList<>();
        for (FloatingPointFormula arg : args) argTypes.add(DOUBLE);
        String name = UF_PREFIX + key + "_" + CALLS.getAndIncrement();
        FunctionDeclaration<FloatingPointFormula> uf = fmgr.getUFManager().declareUF(name, DOUBLE, argTypes);
        // The table is exact only at the samples: a branch over the function is a precision loss
        // (DTOBuilder). The definition alone constrains nothing else, as the function is fresh.
        SymbolRegistry.declare(name, SymbolRegistry.Kind.APPROXIMATE_MODEL_FUNCTION);
        FloatingPointFormula application = fmgr.getUFManager().callUF(uf, Arrays.asList(args));
        sth.addConstraint(fmgr.getFloatingPointFormulaManager().assignment(application, value));
        return application;
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
