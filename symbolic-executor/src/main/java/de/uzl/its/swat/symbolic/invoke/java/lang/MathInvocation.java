package de.uzl.its.swat.symbolic.invoke.java.lang;

import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.NumericalValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.LongValue;

import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.*;

public class MathInvocation {
    public static Value<?, ?> invokeStaticMethod(
            String name,
            Value<?, ?>[] args,
            Type[] desc,
            SymbolicTraceHandler symbolicTraceHandler) throws NotImplementedException, ValueConversionException {
        return invokeStaticMethod(name, args, desc, symbolicTraceHandler, false);
    }

    /**
     * @param strict whether the owner is StrictMath, whose transcendental functions may round
     *     differently from Math's
     */
    public static Value<?, ?> invokeStaticMethod(
            String name,
            Value<?, ?>[] args,
            Type[] desc,
            SymbolicTraceHandler symbolicTraceHandler,
            boolean strict) throws NotImplementedException, ValueConversionException {
        return switch (name) {
            case "abs" -> invokeAbs(args);
            case "max" -> invokeMax(args);
            case "min" -> invokeMin(args);
            case "round" -> invokeRound(args);
            case "sqrt" -> invokeSqrt(args);
            case "floor" -> roundToIntegral(args, FloatingPointRoundingMode.TOWARD_NEGATIVE, Math::floor);
            case "ceil" -> roundToIntegral(args, FloatingPointRoundingMode.TOWARD_POSITIVE, Math::ceil);
            case "rint" -> roundToIntegral(args, FloatingPointRoundingMode.NEAREST_TIES_TO_EVEN, Math::rint);
            // Both are a single multiplication by a constant (Math.toRadians is angdeg * DEGREES_TO_RADIANS).
            case "toRadians" -> scale(args, 0.017453292519943295, strict ? StrictMath::toRadians : Math::toRadians);
            case "toDegrees" -> scale(args, 57.29577951308232, strict ? StrictMath::toDegrees : Math::toDegrees);
            case "getExponent" -> invokeGetExponent(args);
            case "signum" -> invokeSignum(args);
            case "floorDiv" -> invokeFloorDivMod(args, false);
            case "floorMod" -> invokeFloorDivMod(args, true);
            case "pow" -> SampledMath.binary("pow" + (strict ? "/strict" : ""), args, strict ? StrictMath::pow : Math::pow,
                    // inverses: pow(x, y0) = t at x = t^(1/y0), pow(x0, y) = t at y = log(t) / log(x0)
                    (t, y0) -> y0 == 0 ? Double.NaN : Math.pow(t, 1 / y0),
                    (t, x0) -> Math.log(t) / Math.log(x0),
                    symbolicTraceHandler);
            case "atan2" -> SampledMath.binary("atan2" + (strict ? "/strict" : ""), args, strict ? StrictMath::atan2 : Math::atan2,
                    // inverses: atan2(y, x0) = t at y = x0 * tan(t), atan2(y0, x) = t at x = y0 / tan(t)
                    (t, x0) -> x0 * Math.tan(t),
                    (t, y0) -> y0 / Math.tan(t),
                    symbolicTraceHandler);
            default -> SampledMath.unary(name, strict, args, symbolicTraceHandler);
        };
    }

    /** Math.floor, ceil and rint: IEEE round-to-integral in the given mode, which is exactly what they compute. */
    private static Value<?, ?> roundToIntegral(Value<?, ?>[] args, FloatingPointRoundingMode mode, java.util.function.DoubleUnaryOperator concrete) {
        if (args.length != 1 || !(args[0] instanceof DoubleValue a)) return PlaceHolder.instance;
        FloatingPointFormulaManager fpfm = a.context.getFormulaManager().getFloatingPointFormulaManager();
        return new DoubleValue(a.context, concrete.applyAsDouble(a.concrete), fpfm.round(a.formula, mode));
    }

    /** Math.toRadians and toDegrees: one correctly rounded multiplication by a constant. */
    private static Value<?, ?> scale(Value<?, ?>[] args, double factor, java.util.function.DoubleUnaryOperator concrete) {
        if (args.length != 1 || !(args[0] instanceof DoubleValue a)) return PlaceHolder.instance;
        FloatingPointFormulaManager fpfm = a.context.getFormulaManager().getFloatingPointFormulaManager();
        return new DoubleValue(a.context, concrete.applyAsDouble(a.concrete),
                fpfm.multiply(a.formula, new DoubleValue(a.context, factor).formula));
    }

