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
        return switch (name) {
            case "abs" -> invokeAbs(args);
            case "max" -> invokeMax(args);
            case "min" -> invokeMin(args);
            case "round" -> invokeRound(args);
            case "sqrt" -> invokeSqrt(args);

            default -> PlaceHolder.instance;
        };
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
                    bvmgr.greaterOrEquals(v.formula, zero, true),  // signed
                    v.formula,
                    bvmgr.negate(v.formula)));
        } else {
            IntValue v = val.asIntValue();
            BitvectorFormulaManager bvmgr = val.context.getFormulaManager().getBitvectorFormulaManager();
            BitvectorFormula zero = bvmgr.makeBitvector(32, 0);
            return new IntValue(v.context, Math.abs(v.concrete), bfm.ifThenElse(
                    bvmgr.greaterOrEquals(v.formula, zero, true),  // signed
                    v.formula,
                    bvmgr.negate(v.formula)));
        }
    }

    private static Value<?, ?> invokeMax(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        SWATAssert.enforce(args.length == 2, "Wrong argument counts");
        SWATAssert.enforce(args[0] instanceof NumericalValue || args[1] instanceof NumericalValue, "Wrong argument type");
        BooleanFormulaManager bfm = args[0].context.getFormulaManager().getBooleanFormulaManager();

        if (args[0] instanceof FloatValue v && args[1] instanceof FloatValue w) {
            FloatingPointFormulaManager fpfm = v.context.getFormulaManager().getFloatingPointFormulaManager();
            return new FloatValue(v.context, Math.max(v.concrete, w.concrete), fpfm.max(v.formula, w.formula));
        } else if (args[0] instanceof DoubleValue v && args[1] instanceof DoubleValue w) {
            FloatingPointFormulaManager fpfm = v.context.getFormulaManager().getFloatingPointFormulaManager();
            return new DoubleValue(v.context, Math.max(v.concrete, w.concrete), fpfm.max(v.formula, w.formula));
        } else if (args[0] instanceof LongValue v && args[1] instanceof LongValue w) {
            BitvectorFormulaManager bvmgr = v.context.getFormulaManager().getBitvectorFormulaManager();
            return new LongValue(v.context, Math.max(v.concrete, w.concrete), bfm.ifThenElse(
                    bvmgr.greaterOrEquals(v.formula, w.formula, true),  // signed
                    v.formula,
                    w.formula));
        } else {
            IntValue v = args[0].asIntValue();
            IntValue w = args[1].asIntValue();
            BitvectorFormulaManager bvmgr = v.context.getFormulaManager().getBitvectorFormulaManager();
            return new IntValue(v.context, Math.max(v.concrete, w.concrete), bfm.ifThenElse(
                    bvmgr.greaterOrEquals(v.formula, w.formula, true),  // signed
                    v.formula,
                    w.formula));
        }
    }

    private static Value<?, ?> invokeMin(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        SWATAssert.enforce(args.length == 2, "Wrong argument counts");
        SWATAssert.enforce(args[0] instanceof NumericalValue || args[1] instanceof NumericalValue, "Wrong argument type");
        BooleanFormulaManager bfm = args[0].context.getFormulaManager().getBooleanFormulaManager();

        if (args[0] instanceof FloatValue v && args[1] instanceof FloatValue w) {
            FloatingPointFormulaManager fpfm = v.context.getFormulaManager().getFloatingPointFormulaManager();
            return new FloatValue(v.context, Math.min(v.concrete, w.concrete), fpfm.min(v.formula, w.formula));
        } else if (args[0] instanceof DoubleValue v && args[1] instanceof DoubleValue w) {
            FloatingPointFormulaManager fpfm = v.context.getFormulaManager().getFloatingPointFormulaManager();
            return new DoubleValue(v.context, Math.min(v.concrete, w.concrete), fpfm.min(v.formula, w.formula));
        } else if (args[0] instanceof LongValue v && args[1] instanceof LongValue w) {
            BitvectorFormulaManager bvmgr = v.context.getFormulaManager().getBitvectorFormulaManager();
            return new LongValue(v.context, Math.min(v.concrete, w.concrete), bfm.ifThenElse(
                    bvmgr.lessOrEquals(v.formula, w.formula, true),  // signed
                    v.formula,
                    w.formula));
        } else {
            IntValue v = args[0].asIntValue();
            IntValue w = args[1].asIntValue();
            BitvectorFormulaManager bvmgr = v.context.getFormulaManager().getBitvectorFormulaManager();
            return new IntValue(v.context, Math.min(v.concrete, w.concrete), bfm.ifThenElse(
                    bvmgr.lessOrEquals(v.formula, w.formula, true),  // signed
                    v.formula,
                    w.formula));
        }
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
     * - Otherwise, round to nearest integer
     *
     * Note: SMT's fp.to_sbv has undefined behavior for NaN/infinity, so we must
     * explicitly encode Java's semantics with if-then-else.
     */
    private static Value<?, ?> invokeRound(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
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
            FloatingPointFormula minThreshold = fpfm.makeNumber(-2147483648.0f, FormulaType.getSinglePrecisionFloatingPointType());
            FloatingPointFormula maxThreshold = fpfm.makeNumber(2147483647.0f, FormulaType.getSinglePrecisionFloatingPointType());

            // Check special conditions
            BooleanFormula isNaN = fpfm.isNaN(v.formula);
            BooleanFormula isNegInf = fpfm.isInfinity(v.formula);
            BooleanFormula isPosInf = fpfm.isInfinity(v.formula);
            BooleanFormula isNegative = fpfm.lessThan(v.formula, fpfm.makeNumber(0.0f, FormulaType.getSinglePrecisionFloatingPointType()));
            BooleanFormula isNegInfOrVeryNeg = bmgr.or(bmgr.and(isNegInf, isNegative),
                    fpfm.lessOrEquals(v.formula, minThreshold));
            BooleanFormula isPosInfOrVeryPos = bmgr.or(bmgr.and(isPosInf, bmgr.not(isNegative)),
                    fpfm.greaterOrEquals(v.formula, maxThreshold));

            // Normal case: round and convert to int
            FloatingPointFormula rounded = fpfm.round(v.formula, FloatingPointRoundingMode.NEAREST_TIES_TO_EVEN);
            BitvectorFormula normalResult = fpfm.castTo(rounded, true, FormulaType.getBitvectorTypeWithSize(32), FloatingPointRoundingMode.NEAREST_TIES_TO_EVEN);

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
            // Note: These values can't be exactly represented as double, but we use the closest representable values
            FloatingPointFormula minThreshold = fpfm.makeNumber((double) Long.MIN_VALUE, FormulaType.getDoublePrecisionFloatingPointType());
            FloatingPointFormula maxThreshold = fpfm.makeNumber((double) Long.MAX_VALUE, FormulaType.getDoublePrecisionFloatingPointType());

            // Check special conditions
            BooleanFormula isNaN = fpfm.isNaN(v.formula);
            BooleanFormula isInf = fpfm.isInfinity(v.formula);
            BooleanFormula isNegative = fpfm.lessThan(v.formula, fpfm.makeNumber(0.0, FormulaType.getDoublePrecisionFloatingPointType()));
            BooleanFormula isNegInfOrVeryNeg = bmgr.or(bmgr.and(isInf, isNegative),
                    fpfm.lessOrEquals(v.formula, minThreshold));
            BooleanFormula isPosInfOrVeryPos = bmgr.or(bmgr.and(isInf, bmgr.not(isNegative)),
                    fpfm.greaterOrEquals(v.formula, maxThreshold));

            // Normal case: round and convert to long
            FloatingPointFormula rounded = fpfm.round(v.formula, FloatingPointRoundingMode.NEAREST_TIES_TO_EVEN);
            BitvectorFormula normalResult = fpfm.castTo(rounded, true, FormulaType.getBitvectorTypeWithSize(64), FloatingPointRoundingMode.NEAREST_TIES_TO_EVEN);

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
/*

   private static IntValue invokeMax(IntValue a, IntValue b) {
       FormulaManager fmgr = a.context.getFormulaManager();
       BooleanFormula cond = fmgr.getIntegerFormulaManager().greaterOrEquals(a.formula, b.formula);
       NumeralFormula.IntegerFormula res =
               fmgr.getBooleanFormulaManager().ifThenElse(cond, a.formula, b.formula);
       return new IntValue(a.context, Math.max(a.concrete, b.concrete), res);
   }

   private static DoubleValue invokeMax(DoubleValue a, DoubleValue b) {
       FormulaManager fmgr = a.context.getFormulaManager();
       BooleanFormula cond =
               fmgr.getFloatingPointFormulaManager().greaterOrEquals(a.formula, b.formula);
       FloatingPointFormula res =
               fmgr.getBooleanFormulaManager().ifThenElse(cond, a.formula, b.formula);
       return new DoubleValue(a.context, Math.max(a.concrete, b.concrete), res);
   }

    if (args[0] instanceof IntValue a && args[1] instanceof IntValue b) {
               return invokeMax(a, b);
           }
           if (args[0] instanceof DoubleValue a && args[1] instanceof DoubleValue b) {
               return invokeMax(a, b);
           }
       } else if (owner.equals("java/lang/Math") && name.equals("min") && args.length == 2) {
           if (args[0] instanceof IntValue a && args[1] instanceof IntValue b) {
               return a.concrete < b.concrete ? a : b;
           }
*/
}
