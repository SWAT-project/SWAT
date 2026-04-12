package de.uzl.its.swat.symbolic.invoke.java.lang;

import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.symbolic.UFs.EqualsIgnoreCaseUF;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.BooleanValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.BooleanObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import de.uzl.its.swat.thread.ThreadHandler;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.*;

import static java.lang.Thread.currentThread;

/**
 * Handles symbolic invocation of static methods on java.lang.Boolean.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Boolean.html">java.lang.Boolean</a>
 */
public class BooleanInvocation {

    public static Value<?, ?> invokeStaticMethod(String name, Value<?, ?>[] args, Type[] desc)
            throws ValueConversionException, NotImplementedException {
        return switch (name) {
            case "compare" -> invokeCompare(args);
            case "getBoolean" -> invokeGetBoolean(args);
            case "hashCode" -> invokeHashCode(args);
            case "logicalAnd" -> invokeLogicalAnd(args);
            case "logicalOr" -> invokeLogicalOr(args);
            case "logicalXor" -> invokeLogicalXor(args);
            case "parseBoolean" -> invokeParseBoolean(args);
            case "toString" -> invokeToString(args);
            case "valueOf" -> invokeValueOf(args);
            default -> PlaceHolder.instance;
        };
    }

    /**
     * Invocation handler for Boolean.compare(boolean x, boolean y).
     * Compares two boolean values.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Boolean.html#compare(boolean,boolean)">Boolean.compare(boolean, boolean)</a>
     */
    private static Value<?, ?> invokeCompare(Value<?, ?>[] args)
            throws ValueConversionException, NotImplementedException {
        assert args.length == 2 : "[SWAT] Expected 2 argument(s), got " + args.length;
        BooleanValue x = args[0].asBooleanValue();
        BooleanValue y = args[1].asBooleanValue();
        return compare(x, y);
    }

    /**
     * Symbolic wrapper for Boolean.compare(boolean x, boolean y).
     * Returns 0 if x == y, positive if x is true and y is false, negative if x is false and y is true.
     *
     * @param x The first boolean
     * @param y The second boolean
     * @return IntValue representing the comparison result
     */
    private static IntValue compare(BooleanValue x, BooleanValue y) {
        SolverContext ctx = x.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        BooleanFormulaManager bfm = ctx.getFormulaManager().getBooleanFormulaManager();

        // Boolean.compare: (x == y) ? 0 : (x ? 1 : -1)
        BooleanFormula bothEqual = bfm.equivalence(x.formula, y.formula);
        BooleanFormula xTrue = x.formula;

        BitvectorFormula zero = bvmgr.makeBitvector(32, 0);
        BitvectorFormula one = bvmgr.makeBitvector(32, 1);
        BitvectorFormula negOne = bvmgr.makeBitvector(32, -1);

        BitvectorFormula result = bfm.ifThenElse(bothEqual, zero,
                bfm.ifThenElse(xTrue, one, negOne));

        return new IntValue(ctx, Boolean.compare(x.concrete, y.concrete), result);
    }

    /**
     * Invocation handler for Boolean.getBoolean(String name).
     * Returns true if and only if the system property named by the argument exists and equals "true".
     * Not symbolically meaningful as it reads system properties.
     *
     * @param args The invocation arguments from the shadow stack
     * @return PlaceHolder since system properties are not symbolic
     */
    private static Value<?, ?> invokeGetBoolean(Value<?, ?>[] args) {
        assert args.length == 1 : "[SWAT] Expected 1 argument(s), got " + args.length;
        return PlaceHolder.instance;
    }

    /**
     * Invocation handler for Boolean.hashCode(boolean value).
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Boolean.html#hashCode(boolean)">Boolean.hashCode(boolean)</a>
     */
    private static Value<?, ?> invokeHashCode(Value<?, ?>[] args)
            throws ValueConversionException, NotImplementedException {
        assert args.length == 1 : "[SWAT] Expected 1 argument(s), got " + args.length;
        BooleanValue value = args[0].asBooleanValue();
        return hashCode(value);
    }

    /**
     * Symbolic wrapper for Boolean.hashCode(boolean value).
     * Returns 1231 if true, 1237 if false.
     *
     * @param value The boolean value
     * @return IntValue representing the hash code
     */
    private static IntValue hashCode(BooleanValue value) {
        SolverContext ctx = value.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        BooleanFormulaManager bfm = ctx.getFormulaManager().getBooleanFormulaManager();

        BitvectorFormula trueHash = bvmgr.makeBitvector(32, 1231);
        BitvectorFormula falseHash = bvmgr.makeBitvector(32, 1237);

        BitvectorFormula result = bfm.ifThenElse(value.formula, trueHash, falseHash);

        return new IntValue(ctx, Boolean.hashCode(value.concrete), result);
    }