    /**
     * Math.getExponent(float) and getExponent(double): the biased exponent field minus the bias.
     * This also gives MAX_EXPONENT + 1 for NaN and infinities and MIN_EXPONENT - 1 for zeros and
     * subnormals, as specified.
     */
    private static Value<?, ?> invokeGetExponent(Value<?, ?>[] args) {
        if (args.length != 1) return PlaceHolder.instance;
        if (args[0] instanceof DoubleValue d) {
            BitvectorFormulaManager bvmgr = d.context.getFormulaManager().getBitvectorFormulaManager();
            BitvectorFormula bits = d.context.getFormulaManager().getFloatingPointFormulaManager().toIeeeBitvector(d.formula);
            BitvectorFormula field = bvmgr.extend(bvmgr.extract(bits, 62, 52), 21, false);
            return new IntValue(d.context, Math.getExponent(d.concrete), bvmgr.subtract(field, bvmgr.makeBitvector(32, Double.MAX_EXPONENT)));
        } else if (args[0] instanceof FloatValue f) {
            BitvectorFormulaManager bvmgr = f.context.getFormulaManager().getBitvectorFormulaManager();
            BitvectorFormula bits = f.context.getFormulaManager().getFloatingPointFormulaManager().toIeeeBitvector(f.formula);
            BitvectorFormula field = bvmgr.extend(bvmgr.extract(bits, 30, 23), 24, false);
            return new IntValue(f.context, Math.getExponent(f.concrete), bvmgr.subtract(field, bvmgr.makeBitvector(32, Float.MAX_EXPONENT)));
        }
        return PlaceHolder.instance;
    }

    /** Math.signum: the argument itself for zeros and NaN, otherwise 1.0 with its sign. */
    private static Value<?, ?> invokeSignum(Value<?, ?>[] args) {
        if (args.length != 1) return PlaceHolder.instance;
        if (args[0] instanceof DoubleValue d) {
            return new DoubleValue(d.context, Math.signum(d.concrete), signum(d.context, d.formula, new DoubleValue(d.context, 1.0).formula));
        } else if (args[0] instanceof FloatValue f) {
            return new FloatValue(f.context, Math.signum(f.concrete), signum(f.context, f.formula, new FloatValue(f.context, 1.0f).formula));
        }
        return PlaceHolder.instance;
    }

    private static FloatingPointFormula signum(SolverContext ctx, FloatingPointFormula x, FloatingPointFormula one) {
        FloatingPointFormulaManager fpfm = ctx.getFormulaManager().getFloatingPointFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        return bmgr.ifThenElse(bmgr.or(fpfm.isZero(x), fpfm.isNaN(x)), x,
                bmgr.ifThenElse(fpfm.isNegative(x), fpfm.negate(one), one));
    }

    /**
     * Math.floorDiv and floorMod of two ints or two longs, for a concrete non-zero divisor. With a
     * symbolic divisor the ArithmeticException for zero would need a branch of its own, so that
     * case stays unmodelled.
     */
    private static Value<?, ?> invokeFloorDivMod(Value<?, ?>[] args, boolean mod) {
        if (args.length != 2) return PlaceHolder.instance;
        if (args[0] instanceof LongValue x && args[1] instanceof LongValue y) {
            if (y.isSymbolic() || y.concrete == 0) return PlaceHolder.instance;
            BitvectorFormula r = floorDivMod(x.context, x.formula, y.formula, 64, mod);
            return new LongValue(x.context, mod ? Math.floorMod(x.concrete, y.concrete) : Math.floorDiv(x.concrete, y.concrete), r);
        } else if (args[0] instanceof IntValue x && args[1] instanceof IntValue y) {
            if (y.isSymbolic() || y.concrete == 0) return PlaceHolder.instance;
            BitvectorFormula r = floorDivMod(x.context, x.formula, y.formula, 32, mod);
            return new IntValue(x.context, mod ? Math.floorMod(x.concrete, y.concrete) : Math.floorDiv(x.concrete, y.concrete), r);
        }
        return PlaceHolder.instance;
    }

