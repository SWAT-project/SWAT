package de.uzl.its.swat.symbolic.invoke.java.lang;

import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.LongValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.LongObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.StringFormula;
import org.sosy_lab.java_smt.api.StringFormulaManager;

public class LongInvocation {
    public static Value<?, ?> invokeStaticMethod(
            String name,
            Value<?, ?>[] args,
            Type[] desc,
            SymbolicTraceHandler symbolicTraceHandler) throws ValueConversionException, NotImplementedException {
        return switch (name) {
            case "bitCount" -> invokeBitCount(args);
            case "compare" -> invokeCompare(args);
            case "compareUnsigned" -> invokeCompareUnsigned(args);
            case "decode" -> invokeDecode(args);
            case "divideUnsigned" -> invokeDivideUnsigned(args);
            case "getLong" -> invokeGetLong(args);
            case "hashCode" -> invokeHashCode(args);
            case "highestOneBit" -> invokeHighestOneBit(args);
            case "lowestOneBit" -> invokeLowestOneBit(args);
            case "max" -> invokeMax(args);
            case "min" -> invokeMin(args);
            case "numberOfLeadingZeros" -> invokeNumberOfLeadingZeros(args);
            case "numberOfTrailingZeros" -> invokeNumberOfTrailingZeros(args);
            case "parseLong" -> invokeParseLong(args);
            case "parseUnsignedLong" -> invokeParseUnsignedLong(args);
            case "remainderUnsigned" -> invokeRemainderUnsigned(args);
            case "reverse" -> invokeReverse(args);
            case "reverseBytes" -> invokeReverseBytes(args);
            case "rotateLeft" -> invokeRotateLeft(args);
            case "rotateRight" -> invokeRotateRight(args);
            case "signum" -> invokeSignum(args);
            case "sum" -> invokeSum(args);
            case "toBinaryString" -> invokeToBinaryString(args);
            case "toHexString" -> invokeToHexString(args);
            case "toOctalString" -> invokeToOctalString(args);
            case "toString" -> invokeToString(args);
            case "toUnsignedString" -> invokeToUnsignedString(args);
            case "valueOf" -> invokeValueOf(args);
            default -> PlaceHolder.instance;
        };
    }

    /**
     * Invocation handler for Long.bitCount(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeBitCount(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException {
        assert args.length == 1 : "[SWAT] Expected 1 argument(s), got " + args.length;
        return PlaceHolder.instance;
    }
    /**
     * Invocation handler for Long.compare(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeCompare(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 2 : "[SWAT] Expected 2 argument(s), got " + args.length;
        return compare(args[0].asLongValue(), args[1].asLongValue());
    }

    /**
     * Symbolic wrapper for Long.compare(long x, long y)
     *
     * @see <a
     *     href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Long.html#compare(long,long)">Long.compare(long
     *     x, long y)</a>
     * @param x The first long
     * @param y The second long
     * @return The integer result
     */
    private static IntValue compare(LongValue x, LongValue y) {
        SolverContext ctx = x.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        IntegerFormulaManager ifm = ctx.getFormulaManager().getIntegerFormulaManager();
        BooleanFormulaManager bfm = ctx.getFormulaManager().getBooleanFormulaManager();
        // Based on the following implementation:
        // return (x < y) ? -1 : ((x == y) ? 0 : 1);
        // https://github.com/openjdk/jdk/blob/309b929147e7dddfa27879ff31b1eaad271def85/src/java.base/share/classes/java/lang/Long.java#L1425
        // Use signed bitvector comparison
        return new IntValue(
                ctx,
                Long.compare(x.concrete, y.concrete),
                bfm.ifThenElse(
                        bvmgr.lessThan(x.formula, y.formula, true),  // signed
                        ifm.makeNumber(-1),
                        bfm.ifThenElse(
                                bvmgr.equal(x.formula, y.formula),
                                ifm.makeNumber(0),
                                ifm.makeNumber(1))));
    }

