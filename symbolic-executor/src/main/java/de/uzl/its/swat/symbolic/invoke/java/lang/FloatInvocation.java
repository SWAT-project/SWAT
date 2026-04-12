package de.uzl.its.swat.symbolic.invoke.java.lang;

import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormulaManager;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula;
import org.sosy_lab.java_smt.api.SolverContext;

import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.BooleanValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.FloatObjectValue;

public class FloatInvocation {
    public static Value<?, ?> invokeStaticMethod(
            String name,
            Value<?, ?>[] args,
            Type[] desc,
            SymbolicTraceHandler symbolicTraceHandler) throws NotImplementedException, ValueConversionException {
        return switch (name) {
            case "compare" -> invokeCompare(args);
            case "floatToIntBits" -> invokeFloatToIntBits(args);
            case "floatToRawIntBits" -> invokeFloatToIntBits(args); // Same as floatToIntBits for symbolic execution
            case "isFinite" -> invokeIsFinite(args);
            case "isInfinite" -> invokeIsInfinite(args);
            case "isNaN" -> invokeIsNaN(args);
            case "max" -> invokeMax(args);
            case "min" -> invokeMin(args);
            case "sum" -> invokeSum(args);
            case "valueOf" -> invokeValueOf(args);

            default -> PlaceHolder.instance;
        };
    }

    /**
     * Invocation handler for Float.compare(float, float), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied
     * @throws ValueConversionException If arguments cannot be converted
     */
    private static Value<?, ?> invokeCompare(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        SWATAssert.enforce(args.length == 2, "Expected 2 arguments for compare(), got " + args.length);
        return compare(args[0].asFloatValue(), args[1].asFloatValue());
    }

    /**
     * Symbolic wrapper for Float.compare(float f1, float f2)
     * Compares the two specified float values.
     * Returns:
     *   - 0 if f1 is numerically equal to f2
     *   - negative value if f1 is less than f2
     *   - positive value if f1 is greater than f2
     * NaN is considered greater than any other value (including POSITIVE_INFINITY).
     *
     * @param f1 The first float to compare
     * @param f2 The second float to compare
     * @return IntValue representing the comparison result
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Float.html#compare(float,float)">Float.compare(float, float)</a>
     */
    private static IntValue compare(FloatValue f1, FloatValue f2) {
        SolverContext ctx = f1.context;
        FloatingPointFormulaManager fpmgr = ctx.getFormulaManager().getFloatingPointFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        IntegerFormulaManager imgr = ctx.getFormulaManager().getIntegerFormulaManager();

        // Check for NaN cases first
        BooleanFormula f1IsNaN = fpmgr.isNaN(f1.formula);
        BooleanFormula f2IsNaN = fpmgr.isNaN(f2.formula);

        // Regular comparisons (when neither is NaN)
        BooleanFormula isEqual = fpmgr.equalWithFPSemantics(f1.formula, f2.formula);
        BooleanFormula isLess = fpmgr.lessThan(f1.formula, f2.formula);

        // Build the result using IntegerFormula (will be converted to bitvector by IntValue constructor):
        // - If both are NaN: return 0
        // - If only f1 is NaN: return 1 (NaN > anything)
        // - If only f2 is NaN: return -1 (anything < NaN)
        // - If equal: return 0
        // - If f1 < f2: return -1
        // - Otherwise: return 1
        NumeralFormula.IntegerFormula normalResult = bmgr.ifThenElse(isEqual, imgr.makeNumber(0),
                bmgr.ifThenElse(isLess, imgr.makeNumber(-1), imgr.makeNumber(1)));

        NumeralFormula.IntegerFormula result = bmgr.ifThenElse(bmgr.and(f1IsNaN, f2IsNaN), imgr.makeNumber(0),
                bmgr.ifThenElse(f1IsNaN, imgr.makeNumber(1),
                        bmgr.ifThenElse(f2IsNaN, imgr.makeNumber(-1), normalResult)));

        return new IntValue(ctx, Float.compare(f1.concrete, f2.concrete), result);
    }
    /**
     * Invocation handling for the Float static method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Float.html#valueOf(float)">valueOf</a>().
     * Returns a Float instance representing the specified float value.
     *
     * @param args List of Values that correspond to the method arguments
     * @return FloatObjectValue representing the boxed float value
     */
    private static Value<?, ?> invokeValueOf(Value<?, ?>[] args) {
        SWATAssert.enforce(args.length == 1, "Wrong number of arguments for valueOf() method");
        if (args[0] instanceof FloatValue dv) {
            return new FloatObjectValue(dv.context, dv, ObjectValue.ADDRESS_UNKNOWN);
        }
        return PlaceHolder.instance;
    }