    /** As the JDK: q = x / y, minus one if the signs differ and the division is not exact; mod = x - q * y. */
    private static BitvectorFormula floorDivMod(SolverContext ctx, BitvectorFormula x, BitvectorFormula y, int width, boolean mod) {
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        BitvectorFormula q = bvmgr.divide(x, y, true);
        BooleanFormula adjust = bmgr.and(
                bvmgr.lessThan(bvmgr.xor(x, y), bvmgr.makeBitvector(width, 0), true),
                bmgr.not(bvmgr.equal(bvmgr.multiply(q, y), x)));
        BitvectorFormula floorQ = bmgr.ifThenElse(adjust, bvmgr.subtract(q, bvmgr.makeBitvector(width, 1)), q);
        return mod ? bvmgr.subtract(x, bvmgr.multiply(floorQ, y)) : floorQ;
    }

    private static Value<?, ?> invokeAbs(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        SWATAssert.enforce(args.length == 1, "Wrong argument counts");
        SWATAssert.enforce(args[0] instanceof NumericalValue, "Wrong argument type");
        NumericalValue<?, ?> val = (NumericalValue<?, ?>) args[0];
        BooleanFormulaManager bfm = val.context.getFormulaManager().getBooleanFormulaManager();

        if (args[0] instanceof FloatValue v) {
            FloatingPointFormulaManager fpfm = val.context.getFormulaManager().getFloatingPointFormulaManager();
            return new FloatValue(v.context, Math.abs(v.concrete), fpfm.abs(v.formula));
        } else if (args[0] instanceof DoubleValue v) {
            FloatingPointFormulaManager fpfm = val.context.getFormulaManager().getFloatingPointFormulaManager();
            return new DoubleValue(v.context, Math.abs(v.concrete), fpfm.abs(v.formula));
        } else if (args[0] instanceof LongValue v) {
            BitvectorFormulaManager bvmgr = val.context.getFormulaManager().getBitvectorFormulaManager();
            BitvectorFormula zero = bvmgr.makeBitvector(64, 0);
            return new LongValue(v.context, Math.abs(v.concrete), bfm.ifThenElse(
                    bvmgr.greaterOrEquals(v.formula, zero, true), // signed
                    v.formula,
                    bvmgr.negate(v.formula)));
        } else {
            IntValue v = val.asIntValue();
            BitvectorFormulaManager bvmgr = val.context.getFormulaManager().getBitvectorFormulaManager();
            BitvectorFormula zero = bvmgr.makeBitvector(32, 0);
            return new IntValue(v.context, Math.abs(v.concrete), bfm.ifThenElse(
                    bvmgr.greaterOrEquals(v.formula, zero, true), // signed
                    v.formula,
                    bvmgr.negate(v.formula)));
        }
    }

    private static Value<?, ?> invokeMax(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        SWATAssert.enforce(args.length == 2, "Wrong argument counts");
        SWATAssert.enforce(args[0] instanceof NumericalValue || args[1] instanceof NumericalValue,
                "Wrong argument type");
        BooleanFormulaManager bfm = args[0].context.getFormulaManager().getBooleanFormulaManager();

        if (args[0] instanceof FloatValue v && args[1] instanceof FloatValue w) {
            return new FloatValue(v.context, Math.max(v.concrete, w.concrete), fpMinMax(v.context, v.formula, w.formula, true));
        } else if (args[0] instanceof DoubleValue v && args[1] instanceof DoubleValue w) {
            return new DoubleValue(v.context, Math.max(v.concrete, w.concrete), fpMinMax(v.context, v.formula, w.formula, true));
        } else if (args[0] instanceof LongValue v && args[1] instanceof LongValue w) {
            BitvectorFormulaManager bvmgr = v.context.getFormulaManager().getBitvectorFormulaManager();
            return new LongValue(v.context, Math.max(v.concrete, w.concrete), bfm.ifThenElse(
                    bvmgr.greaterOrEquals(v.formula, w.formula, true), // signed
                    v.formula,
                    w.formula));
        } else {
            IntValue v = args[0].asIntValue();
            IntValue w = args[1].asIntValue();
            BitvectorFormulaManager bvmgr = v.context.getFormulaManager().getBitvectorFormulaManager();
            return new IntValue(v.context, Math.max(v.concrete, w.concrete), bfm.ifThenElse(
                    bvmgr.greaterOrEquals(v.formula, w.formula, true), // signed
                    v.formula,
                    w.formula));
        }
    }