    /**
     * Invocation handler for Long.compareUnsigned(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeCompareUnsigned(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 2 : "[SWAT] Expected 2 argument(s), got " + args.length;
        return compareUnsigned(args[0].asLongValue(), args[1].asLongValue());
    }

    /**
     * Symbolic wrapper for Long.compareUnsigned(long x, long y)
     * Compares two long values numerically treating the values as unsigned.
     *
     * @param x The first long
     * @param y The second long
     * @return 0 if x == y; negative if x < y (unsigned); positive if x > y (unsigned)
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Long.html#compareUnsigned(long,long)">Long.compareUnsigned(long, long)</a>
     */
    private static IntValue compareUnsigned(LongValue x, LongValue y) {
        SolverContext ctx = x.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();

        BitvectorFormula zero = bvmgr.makeBitvector(32, 0);
        BitvectorFormula one = bvmgr.makeBitvector(32, 1);
        BitvectorFormula negOne = bvmgr.makeBitvector(32, -1);

        // Use unsigned comparison
        BooleanFormula isEqual = bvmgr.equal(x.formula, y.formula);
        BooleanFormula isLess = bvmgr.lessThan(x.formula, y.formula, false);  // false = unsigned

        BitvectorFormula result = bmgr.ifThenElse(isEqual, zero,
                bmgr.ifThenElse(isLess, negOne, one));

        return new IntValue(ctx, Long.compareUnsigned(x.concrete, y.concrete), result);
    }
    /**
     * Invocation handler for Long.decode(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeDecode(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException {
        assert args.length == 1 : "[SWAT] Expected 1 argument(s), got " + args.length;
        return PlaceHolder.instance;
    }
    /**
     * Invocation handler for Long.divideUnsigned(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeDivideUnsigned(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 2 : "[SWAT] Expected 2 argument(s), got " + args.length;
        return divideUnsigned(args[0].asLongValue(), args[1].asLongValue());
    }

    /**
     * Symbolic wrapper for Long.divideUnsigned(long dividend, long divisor)
     * Returns the unsigned quotient of dividing the first argument by the second.
     *
     * @param dividend The value to be divided
     * @param divisor The value doing the dividing
     * @return The unsigned quotient
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Long.html#divideUnsigned(long,long)">Long.divideUnsigned(long, long)</a>
     */
    private static LongValue divideUnsigned(LongValue dividend, LongValue divisor) {
        SolverContext ctx = dividend.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        // Unsigned division
        BitvectorFormula result = bvmgr.divide(dividend.formula, divisor.formula, false);  // false = unsigned
        return new LongValue(ctx, Long.divideUnsigned(dividend.concrete, divisor.concrete), result);
    }
    /**
     * Invocation handler for Long.getLong(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeGetLong(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException {
        assert args.length == 1 || args.length == 2
                : "[SWAT] Expected 1 or 2 argument(s), got " + args.length;
        return PlaceHolder.instance;
    }
    /**
     * Invocation handler for Long.hashCode(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeHashCode(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 1 : "[SWAT] Expected 1 argument(s), got " + args.length;
        return hashCode(args[0].asLongValue());
    }

    /**
     * Symbolic wrapper for Long.hashCode(long value)
     * Returns (int)(value ^ (value >>> 32))
     *
     * @param value The long value
     * @return The hash code
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Long.html#hashCode(long)">Long.hashCode(long)</a>
     */
    private static IntValue hashCode(LongValue value) {
        SolverContext ctx = value.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        // (int)(value ^ (value >>> 32))
        BitvectorFormula shifted = bvmgr.shiftRight(value.formula, bvmgr.makeBitvector(64, 32), false);  // logical shift
        BitvectorFormula xored = bvmgr.xor(value.formula, shifted);
        // Extract low 32 bits
        BitvectorFormula result = bvmgr.extract(xored, 31, 0);
        return new IntValue(ctx, Long.hashCode(value.concrete), result);
    }
    /**
     * Invocation handler for Long.highestOneBit(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeHighestOneBit(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 1 : "[SWAT] Expected 1 argument(s), got " + args.length;
        return highestOneBit(args[0].asLongValue());
    }

    /**
     * Symbolic wrapper for Long.highestOneBit(long i)
     * Returns a long value with at most a single one-bit, in the position of the highest-order
     * (leftmost) one-bit in the specified long value.
     */
    private static LongValue highestOneBit(LongValue i) {
        SolverContext ctx = i.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();

        // Smear the highest 1-bit down to fill all lower bits
        BitvectorFormula v = i.formula;
        v = bvmgr.or(v, bvmgr.shiftRight(v, bvmgr.makeBitvector(64, 1), true));
        v = bvmgr.or(v, bvmgr.shiftRight(v, bvmgr.makeBitvector(64, 2), true));
        v = bvmgr.or(v, bvmgr.shiftRight(v, bvmgr.makeBitvector(64, 4), true));
        v = bvmgr.or(v, bvmgr.shiftRight(v, bvmgr.makeBitvector(64, 8), true));
        v = bvmgr.or(v, bvmgr.shiftRight(v, bvmgr.makeBitvector(64, 16), true));
        v = bvmgr.or(v, bvmgr.shiftRight(v, bvmgr.makeBitvector(64, 32), true));

        // Isolate highest bit: v - (v >>> 1)
        BitvectorFormula result = bvmgr.subtract(v, bvmgr.shiftRight(v, bvmgr.makeBitvector(64, 1), false));

        return new LongValue(ctx, Long.highestOneBit(i.concrete), result);
    }