    /**
     * Invocation handler for Float.max(float, float).
     */
    private static Value<?, ?> invokeMax(Value<?, ?>[] args)
            throws ValueConversionException, NotImplementedException {
        SWATAssert.enforce(args.length == 2, "Expected 2 arguments for max(), got " + args.length);
        return max(args[0].asFloatValue(), args[1].asFloatValue());
    }

    /**
     * Symbolic wrapper for Float.max(float a, float b)
     * Returns the greater of two float values.
     *
     * @param a The first float
     * @param b The second float
     * @return FloatValue representing the maximum
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Float.html#max(float,float)">Float.max(float, float)</a>
     */
    private static FloatValue max(FloatValue a, FloatValue b) {
        SolverContext ctx = a.context;
        FloatingPointFormulaManager fpmgr = ctx.getFormulaManager().getFloatingPointFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();

        // Handle NaN: if either is NaN, result is NaN
        BooleanFormula aIsNaN = fpmgr.isNaN(a.formula);
        BooleanFormula bIsNaN = fpmgr.isNaN(b.formula);
        BooleanFormula aGreater = fpmgr.greaterOrEquals(a.formula, b.formula);

        // If a is NaN, return a (NaN); else if b is NaN, return b (NaN); else return max
        FloatingPointFormula result = bmgr.ifThenElse(aIsNaN, a.formula,
                bmgr.ifThenElse(bIsNaN, b.formula,
                        bmgr.ifThenElse(aGreater, a.formula, b.formula)));

        return new FloatValue(ctx, Float.max(a.concrete, b.concrete), result);
    }

    /**
     * Invocation handler for Float.min(float, float).
     */
    private static Value<?, ?> invokeMin(Value<?, ?>[] args)
            throws ValueConversionException, NotImplementedException {
        SWATAssert.enforce(args.length == 2, "Expected 2 arguments for min(), got " + args.length);
        return min(args[0].asFloatValue(), args[1].asFloatValue());
    }

    /**
     * Symbolic wrapper for Float.min(float a, float b)
     * Returns the smaller of two float values.
     *
     * @param a The first float
     * @param b The second float
     * @return FloatValue representing the minimum
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Float.html#min(float,float)">Float.min(float, float)</a>
     */
    private static FloatValue min(FloatValue a, FloatValue b) {
        SolverContext ctx = a.context;
        FloatingPointFormulaManager fpmgr = ctx.getFormulaManager().getFloatingPointFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();

        // Handle NaN: if either is NaN, result is NaN
        BooleanFormula aIsNaN = fpmgr.isNaN(a.formula);
        BooleanFormula bIsNaN = fpmgr.isNaN(b.formula);
        BooleanFormula aLessOrEqual = fpmgr.lessOrEquals(a.formula, b.formula);

        // If a is NaN, return a (NaN); else if b is NaN, return b (NaN); else return min
        FloatingPointFormula result = bmgr.ifThenElse(aIsNaN, a.formula,
                bmgr.ifThenElse(bIsNaN, b.formula,
                        bmgr.ifThenElse(aLessOrEqual, a.formula, b.formula)));

        return new FloatValue(ctx, Float.min(a.concrete, b.concrete), result);
    }

    /**
     * Invocation handler for Float.sum(float, float).
     */
    private static Value<?, ?> invokeSum(Value<?, ?>[] args)
            throws ValueConversionException, NotImplementedException {
        SWATAssert.enforce(args.length == 2, "Expected 2 arguments for sum(), got " + args.length);
        return sum(args[0].asFloatValue(), args[1].asFloatValue());
    }

    /**
     * Symbolic wrapper for Float.sum(float a, float b)
     * Adds two float values together.
     *
     * @param a The first float
     * @param b The second float
     * @return FloatValue representing the sum
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Float.html#sum(float,float)">Float.sum(float, float)</a>
     */
    private static FloatValue sum(FloatValue a, FloatValue b) {
        SolverContext ctx = a.context;
        FloatingPointFormulaManager fpmgr = ctx.getFormulaManager().getFloatingPointFormulaManager();
        return new FloatValue(ctx, Float.sum(a.concrete, b.concrete), fpmgr.add(a.formula, b.formula));
    }