    private static Value<?, ?> invokeMin(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        SWATAssert.enforce(args.length == 2, "Wrong argument counts");
        SWATAssert.enforce(args[0] instanceof NumericalValue || args[1] instanceof NumericalValue,
                "Wrong argument type");
        BooleanFormulaManager bfm = args[0].context.getFormulaManager().getBooleanFormulaManager();

        if (args[0] instanceof FloatValue v && args[1] instanceof FloatValue w) {
            return new FloatValue(v.context, Math.min(v.concrete, w.concrete), fpMinMax(v.context, v.formula, w.formula, false));
        } else if (args[0] instanceof DoubleValue v && args[1] instanceof DoubleValue w) {
            return new DoubleValue(v.context, Math.min(v.concrete, w.concrete), fpMinMax(v.context, v.formula, w.formula, false));
        } else if (args[0] instanceof LongValue v && args[1] instanceof LongValue w) {
            BitvectorFormulaManager bvmgr = v.context.getFormulaManager().getBitvectorFormulaManager();
            return new LongValue(v.context, Math.min(v.concrete, w.concrete), bfm.ifThenElse(
                    bvmgr.lessOrEquals(v.formula, w.formula, true), // signed
                    v.formula,
                    w.formula));
        } else {
            IntValue v = args[0].asIntValue();
            IntValue w = args[1].asIntValue();
            BitvectorFormulaManager bvmgr = v.context.getFormulaManager().getBitvectorFormulaManager();
            return new IntValue(v.context, Math.min(v.concrete, w.concrete), bfm.ifThenElse(
                    bvmgr.lessOrEquals(v.formula, w.formula, true), // signed
                    v.formula,
                    w.formula));
        }
    }

    /**
     * Math.max and min of two floats or doubles as the JDK computes them: NaN if either argument is
     * NaN, and +0.0 (max) or -0.0 (min) for two zeros of different sign. SMT-LIB's fp.max and fp.min
     * differ in both: they return the other argument for one NaN and either zero for two zeros.
     */
    private static FloatingPointFormula fpMinMax(SolverContext ctx, FloatingPointFormula a, FloatingPointFormula b, boolean max) {
        FloatingPointFormulaManager fpfm = ctx.getFormulaManager().getFloatingPointFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        // max: if (a != a) a; else if (a, b zero and a = -0.0) b; else a >= b ? a : b
        // min: if (a != a) a; else if (a, b zero and b = -0.0) b; else a <= b ? a : b
        BooleanFormula negZero = max
                ? bmgr.and(fpfm.isZero(a), fpfm.isNegative(a))
                : bmgr.and(fpfm.isZero(b), fpfm.isNegative(b));
        return bmgr.ifThenElse(fpfm.isNaN(a), a,
                bmgr.ifThenElse(bmgr.and(fpfm.isZero(a), fpfm.isZero(b), negZero), b,
                        bmgr.ifThenElse(max ? fpfm.greaterOrEquals(a, b) : fpfm.lessOrEquals(a, b), a, b)));
    }

    private static Value<?, ?> invokeSqrt(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        SWATAssert.enforce(args.length == 1, "Wrong argument counts");
        SWATAssert.enforce(args[0] instanceof DoubleValue, "Wrong argument type");
        // Todo: Are NaN inf and zero values handled correctly? -> Write tests
        DoubleValue dv = args[0].asDoubleValue();
        FloatingPointFormulaManager fpfm = dv.context.getFormulaManager().getFloatingPointFormulaManager();
        return new DoubleValue(dv.context, Math.sqrt(dv.concrete), fpfm.sqrt(dv.formula));
    }

