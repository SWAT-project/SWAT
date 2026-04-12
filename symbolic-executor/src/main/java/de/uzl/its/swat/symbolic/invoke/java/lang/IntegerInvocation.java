package de.uzl.its.swat.symbolic.invoke.java.lang;

import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.LongValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.IntegerObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.*;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;

public class IntegerInvocation {

    public static Value<?, ?> invokeStaticMethod(String name, Value<?, ?>[] args, Type[] desc) throws ValueConversionException, NotImplementedException {
        return switch (name) {
            case "bitCode" -> invokeBitCode(args);
            case "compare" -> invokeCompare(args);
            case "compareUnsigned" -> invokeCompareUnsigned(args);
            case "decode" -> invokeDecode(args);
            case "divideUnsigned" -> invokeDivideUnsigned(args);
            case "getInteger" -> invokeGetInteger(args);
            case "hashCode" -> invokeHashCode(args);
            case "highestOneBit" -> invokeHighestOneBit(args);
            case "lowestOneBit" -> invokeLowestOneBit(args);
            case "max" -> invokeMax(args);
            case "min" -> invokeMin(args);
            case "numberOfLeadingZeros" -> invokeNumberOfLeadingZeros(args);
            case "numberOfTrailingZeros" -> invokeNumberOfTrailingZeros(args);
            case "parseInt" -> invokeParseInt(args);
            case "parseUnsignedInt" -> invokeParseUnsignedInt(args);
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
            case "toUnsignedLong" -> invokeToUnsignedLong(args);
            case "toUnsignedString" -> invokeToUnsignedString(args);
            case "valueOf" -> invokeValueOf(args);
            default -> PlaceHolder.instance;
        };
    }

    /**
     * Invocation handler for Integer.bitCode(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeBitCode(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException {
        assert args.length == 1 : "[SWAT] Expected 1 argument(s), got " + args.length;
        return PlaceHolder.instance;
    }

    /**
     * Invocation handler for Integer.compare(*), handles argument conversion.
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
        return compare(args[0].asIntValue(), args[1].asIntValue());
    }

    /**
     * Symbolic wrapper for Integer.compare(int x, int y)
     *
     * @param x The first int
     * @param y The second int
     * @return The integer result
     * @see <a
     *     href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Integer.html#compare(int,int)</a>
     */
    private static IntValue compare(IntValue x, IntValue y) {
        SolverContext context = x.context;
        BitvectorFormulaManager bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        return new IntValue(context, x.concrete - y.concrete, bvmgr.subtract(x.formula, y.formula));
    }

    /**
     * Invocation handler for Integer.compareUnsigned(*), handles argument conversion.
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
        return compareUnsigned(args[0].asIntValue(), args[1].asIntValue());
    }

    /**
     * Symbolic wrapper for Integer.compareUnsigned(int x, int y)
     * Compares two int values numerically treating the values as unsigned.
     *
     * @param x The first int
     * @param y The second int
     * @return 0 if x == y; negative if x < y (unsigned); positive if x > y (unsigned)
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Integer.html#compareUnsigned(int,int)">Integer.compareUnsigned(int, int)</a>
     */
    private static IntValue compareUnsigned(IntValue x, IntValue y) {
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

        return new IntValue(ctx, Integer.compareUnsigned(x.concrete, y.concrete), result);
    }

    /**
     * Invocation handler for Integer.decode(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeDecode(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 1 : "[SWAT] Expected 1 argument(s), got " + args.length;
        StringValue nm = args[0].asStringValue();
        if (nm.concrete.matches("^[+]?[1-9][0-9]*")) {
            // ToDo: What about other cases...
            return decode(nm);
        }
        return PlaceHolder.instance;
    }

    /**
     * Symbolic wrapper for Integer.decode(String nm)
     *
     * @param nm The String to decode
     * @return The decoded primitive wrapped in an Integer
     * @see <a
     *     href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Integer.html#decode(java.lang.String)</a>
     */
    private static IntegerObjectValue decode(StringValue nm) {
        SolverContext ctx = nm.context;
        StringFormulaManager sfm = ctx.getFormulaManager().getStringFormulaManager();
        return new IntegerObjectValue(
                ctx,
                new IntValue(ctx, Integer.decode(nm.concrete), sfm.toIntegerFormula(nm.formula)),
                ObjectValue.ADDRESS_UNKNOWN);
    }