    /**
     * Invocation handler for Long.lowestOneBit(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeLowestOneBit(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 1 : "Expected 1 argument(s), got " + args.length;
        return lowestOneBit(args[0].asLongValue());
    }

    /**
     * Symbolic wrapper for Long.lowestOneBit(long i)
     * Returns a long value with at most a single one-bit, in the position of the lowest-order
     * (rightmost) one-bit in the specified long value.
     */
    private static LongValue lowestOneBit(LongValue i) {
        SolverContext ctx = i.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();

        // lowestOneBit = i & -i
        BitvectorFormula negI = bvmgr.negate(i.formula);
        BitvectorFormula result = bvmgr.and(i.formula, negI);

        return new LongValue(ctx, Long.lowestOneBit(i.concrete), result);
    }

    /**
     * Invocation handler for Long.max(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeMax(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 2 : "Expected 2 arguments, got " + args.length;
        return max(args[0].asLongValue(), args[1].asLongValue());
    }

    /**
     * Symbolic wrapper for Long.max(long a, ,long b)
     *
     * @see <a
     *     href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Long.html#max(long,long)">Long.max(long
     *     a, long b)</a>
     * @param a The first long
     * @param b The second long
     * @return The larger of the two parameters
     */
    private static LongValue max(LongValue a, LongValue b) {
        SolverContext ctx = a.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        // Use signed bitvector comparison
        BooleanFormula cond = bvmgr.greaterOrEquals(a.formula, b.formula, true);
        BitvectorFormula res = bmgr.ifThenElse(cond, a.formula, b.formula);
        return new LongValue(a.context, Long.max(a.concrete, b.concrete), res);
    }

    /**
     * Invocation handler for Long.min(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeMin(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 2 : "Expected 2 argument(s), got " + args.length;
        return min(args[0].asLongValue(), args[1].asLongValue());
    }

    /**
     * Symbolic wrapper for Long.min(long a, long b)
     *
     * @see <a
     *     href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Long.html#min(long,long)">Long.max(long
     *     a, long b)</a>
     * @param a The first long
     * @param b The second long
     * @return The smaller of the two parameters
     */
    private static LongValue min(LongValue a, LongValue b) {
        SolverContext ctx = a.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        // Use signed bitvector comparison
        BooleanFormula cond = bvmgr.lessOrEquals(a.formula, b.formula, true);
        BitvectorFormula res = bmgr.ifThenElse(cond, a.formula, b.formula);
        return new LongValue(a.context, Long.min(a.concrete, b.concrete), res);
    }