    /**
     * Invocation handler for Math.round(float) and Math.round(double).
     * Math.round(float) returns int, Math.round(double) returns long.
     *
     * Java Math.round special cases:
     * - NaN returns 0
     * - Negative infinity or values <= MIN_VALUE return MIN_VALUE
     * - Positive infinity or values >= MAX_VALUE return MAX_VALUE
     * - Otherwise, round to the nearest integer, ties toward positive infinity
     *
     * Note: SMT's fp.to_sbv has undefined behavior for NaN/infinity, so we must
     * explicitly encode Java's semantics with if-then-else.
     */
    private static Value<?, ?> invokeRound(Value<?, ?>[] args)
            throws NotImplementedException, ValueConversionException {
        SWATAssert.enforce(args.length == 1, "Wrong argument counts");

        if (args[0] instanceof FloatValue v) {
            // Math.round(float) -> int
            FloatingPointFormulaManager fpfm = v.context.getFormulaManager().getFloatingPointFormulaManager();
            BitvectorFormulaManager bvmgr = v.context.getFormulaManager().getBitvectorFormulaManager();
            BooleanFormulaManager bmgr = v.context.getFormulaManager().getBooleanFormulaManager();

            // Constants for special case handling
            BitvectorFormula zero32 = bvmgr.makeBitvector(32, 0);
            BitvectorFormula intMin = bvmgr.makeBitvector(32, Integer.MIN_VALUE);
            BitvectorFormula intMax = bvmgr.makeBitvector(32, Integer.MAX_VALUE);

            // Float threshold for Integer.MIN_VALUE and MAX_VALUE
            // Integer.MIN_VALUE = -2147483648, Integer.MAX_VALUE = 2147483647
            FloatingPointFormula minThreshold = fpfm.makeNumber(-2147483648.0f,
                    FormulaType.getSinglePrecisionFloatingPointType());
            FloatingPointFormula maxThreshold = fpfm.makeNumber(2147483647.0f,
                    FormulaType.getSinglePrecisionFloatingPointType());

            // Check special conditions
            BooleanFormula isNaN = fpfm.isNaN(v.formula);
            BooleanFormula isNegInf = fpfm.isInfinity(v.formula);
            BooleanFormula isPosInf = fpfm.isInfinity(v.formula);
            BooleanFormula isNegative = fpfm.lessThan(v.formula,
                    fpfm.makeNumber(0.0f, FormulaType.getSinglePrecisionFloatingPointType()));
            BooleanFormula isNegInfOrVeryNeg = bmgr.or(bmgr.and(isNegInf, isNegative),
                    fpfm.lessOrEquals(v.formula, minThreshold));
            BooleanFormula isPosInfOrVeryPos = bmgr.or(bmgr.and(isPosInf, bmgr.not(isNegative)),
                    fpfm.greaterOrEquals(v.formula, maxThreshold));

            // Normal case: round and convert to int
            FloatingPointFormula rounded = roundHalfUp(v.context, v.formula, new FloatValue(v.context, 0.5f).formula, new FloatValue(v.context, 1.0f).formula);
            BitvectorFormula normalResult = fpfm.castTo(rounded, true, FormulaType.getBitvectorTypeWithSize(32),
                    FloatingPointRoundingMode.NEAREST_TIES_TO_EVEN);

            // Build if-then-else chain for Java semantics:
            // if NaN then 0
            // else if negInf/veryNeg then MIN_VALUE
            // else if posInf/veryPos then MAX_VALUE
            // else normal round
            BitvectorFormula result = bmgr.ifThenElse(isNaN, zero32,
                    bmgr.ifThenElse(isNegInfOrVeryNeg, intMin,
                            bmgr.ifThenElse(isPosInfOrVeryPos, intMax, normalResult)));

            return new IntValue(v.context, Math.round(v.concrete), result);
        } else if (args[0] instanceof DoubleValue v) {
            // Math.round(double) -> long
            FloatingPointFormulaManager fpfm = v.context.getFormulaManager().getFloatingPointFormulaManager();
            BitvectorFormulaManager bvmgr = v.context.getFormulaManager().getBitvectorFormulaManager();
            BooleanFormulaManager bmgr = v.context.getFormulaManager().getBooleanFormulaManager();

            // Constants for special case handling
            BitvectorFormula zero64 = bvmgr.makeBitvector(64, 0);
            BitvectorFormula longMin = bvmgr.makeBitvector(64, Long.MIN_VALUE);
            BitvectorFormula longMax = bvmgr.makeBitvector(64, Long.MAX_VALUE);

            // Double threshold for Long.MIN_VALUE and MAX_VALUE
            // Long.MIN_VALUE = -9223372036854775808, Long.MAX_VALUE = 9223372036854775807
            // Note: These values can't be exactly represented as double, but we use the
            // closest representable values
            FloatingPointFormula minThreshold = fpfm.makeNumber((double) Long.MIN_VALUE,
                    FormulaType.getDoublePrecisionFloatingPointType());
            FloatingPointFormula maxThreshold = fpfm.makeNumber((double) Long.MAX_VALUE,
                    FormulaType.getDoublePrecisionFloatingPointType());

            // Check special conditions
            BooleanFormula isNaN = fpfm.isNaN(v.formula);
            BooleanFormula isInf = fpfm.isInfinity(v.formula);
            BooleanFormula isNegative = fpfm.lessThan(v.formula,
                    fpfm.makeNumber(0.0, FormulaType.getDoublePrecisionFloatingPointType()));
            BooleanFormula isNegInfOrVeryNeg = bmgr.or(bmgr.and(isInf, isNegative),
                    fpfm.lessOrEquals(v.formula, minThreshold));
            BooleanFormula isPosInfOrVeryPos = bmgr.or(bmgr.and(isInf, bmgr.not(isNegative)),
                    fpfm.greaterOrEquals(v.formula, maxThreshold));

            // Normal case: round and convert to long
            FloatingPointFormula rounded = roundHalfUp(v.context, v.formula, new DoubleValue(v.context, 0.5).formula, new DoubleValue(v.context, 1.0).formula);
            BitvectorFormula normalResult = fpfm.castTo(rounded, true, FormulaType.getBitvectorTypeWithSize(64),
                    FloatingPointRoundingMode.NEAREST_TIES_TO_EVEN);

            // Build if-then-else chain for Java semantics:
            // if NaN then 0
            // else if negInf/veryNeg then MIN_VALUE
            // else if posInf/veryPos then MAX_VALUE
            // else normal round
            BitvectorFormula result = bmgr.ifThenElse(isNaN, zero64,
                    bmgr.ifThenElse(isNegInfOrVeryNeg, longMin,
                            bmgr.ifThenElse(isPosInfOrVeryPos, longMax, normalResult)));

            return new LongValue(v.context, Math.round(v.concrete), result);
        } else {
            return PlaceHolder.instance;
        }
    }
    /**
     * The integer closest to x, ties toward positive infinity, as Math.round rounds: floor(x), plus
     * one if x - floor(x) >= 0.5. The difference is exact wherever it can reach 0.5 (Sterbenz; for x
     * in (-0.5, 0) it is above 0.5 and rounding keeps it there), and beyond 2^52 (2^23) x is
     * integral, so floor(x) + 1 is only formed where it is exact.
     */
    private static FloatingPointFormula roundHalfUp(SolverContext ctx, FloatingPointFormula x, FloatingPointFormula half, FloatingPointFormula one) {
        FloatingPointFormulaManager fpfm = ctx.getFormulaManager().getFloatingPointFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        FloatingPointFormula floor = fpfm.round(x, FloatingPointRoundingMode.TOWARD_NEGATIVE);
        return bmgr.ifThenElse(fpfm.greaterOrEquals(fpfm.subtract(x, floor), half), fpfm.add(floor, one), floor);
    }