    /**
     * Invocation handler for Integer.divideUnsigned(*), handles argument conversion.
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
        return divideUnsigned(args[0].asIntValue(), args[1].asIntValue());
    }

    /**
     * Symbolic wrapper for Integer.divideUnsigned(int dividend, int divisor)
     * Returns the unsigned quotient of dividing the first argument by the second.
     *
     * @param dividend The value to be divided
     * @param divisor The value doing the dividing
     * @return The unsigned quotient
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Integer.html#divideUnsigned(int,int)">Integer.divideUnsigned(int, int)</a>
     */
    private static IntValue divideUnsigned(IntValue dividend, IntValue divisor) {
        SolverContext ctx = dividend.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        // Unsigned division
        BitvectorFormula result = bvmgr.divide(dividend.formula, divisor.formula, false);  // false = unsigned
        return new IntValue(ctx, Integer.divideUnsigned(dividend.concrete, divisor.concrete), result);
    }

    /**
     * Invocation handler for Integer.getInteger(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeGetInteger(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException {
        assert args.length == 1 || args.length == 2
                : "[SWAT] Expected 1 or 2 argument(s), got " + args.length;
        return PlaceHolder.instance;
    }

    /**
     * Invocation handler for Integer.hashCode(*), handles argument conversion.
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
        return hashCode(args[0].asIntValue());
    }

    /**
     * Symbolic wrapper for Integer.hashCode(int value)
     * The hash code of an int is itself (identity function).
     *
     * @param value The integer value
     * @return The hash code (same as input)
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Integer.html#hashCode(int)">Integer.hashCode(int)</a>
     */
    private static IntValue hashCode(IntValue value) {
        // Integer.hashCode(int) returns the value itself
        return value;
    }

    /**
     * Invocation handler for Integer.highestOneBit(*), handles argument conversion.
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
        return highestOneBit(args[0].asIntValue());
    }

    /**
     * Symbolic wrapper for Integer.highestOneBit(int i)
     * Returns an int value with at most a single one-bit, in the position of the highest-order
     * (leftmost) one-bit in the specified int value.
     *
     * Implementation: Smear the highest bit down, then isolate it.
     * i |= (i >> 1); i |= (i >> 2); i |= (i >> 4); i |= (i >> 8); i |= (i >> 16);
     * return i - (i >>> 1);
     */
    private static IntValue highestOneBit(IntValue i) {
        SolverContext ctx = i.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();

        // Smear the highest 1-bit down to fill all lower bits
        BitvectorFormula v = i.formula;
        v = bvmgr.or(v, bvmgr.shiftRight(v, bvmgr.makeBitvector(32, 1), true));  // signed shift
        v = bvmgr.or(v, bvmgr.shiftRight(v, bvmgr.makeBitvector(32, 2), true));
        v = bvmgr.or(v, bvmgr.shiftRight(v, bvmgr.makeBitvector(32, 4), true));
        v = bvmgr.or(v, bvmgr.shiftRight(v, bvmgr.makeBitvector(32, 8), true));
        v = bvmgr.or(v, bvmgr.shiftRight(v, bvmgr.makeBitvector(32, 16), true));

        // Isolate highest bit: v - (v >>> 1)
        BitvectorFormula result = bvmgr.subtract(v, bvmgr.shiftRight(v, bvmgr.makeBitvector(32, 1), false));

        return new IntValue(ctx, Integer.highestOneBit(i.concrete), result);
    }

    /**
     * Invocation handler for Integer.lowestOneBit(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeLowestOneBit(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 1 : "[SWAT] Expected 1 argument(s), got " + args.length;
        return lowestOneBit(args[0].asIntValue());
    }

    /**
     * Symbolic wrapper for Integer.lowestOneBit(int i)
     * Returns an int value with at most a single one-bit, in the position of the lowest-order
     * (rightmost) one-bit in the specified int value.
     *
     * Implementation: i & -i (isolates the lowest set bit)
     */
    private static IntValue lowestOneBit(IntValue i) {
        SolverContext ctx = i.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();

        // lowestOneBit = i & -i
        BitvectorFormula negI = bvmgr.negate(i.formula);
        BitvectorFormula result = bvmgr.and(i.formula, negI);

        return new IntValue(ctx, Integer.lowestOneBit(i.concrete), result);
    }

