package de.uzl.its.swat.symbolic.invoke.java.lang;

import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.BooleanValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.DoubleObjectValue;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FloatingPointFormulaManager;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula;
import org.sosy_lab.java_smt.api.SolverContext;

public class DoubleInvocation {
    public static Value<?, ?> invokeStaticMethod(
            String name,
            Value<?, ?>[] args,
            Type[] desc,
            SymbolicTraceHandler symbolicTraceHandler) throws NotImplementedException, ValueConversionException {
        return switch (name) {
            case "compare" -> invokeCompare(args);
            case "isFinite" -> invokeIsFinite(args);
            case "isInfinite" -> invokeIsInfinite(args);
            case "isNaN" -> invokeIsNaN(args);
            case "valueOf" -> invokeValueOf(args);

            default -> PlaceHolder.instance;
        };
    }

    /**
     * Invocation handler for Double.compare(double, double).
     */
    private static Value<?, ?> invokeCompare(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        SWATAssert.enforce(args.length == 2, "Expected 2 arguments for compare(), got " + args.length);
        return compare(args[0].asDoubleValue(), args[1].asDoubleValue());
    }

    /**
     * Symbolic wrapper for Double.compare(double d1, double d2)
     * Compares the two specified double values.
     * Returns:
     *   - 0 if d1 is numerically equal to d2
     *   - negative value if d1 is less than d2
     *   - positive value if d1 is greater than d2
     * NaN is considered greater than any other value (including POSITIVE_INFINITY).
     */
    private static IntValue compare(DoubleValue d1, DoubleValue d2) {
        SolverContext ctx = d1.context;
        FloatingPointFormulaManager fpmgr = ctx.getFormulaManager().getFloatingPointFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        IntegerFormulaManager imgr = ctx.getFormulaManager().getIntegerFormulaManager();

        // Check for NaN cases first
        BooleanFormula d1IsNaN = fpmgr.isNaN(d1.formula);
        BooleanFormula d2IsNaN = fpmgr.isNaN(d2.formula);

        // Regular comparisons (when neither is NaN)
        BooleanFormula isEqual = fpmgr.equalWithFPSemantics(d1.formula, d2.formula);
        BooleanFormula isLess = fpmgr.lessThan(d1.formula, d2.formula);

        // Build the result:
        // - If both are NaN: return 0
        // - If only d1 is NaN: return 1 (NaN > anything)
        // - If only d2 is NaN: return -1 (anything < NaN)
        // - If equal: return 0
        // - If d1 < d2: return -1
        // - Otherwise: return 1
        NumeralFormula.IntegerFormula normalResult = bmgr.ifThenElse(isEqual, imgr.makeNumber(0),
                bmgr.ifThenElse(isLess, imgr.makeNumber(-1), imgr.makeNumber(1)));

        NumeralFormula.IntegerFormula result = bmgr.ifThenElse(bmgr.and(d1IsNaN, d2IsNaN), imgr.makeNumber(0),
                bmgr.ifThenElse(d1IsNaN, imgr.makeNumber(1),
                        bmgr.ifThenElse(d2IsNaN, imgr.makeNumber(-1), normalResult)));

        return new IntValue(ctx, Double.compare(d1.concrete, d2.concrete), result);
    }

    /**
     * Invocation handler for Double.isNaN(double).
     */
    private static Value<?, ?> invokeIsNaN(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        SWATAssert.enforce(args.length == 1, "Expected 1 argument for isNaN(), got " + args.length);
        DoubleValue d = args[0].asDoubleValue();
        FloatingPointFormulaManager fpmgr = d.context.getFormulaManager().getFloatingPointFormulaManager();
        return new BooleanValue(d.context, Double.isNaN(d.concrete), fpmgr.isNaN(d.formula));
    }

    /**
     * Invocation handler for Double.isInfinite(double).
     */
    private static Value<?, ?> invokeIsInfinite(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        SWATAssert.enforce(args.length == 1, "Expected 1 argument for isInfinite(), got " + args.length);
        DoubleValue d = args[0].asDoubleValue();
        FloatingPointFormulaManager fpmgr = d.context.getFormulaManager().getFloatingPointFormulaManager();
        return new BooleanValue(d.context, Double.isInfinite(d.concrete), fpmgr.isInfinity(d.formula));
    }

    /**
     * Invocation handler for Double.isFinite(double).
     * Returns true if the argument is a finite floating-point value (not NaN and not infinite).
     */
    private static Value<?, ?> invokeIsFinite(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        SWATAssert.enforce(args.length == 1, "Expected 1 argument for isFinite(), got " + args.length);
        DoubleValue d = args[0].asDoubleValue();
        FloatingPointFormulaManager fpmgr = d.context.getFormulaManager().getFloatingPointFormulaManager();
        BooleanFormulaManager bmgr = d.context.getFormulaManager().getBooleanFormulaManager();
        // isFinite = !isNaN && !isInfinite
        BooleanFormula isFinite = bmgr.and(bmgr.not(fpmgr.isNaN(d.formula)), bmgr.not(fpmgr.isInfinity(d.formula)));
        return new BooleanValue(d.context, Double.isFinite(d.concrete), isFinite);
    }
    /**
     * Invocation handling for the Double static method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Double.html#valueOf(double)">valueOf</a>().
     * Returns a Double instance representing the specified double value.
     *
     * @param args List of Values that correspond to the method arguments
     * @return DoubleObjectValue representing the boxed double value
     */
    private static Value<?, ?> invokeValueOf(Value<?, ?>[] args) {
        SWATAssert.enforce(args.length == 1, "Wrong number of arguments for valueOf() method");
        if (args[0] instanceof DoubleValue dv) {
            return new DoubleObjectValue(dv.context, dv, ObjectValue.ADDRESS_UNKNOWN);
        }
        return PlaceHolder.instance;
    }
}