    /**
     * Invocation handler for Boolean.logicalAnd(boolean a, boolean b).
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Boolean.html#logicalAnd(boolean,boolean)">Boolean.logicalAnd(boolean, boolean)</a>
     */
    private static Value<?, ?> invokeLogicalAnd(Value<?, ?>[] args)
            throws ValueConversionException, NotImplementedException {
        assert args.length == 2 : "[SWAT] Expected 2 argument(s), got " + args.length;
        BooleanValue a = args[0].asBooleanValue();
        BooleanValue b = args[1].asBooleanValue();
        return logicalAnd(a, b);
    }

    /**
     * Symbolic wrapper for Boolean.logicalAnd(boolean a, boolean b).
     *
     * @param a The first boolean
     * @param b The second boolean
     * @return BooleanValue representing a && b
     */
    private static BooleanValue logicalAnd(BooleanValue a, BooleanValue b) {
        SolverContext ctx = a.context;
        BooleanFormulaManager bfm = ctx.getFormulaManager().getBooleanFormulaManager();
        return new BooleanValue(ctx, a.concrete && b.concrete, bfm.and(a.formula, b.formula));
    }

    /**
     * Invocation handler for Boolean.logicalOr(boolean a, boolean b).
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Boolean.html#logicalOr(boolean,boolean)">Boolean.logicalOr(boolean, boolean)</a>
     */
    private static Value<?, ?> invokeLogicalOr(Value<?, ?>[] args)
            throws ValueConversionException, NotImplementedException {
        assert args.length == 2 : "[SWAT] Expected 2 argument(s), got " + args.length;
        BooleanValue a = args[0].asBooleanValue();
        BooleanValue b = args[1].asBooleanValue();
        return logicalOr(a, b);
    }

    /**
     * Symbolic wrapper for Boolean.logicalOr(boolean a, boolean b).
     *
     * @param a The first boolean
     * @param b The second boolean
     * @return BooleanValue representing a || b
     */
    private static BooleanValue logicalOr(BooleanValue a, BooleanValue b) {
        SolverContext ctx = a.context;
        BooleanFormulaManager bfm = ctx.getFormulaManager().getBooleanFormulaManager();
        return new BooleanValue(ctx, a.concrete || b.concrete, bfm.or(a.formula, b.formula));
    }

    /**
     * Invocation handler for Boolean.logicalXor(boolean a, boolean b).
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Boolean.html#logicalXor(boolean,boolean)">Boolean.logicalXor(boolean, boolean)</a>
     */
    private static Value<?, ?> invokeLogicalXor(Value<?, ?>[] args)
            throws ValueConversionException, NotImplementedException {
        assert args.length == 2 : "[SWAT] Expected 2 argument(s), got " + args.length;
        BooleanValue a = args[0].asBooleanValue();
        BooleanValue b = args[1].asBooleanValue();
        return logicalXor(a, b);
    }

    /**
     * Symbolic wrapper for Boolean.logicalXor(boolean a, boolean b).
     *
     * @param a The first boolean
     * @param b The second boolean
     * @return BooleanValue representing a ^ b
     */
    private static BooleanValue logicalXor(BooleanValue a, BooleanValue b) {
        SolverContext ctx = a.context;
        BooleanFormulaManager bfm = ctx.getFormulaManager().getBooleanFormulaManager();
        return new BooleanValue(ctx, a.concrete ^ b.concrete, bfm.xor(a.formula, b.formula));
    }

    /**
     * Invocation handler for Boolean.parseBoolean(String s).
     * Uses EqualsIgnoreCaseUF to properly model case-insensitive comparison with "true".
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Boolean.html#parseBoolean(java.lang.String)">Boolean.parseBoolean(String)</a>
     */
    private static Value<?, ?> invokeParseBoolean(Value<?, ?>[] args)
            throws ValueConversionException, NotImplementedException {
        assert args.length == 1 : "[SWAT] Expected 1 argument(s), got " + args.length;
        StringValue s = args[0].asStringValue();
        return parseBoolean(s);
    }