    /**
     * Invocation handler for Integer.max(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeMax(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 2 : "[SWAT] Expected 2 argument(s), got " + args.length;
        return max(args[0].asIntValue(), args[1].asIntValue());
    }

    /**
     * Symbolic wrapper for Integer.max(int a, int b)
     *
     * @param a The first int
     * @param b The second int
     * @return The larger of the two parameters
     * @see <a
     *     href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Integer.html#max(int,int)">Integer.max(int
     *     a, int b)</a>
     */
    private static IntValue max(IntValue a, IntValue b) {
        SolverContext ctx = a.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        // Use signed bitvector comparison
        BooleanFormula cond = bvmgr.greaterOrEquals(a.formula, b.formula, true);
        BitvectorFormula res = bmgr.ifThenElse(cond, a.formula, b.formula);
        return new IntValue(a.context, Integer.max(a.concrete, b.concrete), res);
    }

    /**
     * Invocation handler for Integer.min(*), handles argument conversion.
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
        return min(args[0].asIntValue(), args[1].asIntValue());
    }

    /**
     * Symbolic wrapper for Integer.min(int a, int b)
     *
     * @param a The first int
     * @param b The second int
     * @return The smaller of the two parameters
     * @see <a
     *     href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Integer.html#min(int,int)">Integer.min(int
     *     a, int b)</a>
     */
    private static IntValue min(IntValue a, IntValue b) {
        SolverContext ctx = a.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        // Use signed bitvector comparison
        BooleanFormula cond = bvmgr.lessOrEquals(a.formula, b.formula, true);
        BitvectorFormula res = bmgr.ifThenElse(cond, a.formula, b.formula);
        return new IntValue(a.context, Integer.min(a.concrete, b.concrete), res);
    }

    /**
     * Invocation handler for Integer.numberOfLeadingZeros(*), handles argument conversion.
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
     * Invocation handler for Integer.numberOfTrailingZeros(*), handles argument conversion.
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
     * Invocation handler for Integer.parseInt(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeParseInt(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 1 || args.length == 2
                : "Expected 1 or 2 argument(s), got " + args.length;
        if (args.length == 1 && args[0] instanceof StringValue sv) {
            return parseInt(sv);
        }
        // parseInt with radix not yet supported
        return PlaceHolder.instance;
    }

    /**
     * Symbolic wrapper for Integer.parseInt(String s)
     * Parses the string argument as a signed decimal integer.
     *
     * @param s The String containing the integer representation to be parsed
     * @return The integer value represented by the argument
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Integer.html#parseInt(java.lang.String)">Integer.parseInt(String)</a>
     */
    private static IntValue parseInt(StringValue s) {
        SolverContext ctx = s.context;
        StringFormulaManager sfm = ctx.getFormulaManager().getStringFormulaManager();
        return new IntValue(ctx, Integer.parseInt(s.concrete), sfm.toIntegerFormula(s.formula));
    }

    /**
     * Invocation handler for Integer.parseUnsignedInt(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeParseUnsignedInt(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException {
        assert args.length == 1 || args.length == 2
                : "Expected 1 or 2 argument(s), got " + args.length;
        return PlaceHolder.instance;
    }

    /**
     * Invocation handler for Integer.remainderUnsigned(*), handles argument conversion.
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
        return remainderUnsigned(args[0].asIntValue(), args[1].asIntValue());
    }

    /**
     * Symbolic wrapper for Integer.remainderUnsigned(int dividend, int divisor)
     * Returns the unsigned remainder from dividing the first argument by the second.
     *
     * @param dividend The value to be divided
     * @param divisor The value doing the dividing
     * @return The unsigned remainder
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Integer.html#remainderUnsigned(int,int)">Integer.remainderUnsigned(int, int)</a>
     */
    private static IntValue remainderUnsigned(IntValue dividend, IntValue divisor) {
        SolverContext ctx = dividend.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        // Unsigned remainder: dividend - (dividend / divisor) * divisor
        BitvectorFormula quotient = bvmgr.divide(dividend.formula, divisor.formula, false);  // unsigned division
        BitvectorFormula product = bvmgr.multiply(quotient, divisor.formula);
        BitvectorFormula result = bvmgr.subtract(dividend.formula, product);
        return new IntValue(ctx, Integer.remainderUnsigned(dividend.concrete, divisor.concrete), result);
    }