    /*
     * 
     * private static IntValue invokeMax(IntValue a, IntValue b) {
     * FormulaManager fmgr = a.context.getFormulaManager();
     * BooleanFormula cond =
     * fmgr.getIntegerFormulaManager().greaterOrEquals(a.formula, b.formula);
     * NumeralFormula.IntegerFormula res =
     * fmgr.getBooleanFormulaManager().ifThenElse(cond, a.formula, b.formula);
     * return new IntValue(a.context, Math.max(a.concrete, b.concrete), res);
     * }
     * 
     * private static DoubleValue invokeMax(DoubleValue a, DoubleValue b) {
     * FormulaManager fmgr = a.context.getFormulaManager();
     * BooleanFormula cond =
     * fmgr.getFloatingPointFormulaManager().greaterOrEquals(a.formula, b.formula);
     * FloatingPointFormula res =
     * fmgr.getBooleanFormulaManager().ifThenElse(cond, a.formula, b.formula);
     * return new DoubleValue(a.context, Math.max(a.concrete, b.concrete), res);
     * }
     * 
     * if (args[0] instanceof IntValue a && args[1] instanceof IntValue b) {
     * return invokeMax(a, b);
     * }
     * if (args[0] instanceof DoubleValue a && args[1] instanceof DoubleValue b) {
     * return invokeMax(a, b);
     * }
     * } else if (owner.equals("java/lang/Math") && name.equals("min") &&
     * args.length == 2) {
     * if (args[0] instanceof IntValue a && args[1] instanceof IntValue b) {
     * return a.concrete < b.concrete ? a : b;
     * }
     */
}