    /**
     * Symbolic wrapper for Boolean.parseBoolean(String s).
     * Returns true if the string argument equals "true" (case-insensitive).
     * Implemented using EqualsIgnoreCaseUF.
     *
     * @param s The string to parse
     * @return BooleanValue representing the parsed boolean
     */
    private static BooleanValue parseBoolean(StringValue s) {
        SolverContext ctx = s.context;
        StringFormulaManager sfm = ctx.getFormulaManager().getStringFormulaManager();

        // Fast path: both concrete
        if (!s.isSymbolic()) {
            return new BooleanValue(ctx, Boolean.parseBoolean(s.concrete));
        }

        try {
            // Boolean.parseBoolean returns "true".equalsIgnoreCase(s)
            StringFormula trueStr = sfm.makeString("true");

            EqualsIgnoreCaseUF uf = ThreadHandler.getUFHandler(currentThread().getId()).getEqualsIgnoreCaseUF();

            // The symbolic boolean result
            BooleanFormula ufCall = uf.getUFCall(trueStr, s.formula);

            // Create and add constraints that define the UF behavior
            BooleanFormula constraints = uf.createEqualsIgnoreCaseConstraints(
                    trueStr, s.formula,
                    "true", s.concrete,
                    false, s.isSymbolic()  // "true" is not symbolic, s may be
            );
            ThreadHandler.getSymbolicTraceHandler(currentThread().getId()).addUF(constraints);

            // Return with concrete result for concolic steering
            boolean concreteResult = Boolean.parseBoolean(s.concrete);
            return new BooleanValue(ctx, concreteResult, ufCall);

        } catch (Exception e) {
            // Fallback to concrete-only if UF setup fails
            return new BooleanValue(ctx, Boolean.parseBoolean(s.concrete));
        }
    }

    /**
     * Invocation handler for Boolean.toString(boolean b).
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Boolean.html#toString(boolean)">Boolean.toString(boolean)</a>
     */
    private static Value<?, ?> invokeToString(Value<?, ?>[] args)
            throws ValueConversionException, NotImplementedException {
        assert args.length == 1 : "[SWAT] Expected 1 argument(s), got " + args.length;
        BooleanValue b = args[0].asBooleanValue();
        return toString(b);
    }

    /**
     * Symbolic wrapper for Boolean.toString(boolean b).
     * Returns "true" if b is true, "false" otherwise.
     *
     * @param b The boolean value
     * @return StringValue representing "true" or "false"
     */
    private static StringValue toString(BooleanValue b) {
        SolverContext ctx = b.context;
        BooleanFormulaManager bfm = ctx.getFormulaManager().getBooleanFormulaManager();
        StringFormulaManager sfm = ctx.getFormulaManager().getStringFormulaManager();

        StringFormula sf = bfm.ifThenElse(
                b.formula,
                sfm.makeString("true"),
                sfm.makeString("false")
        );

        return new StringValue(ctx, Boolean.toString(b.concrete), sf, ObjectValue.ADDRESS_UNKNOWN);
    }

    /**
     * Invocation handler for Boolean.valueOf(*).
     * Handles both valueOf(boolean) and valueOf(String).
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Boolean.html#valueOf(boolean)">Boolean.valueOf(boolean)</a>
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Boolean.html#valueOf(java.lang.String)">Boolean.valueOf(String)</a>
     */
    private static Value<?, ?> invokeValueOf(Value<?, ?>[] args)
            throws ValueConversionException, NotImplementedException {
        assert args.length == 1 : "[SWAT] Expected 1 argument(s), got " + args.length;

        if (args[0] instanceof BooleanValue bv) {
            return valueOfBoolean(bv);
        } else if (args[0] instanceof StringValue sv) {
            return valueOfString(sv);
        }

        // Try to convert to BooleanValue
        try {
            BooleanValue bv = args[0].asBooleanValue();
            return valueOfBoolean(bv);
        } catch (Exception e) {
            // Try StringValue
            try {
                StringValue sv = args[0].asStringValue();
                return valueOfString(sv);
            } catch (Exception e2) {
                return PlaceHolder.instance;
            }
        }
    }

    /**
     * Symbolic wrapper for Boolean.valueOf(boolean b).
     *
     * @param b The primitive boolean
     * @return The boxed Boolean
     */
    private static BooleanObjectValue valueOfBoolean(BooleanValue b) {
        return new BooleanObjectValue(b.context, b, ObjectValue.ADDRESS_UNKNOWN);
    }

    /**
     * Symbolic wrapper for Boolean.valueOf(String s).
     *
     * @param s The string to parse
     * @return The boxed Boolean
     */
    private static BooleanObjectValue valueOfString(StringValue s) {
        BooleanValue parsed = parseBoolean(s);
        return new BooleanObjectValue(s.context, parsed, ObjectValue.ADDRESS_UNKNOWN);
    }
}