    /**
     * Invocation handler for Integer.reverse(*), handles argument conversion.
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
     * Invocation handler for Integer.reverseBytes(*), handles argument conversion.
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
        return reverseBytes(args[0].asIntValue());
    }

    /**
     * Symbolic wrapper for Integer.reverseBytes(int i)
     * Returns the value obtained by reversing the order of the bytes.
     */
    private static IntValue reverseBytes(IntValue i) {
        SolverContext ctx = i.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        // Extract each byte and concatenate in reverse order
        BitvectorFormula b0 = bvmgr.extract(i.formula, 7, 0);    // bits 7-0
        BitvectorFormula b1 = bvmgr.extract(i.formula, 15, 8);   // bits 15-8
        BitvectorFormula b2 = bvmgr.extract(i.formula, 23, 16);  // bits 23-16
        BitvectorFormula b3 = bvmgr.extract(i.formula, 31, 24);  // bits 31-24
        // Reverse: b0 becomes most significant, b3 becomes least significant
        BitvectorFormula reversed = bvmgr.concat(b0, bvmgr.concat(b1, bvmgr.concat(b2, b3)));
        return new IntValue(ctx, Integer.reverseBytes(i.concrete), reversed);
    }

    /**
     * Invocation handler for Integer.rotateLeft(*), handles argument conversion.
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
        return rotateLeft(args[0].asIntValue(), args[1].asIntValue());
    }

    /**
     * Symbolic wrapper for Integer.rotateLeft(int i, int distance)
     * Returns the value obtained by rotating the bits left by distance.
     */
    private static IntValue rotateLeft(IntValue i, IntValue distance) {
        SolverContext ctx = i.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        BitvectorFormula result = bvmgr.rotateLeft(i.formula, distance.formula);
        return new IntValue(ctx, Integer.rotateLeft(i.concrete, distance.concrete), result);
    }

    /**
     * Invocation handler for Integer.rotateRight(*), handles argument conversion.
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
        return rotateRight(args[0].asIntValue(), args[1].asIntValue());
    }

    /**
     * Symbolic wrapper for Integer.rotateRight(int i, int distance)
     * Returns the value obtained by rotating the bits right by distance.
     */
    private static IntValue rotateRight(IntValue i, IntValue distance) {
        SolverContext ctx = i.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        BitvectorFormula result = bvmgr.rotateRight(i.formula, distance.formula);
        return new IntValue(ctx, Integer.rotateRight(i.concrete, distance.concrete), result);
    }

    /**
     * Invocation handler for Integer.signum(*), handles argument conversion.
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
        return signum(args[0].asIntValue());
    }

    /**
     * Symbolic wrapper for Integer.signum(int i)
     *
     * @param i The integer to test
     * @return The signum of the integer
     * @see <a
     *     href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Integer.html#signum(int)">Integer.signum(int
     *     i)</a>
     */
    private static IntValue signum(IntValue i) {
        SolverContext ctx = i.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        IntegerFormulaManager ifm = ctx.getFormulaManager().getIntegerFormulaManager();
        BooleanFormulaManager bfm = ctx.getFormulaManager().getBooleanFormulaManager();
        BitvectorFormula zero = bvmgr.makeBitvector(32, 0);
        return new IntValue(
                ctx,
                Integer.signum(i.concrete),
                bfm.ifThenElse(
                        bvmgr.equal(i.formula, zero),
                        ifm.makeNumber(0),
                        bfm.ifThenElse(
                                bvmgr.greaterThan(i.formula, zero, true),  // signed
                                ifm.makeNumber(1),
                                ifm.makeNumber(-1))));
    }

    /**
     * Invocation handler for Integer.sum(*), handles argument conversion.
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
        return sum(args[0].asIntValue(), args[1].asIntValue());
    }

    /**
     * Symbolic wrapper for Integer.sum(int a, int b)
     *
     * @param a The first integer
     * @param b The second integer
     * @return The sum of both arguments
     * @see <a
     *     href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Integer.html#sum(int,int)">Integer.sum(int
     *     a, int b)</a>
     */
    private static IntValue sum(IntValue a, IntValue b) {
        SolverContext ctx = a.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        return new IntValue(
                ctx, Integer.sum(a.concrete, b.concrete), bvmgr.add(a.formula, b.formula));
    }