    /**
     * Invocation handler for Float.isNaN(float).
     */
    private static Value<?, ?> invokeIsNaN(Value<?, ?>[] args)
            throws ValueConversionException, NotImplementedException {
        SWATAssert.enforce(args.length == 1, "Expected 1 argument for isNaN(), got " + args.length);
        return isNaN(args[0].asFloatValue());
    }

    /**
     * Symbolic wrapper for Float.isNaN(float v)
     * Returns true if the specified number is a Not-a-Number (NaN) value.
     *
     * @param v The float to test
     * @return BooleanValue representing whether v is NaN
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Float.html#isNaN(float)">Float.isNaN(float)</a>
     */
    private static BooleanValue isNaN(FloatValue v) {
        SolverContext ctx = v.context;
        FloatingPointFormulaManager fpmgr = ctx.getFormulaManager().getFloatingPointFormulaManager();
        return new BooleanValue(ctx, Float.isNaN(v.concrete), fpmgr.isNaN(v.formula));
    }

    /**
     * Invocation handler for Float.isInfinite(float).
     */
    private static Value<?, ?> invokeIsInfinite(Value<?, ?>[] args)
            throws ValueConversionException, NotImplementedException {
        SWATAssert.enforce(args.length == 1, "Expected 1 argument for isInfinite(), got " + args.length);
        return isInfinite(args[0].asFloatValue());
    }

    /**
     * Symbolic wrapper for Float.isInfinite(float v)
     * Returns true if the specified number is infinitely large in magnitude.
     *
     * @param v The float to test
     * @return BooleanValue representing whether v is infinite
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Float.html#isInfinite(float)">Float.isInfinite(float)</a>
     */
    private static BooleanValue isInfinite(FloatValue v) {
        SolverContext ctx = v.context;
        FloatingPointFormulaManager fpmgr = ctx.getFormulaManager().getFloatingPointFormulaManager();
        return new BooleanValue(ctx, Float.isInfinite(v.concrete), fpmgr.isInfinity(v.formula));
    }

    /**
     * Invocation handler for Float.isFinite(float).
     */
    private static Value<?, ?> invokeIsFinite(Value<?, ?>[] args)
            throws ValueConversionException, NotImplementedException {
        SWATAssert.enforce(args.length == 1, "Expected 1 argument for isFinite(), got " + args.length);
        return isFinite(args[0].asFloatValue());
    }

    /**
     * Symbolic wrapper for Float.isFinite(float f)
     * Returns true if the argument is a finite floating-point value.
     *
     * @param f The float to test
     * @return BooleanValue representing whether f is finite (not NaN and not infinite)
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Float.html#isFinite(float)">Float.isFinite(float)</a>
     */
    private static BooleanValue isFinite(FloatValue f) {
        SolverContext ctx = f.context;
        FloatingPointFormulaManager fpmgr = ctx.getFormulaManager().getFloatingPointFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();

        // isFinite = !isNaN && !isInfinite
        BooleanFormula isFiniteFormula = bmgr.and(
                bmgr.not(fpmgr.isNaN(f.formula)),
                bmgr.not(fpmgr.isInfinity(f.formula)));

        return new BooleanValue(ctx, Float.isFinite(f.concrete), isFiniteFormula);
    }

    /**
     * Invocation handler for Float.floatToIntBits(float).
     */
    private static Value<?, ?> invokeFloatToIntBits(Value<?, ?>[] args)
            throws ValueConversionException, NotImplementedException {
        SWATAssert.enforce(args.length == 1, "Expected 1 argument for floatToIntBits(), got " + args.length);
        return floatToIntBits(args[0].asFloatValue());
    }

    /**
     * Symbolic wrapper for Float.floatToIntBits(float value)
     * Returns a representation of the specified floating-point value according to the IEEE 754
     * floating-point "single format" bit layout.
     *
     * @param f The float to convert
     * @return IntValue representing the bit representation
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Float.html#floatToIntBits(float)">Float.floatToIntBits(float)</a>
     */
    private static IntValue floatToIntBits(FloatValue f) {
        SolverContext ctx = f.context;
        FloatingPointFormulaManager fpmgr = ctx.getFormulaManager().getFloatingPointFormulaManager();
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();

        // Convert the floating-point formula to a 32-bit bitvector representation
        // This gives us the IEEE 754 bit representation
        BitvectorFormula bvFormula = fpmgr.toIeeeBitvector(f.formula);

        return new IntValue(ctx, Float.floatToIntBits(f.concrete), bvFormula);
    }
}