    /**
     * Invocation handler for Long.numberOfLeadingZeros(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeNumberOfLeadingZeros(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException {
        assert args.length == 1 : "Expected 1 argument(s), got " + args.length;
        return PlaceHolder.instance;
    }

    /**
     * Invocation handler for Long.numberOfTrailingZeros(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeNumberOfTrailingZeros(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException {
        assert args.length == 1 : "Expected 1 argument(s), got " + args.length;
        return PlaceHolder.instance;
    }

    /**
     * Invocation handler for Long.parseLong(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeParseLong(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 1 || args.length == 2
                : "Expected 1 or 2 argument(s), got " + args.length;
        if (args.length == 1 && args[0] instanceof StringValue sv) {
            return parseLong(sv);
        }
        // parseLong with radix not yet supported
        return PlaceHolder.instance;
    }

    /**
     * Symbolic wrapper for Long.parseLong(String s)
     * Parses the string argument as a signed decimal long.
     *
     * @param s The String containing the long representation to be parsed
     * @return The long value represented by the argument
     */
    private static LongValue parseLong(StringValue s) {
        SolverContext ctx = s.context;
        StringFormulaManager sfm = ctx.getFormulaManager().getStringFormulaManager();
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        return new LongValue(ctx, Long.parseLong(s.concrete),
                bvmgr.makeBitvector(64, sfm.toIntegerFormula(s.formula)));
    }

    /**
     * Invocation handler for Long.parseUnsignedLong(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeParseUnsignedLong(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException {
        assert args.length == 1 || args.length == 2
                : "Expected 1 or 2 argument(s), got " + args.length;
        return PlaceHolder.instance;
    }

    /**
     * Invocation handler for Long.remainderUnsigned(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeRemainderUnsigned(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 2 : "Expected 2 argument(s), got " + args.length;
        return remainderUnsigned(args[0].asLongValue(), args[1].asLongValue());
    }

    /**
     * Symbolic wrapper for Long.remainderUnsigned(long dividend, long divisor)
     * Returns the unsigned remainder from dividing the first argument by the second.
     *
     * @param dividend The value to be divided
     * @param divisor The value doing the dividing
     * @return The unsigned remainder
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Long.html#remainderUnsigned(long,long)">Long.remainderUnsigned(long, long)</a>
     */
    private static LongValue remainderUnsigned(LongValue dividend, LongValue divisor) {
        SolverContext ctx = dividend.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        // Unsigned remainder: dividend - (dividend / divisor) * divisor
        BitvectorFormula quotient = bvmgr.divide(dividend.formula, divisor.formula, false);  // unsigned division
        BitvectorFormula product = bvmgr.multiply(quotient, divisor.formula);
        BitvectorFormula result = bvmgr.subtract(dividend.formula, product);
        return new LongValue(ctx, Long.remainderUnsigned(dividend.concrete, divisor.concrete), result);
    }

    /**
     * Invocation handler for Long.reverse(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeReverse(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException {
        assert args.length == 1 : "Expected 1 argument(s), got " + args.length;
        return PlaceHolder.instance;
    }

    /**
     * Invocation handler for Long.reverseBytes(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeReverseBytes(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 1 : "Expected 1 argument(s), got " + args.length;
        return reverseBytes(args[0].asLongValue());
    }

    /**
     * Symbolic wrapper for Long.reverseBytes(long i)
     * Returns the value obtained by reversing the order of the bytes.
     */
    private static LongValue reverseBytes(LongValue i) {
        SolverContext ctx = i.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        // Extract each byte and concatenate in reverse order
        BitvectorFormula b0 = bvmgr.extract(i.formula, 7, 0);    // bits 7-0
        BitvectorFormula b1 = bvmgr.extract(i.formula, 15, 8);   // bits 15-8
        BitvectorFormula b2 = bvmgr.extract(i.formula, 23, 16);  // bits 23-16
        BitvectorFormula b3 = bvmgr.extract(i.formula, 31, 24);  // bits 31-24
        BitvectorFormula b4 = bvmgr.extract(i.formula, 39, 32);  // bits 39-32
        BitvectorFormula b5 = bvmgr.extract(i.formula, 47, 40);  // bits 47-40
        BitvectorFormula b6 = bvmgr.extract(i.formula, 55, 48);  // bits 55-48
        BitvectorFormula b7 = bvmgr.extract(i.formula, 63, 56);  // bits 63-56
        // Reverse: b0 becomes most significant, b7 becomes least significant
        BitvectorFormula reversed = bvmgr.concat(b0, bvmgr.concat(b1, bvmgr.concat(b2, bvmgr.concat(b3,
                bvmgr.concat(b4, bvmgr.concat(b5, bvmgr.concat(b6, b7)))))));
        return new LongValue(ctx, Long.reverseBytes(i.concrete), reversed);
    }