    /**
     * Invocation handler for Integer.toBinaryString(*), handles argument conversion.
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
     * Invocation handler for Integer.toHexString(*), handles argument conversion.
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
     * Invocation handler for Integer.toOctalString(*), handles argument conversion.
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
     * Invocation handler for Integer.toString(*), handles argument conversion.
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
        if (args.length == 1 && args[0] instanceof IntValue iv) {
            return toString(iv);
        }
        // toString with radix not yet supported
        return PlaceHolder.instance;
    }

    /**
     * Symbolic wrapper for Integer.toString(int i)
     * Returns a String object representing the specified integer.
     *
     * Note: Z3's str.from_int only works with non-negative integers (returns "" for negative).
     * To handle negative numbers, we use: ite(i < 0, str.++ "-" (str.from_int |i|), str.from_int i)
     *
     * @param i The integer to be converted
     * @return A string representation of the argument in base 10
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Integer.html#toString(int)">Integer.toString(int)</a>
     */
    private static StringValue toString(IntValue i) {
        SolverContext ctx = i.context;
        StringFormulaManager sfm = ctx.getFormulaManager().getStringFormulaManager();
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        IntegerFormulaManager imgr = ctx.getFormulaManager().getIntegerFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();

        BitvectorFormula zero32 = bvmgr.makeBitvector(32, 0);

        // Check if value is negative (signed comparison)
        BooleanFormula isNegative = bvmgr.lessThan(i.formula, zero32, true);

        // Get unsigned interpretation of the bitvector
        NumeralFormula.IntegerFormula unsignedVal = bvmgr.toIntegerFormula(i.formula, false);

        // For non-negative: just convert unsigned value to string
        StringFormula nonNegStr = sfm.toStringFormula(unsignedVal);

        // For negative: absolute value = 2^32 - unsigned_value, then prepend "-"
        NumeralFormula.IntegerFormula twoTo32 = imgr.makeNumber(4294967296L);
        NumeralFormula.IntegerFormula absVal = imgr.subtract(twoTo32, unsignedVal);
        StringFormula negStr = sfm.concat(sfm.makeString("-"), sfm.toStringFormula(absVal));

        // Combine with if-then-else
        StringFormula resultFormula = bmgr.ifThenElse(isNegative, negStr, nonNegStr);

        return new StringValue(ctx, Integer.toString(i.concrete), resultFormula, ObjectValue.ADDRESS_UNKNOWN);
    }

    /**
     * Invocation handler for Integer.toUnsignedLong(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeToUnsignedLong(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 1 : "Expected 1 argument(s), got " + args.length;
        return toUnsignedLong(args[0].asIntValue());
    }

    /**
     * Symbolic wrapper for Integer.toUnsignedLong(int x)
     * Converts the argument to a long by an unsigned conversion.
     *
     * @param x The int value to convert
     * @return The argument converted to long by unsigned conversion
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Integer.html#toUnsignedLong(int)">Integer.toUnsignedLong(int)</a>
     */
    private static LongValue toUnsignedLong(IntValue x) {
        SolverContext ctx = x.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        // Zero-extend 32-bit to 64-bit (unsigned conversion)
        BitvectorFormula bv64 = bvmgr.extend(x.formula, 32, false);  // false = zero extend
        return new LongValue(ctx, Integer.toUnsignedLong(x.concrete), bv64);
    }

    /**
     * Invocation handler for Integer.toUnsignedString(*), handles argument conversion.
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
     * Invocation handler for Integer.valueOf(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeValueOf(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException {
        assert args.length == 1 || args.length == 2
                : "Expected 1 or 2 argument(s), got " + args.length;
        if (args.length == 1 && args[0] instanceof IntValue val) {
            return valueOf(val);
        }
        return PlaceHolder.instance;
    }

    /**
     * Symbolic wrapper for Integer.valueOf(int i)
     *
     * @param i The primitive int
     * @return The boxed int
     * @see <a
     *     href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Integer.html#valueOf(int)">Integer.valueOf(int
     *     i)</a>
     */
    private static IntegerObjectValue valueOf(IntValue i) {
        return new IntegerObjectValue(i.context, i, ObjectValue.ADDRESS_UNKNOWN);
    }
}