    /**
     * Invocation handler for Long.rotateLeft(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeRotateLeft(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 2 : "Expected 2 argument(s), got " + args.length;
        return rotateLeft(args[0].asLongValue(), args[1].asIntValue());
    }

    /**
     * Symbolic wrapper for Long.rotateLeft(long i, int distance)
     * Returns the value obtained by rotating the bits left by distance.
     */
    private static LongValue rotateLeft(LongValue i, IntValue distance) {
        SolverContext ctx = i.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        // Extend distance to 64 bits for the operation
        BitvectorFormula dist64 = bvmgr.extend(distance.formula, 32, false);
        BitvectorFormula result = bvmgr.rotateLeft(i.formula, dist64);
        return new LongValue(ctx, Long.rotateLeft(i.concrete, distance.concrete), result);
    }

    /**
     * Invocation handler for Long.rotateRight(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeRotateRight(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 2 : "Expected 2 argument(s), got " + args.length;
        return rotateRight(args[0].asLongValue(), args[1].asIntValue());
    }

    /**
     * Symbolic wrapper for Long.rotateRight(long i, int distance)
     * Returns the value obtained by rotating the bits right by distance.
     */
    private static LongValue rotateRight(LongValue i, IntValue distance) {
        SolverContext ctx = i.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        // Extend distance to 64 bits for the operation
        BitvectorFormula dist64 = bvmgr.extend(distance.formula, 32, false);
        BitvectorFormula result = bvmgr.rotateRight(i.formula, dist64);
        return new LongValue(ctx, Long.rotateRight(i.concrete, distance.concrete), result);
    }

    /**
     * Invocation handler for Long.signum(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeSignum(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 1 : "Expected 1 argument(s), got " + args.length;
        return signum(args[0].asLongValue());
    }

    /**
     * Symbolic wrapper for Long.signum(long i)
     *
     * @see <a
     *     href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Long.html#signum(long)">Long.signum(long
     *     i)</a>
     * @param i The long to test
     * @return The signum of the long
     */
    private static IntValue signum(LongValue i) {
        SolverContext ctx = i.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        IntegerFormulaManager ifm = ctx.getFormulaManager().getIntegerFormulaManager();
        BooleanFormulaManager bfm = ctx.getFormulaManager().getBooleanFormulaManager();
        BitvectorFormula zero = bvmgr.makeBitvector(64, 0);
        return new IntValue(
                ctx,
                Long.signum(i.concrete),
                bfm.ifThenElse(
                        bvmgr.equal(i.formula, zero),
                        ifm.makeNumber(0),
                        bfm.ifThenElse(
                                bvmgr.greaterThan(i.formula, zero, true),  // signed
                                ifm.makeNumber(1),
                                ifm.makeNumber(-1))));
    }

    /**
     * Invocation handler for Long.sum(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeSum(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 2 : "Expected 2 argument(s), got " + args.length;
        return sum(args[0].asLongValue(), args[1].asLongValue());
    }

    /**
     * Symbolic wrapper for Long.sum(long a, long b)
     *
     * @see <a
     *     href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Long.html#sum(long,long)">Long.sum(long
     *     a, long b)</a>
     * @param a The first long
     * @param b The second long
     * @return The sum of both arguments
     */
    private static LongValue sum(LongValue a, LongValue b) {
        SolverContext ctx = a.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        return new LongValue(ctx, Long.sum(a.concrete, b.concrete), bvmgr.add(a.formula, b.formula));
    }
    /**
     * Invocation handler for Long.toBinaryString(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeToBinaryString(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException {
        assert args.length == 1 : "Expected 1 argument(s), got " + args.length;
        return PlaceHolder.instance;
    }

    /**
     * Invocation handler for Long.toHexString(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeToHexString(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException {
        assert args.length == 1 : "Expected 1 argument(s), got " + args.length;
        return PlaceHolder.instance;
    }

    /**
     * Invocation handler for Long.toOctalString(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeToOctalString(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException {
        assert args.length == 1 : "Expected 1 argument(s), got " + args.length;
        return PlaceHolder.instance;
    }

    /**
     * Invocation handler for Long.toString(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeToString(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 1 || args.length == 2
                : "Expected 1 or 2 argument(s), got " + args.length;
        if (args.length == 1 && args[0] instanceof LongValue lv) {
            return toString(lv);
        }
        // toString with radix not yet supported
        return PlaceHolder.instance;
    }

    /**
     * Symbolic wrapper for Long.toString(long i)
     * Returns a String object representing the specified long.
     *
     * Note: Z3's str.from_int only works with non-negative integers (returns "" for negative).
     * To handle negative numbers, we use: ite(i < 0, str.++ "-" (str.from_int |i|), str.from_int i)
     *
     * @param l The long to be converted
     * @return A string representation of the argument in base 10
     */
    private static StringValue toString(LongValue l) {
        SolverContext ctx = l.context;
        StringFormulaManager sfm = ctx.getFormulaManager().getStringFormulaManager();
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        IntegerFormulaManager imgr = ctx.getFormulaManager().getIntegerFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();

        BitvectorFormula zero64 = bvmgr.makeBitvector(64, 0);

        // Check if value is negative (signed comparison)
        BooleanFormula isNegative = bvmgr.lessThan(l.formula, zero64, true);

        // Get unsigned interpretation of the bitvector
        NumeralFormula.IntegerFormula unsignedVal = bvmgr.toIntegerFormula(l.formula, false);

        // For non-negative: just convert unsigned value to string
        StringFormula nonNegStr = sfm.toStringFormula(unsignedVal);

        // For negative: absolute value = 2^64 - unsigned_value, then prepend "-"
        // Note: 2^64 is too large for long, so we use BigInteger via string
        NumeralFormula.IntegerFormula twoTo64 = imgr.makeNumber(new java.math.BigInteger("18446744073709551616"));
        NumeralFormula.IntegerFormula absVal = imgr.subtract(twoTo64, unsignedVal);
        StringFormula negStr = sfm.concat(sfm.makeString("-"), sfm.toStringFormula(absVal));

        // Combine with if-then-else
        StringFormula resultFormula = bmgr.ifThenElse(isNegative, negStr, nonNegStr);

        return new StringValue(ctx, Long.toString(l.concrete), resultFormula, ObjectValue.ADDRESS_UNKNOWN);
    }

    /**
     * Invocation handler for Long.toUnsignedString(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeToUnsignedString(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException {
        assert args.length == 1 || args.length == 2
                : "Expected 1 or 2 argument(s), got " + args.length;
        return PlaceHolder.instance;
    }

    /**
     * Invocation handler for Long.valueOf(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeValueOf(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 1 || args.length == 2
                : "Expected 1 or 2 argument(s), got " + args.length;
        if (args.length == 1 && args[0] instanceof LongValue val) {
            return new LongObjectValue(val.context, val, ObjectValue.ADDRESS_UNKNOWN);
        }
        return PlaceHolder.instance;
    }
}
