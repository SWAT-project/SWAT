package de.uzl.its.swat.symbolic.invoke.java.lang;

import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.LongValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.ShortValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.ShortObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.StringFormula;
import org.sosy_lab.java_smt.api.StringFormulaManager;

public class ShortInvocation {

    public static Value<?, ?> invokeStaticMethod(String name, Value<?, ?>[] args, Type[] desc) throws ValueConversionException, NotImplementedException {
        return switch (name) {
            case "compare" -> invokeCompare(args);
            case "compareUnsigned" -> invokeCompareUnsigned(args);
            case "decode" -> invokeDecode(args);
            case "hashCode" -> invokeHashCode(args);
            case "parseShort" -> invokeParseShort(args);
            case "reverseBytes" -> invokeReverseBytes(args);
            case "toString" -> invokeToString(args);
            case "toUnsignedInt" -> invokeToUnsignedInt(args);
            case "toUnsignedLong" -> invokeToUnsignedLong(args);
            case "valueOf" -> invokeValueOf(args);

            default -> PlaceHolder.instance;
        };
    }
    /**
     * Invocation handler for Short.compare(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied, should not happen
     */
    private static Value<?, ?> invokeCompare(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, NotImplementedException {
        assert args.length == 2 : "[SWAT] Expected 2 argument(s), got " + args.length;
        return compare(args[0].asShortValue(), args[1].asShortValue());
    }

    /**
     * Symbolic wrapper for Short.compare(short x, short y)
     *
     * @see <a
     *     href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Short.html#compare(short,short)</a>
     * @param x The first short
     * @param y The second short
     * @return The integer result
     */
    private static IntValue compare(ShortValue x, ShortValue y) {
        // Promote both values for integer operation
        IntValue ix = x.asIntValue();
        IntValue iy = y.asIntValue();
        // Perform the subtraction, based on the implementation:
        // https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/lang/Short.java#L524
        return ix.ISUB(iy);
    }

    /**
     * Invocation handler for Short.compareUnsigned(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied (should not happen)
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeCompareUnsigned(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 2 : "[SWAT] Expected 2 argument(s), got " + args.length;
        return compareUnsigned(args[0].asShortValue(), args[1].asShortValue());
    }

    /**
     * Symbolic wrapper for Short.compareUnsigned(short x, short y)
     * Compares two short values numerically treating the values as unsigned.
     *
     * @param x The first short
     * @param y The second short
     * @return 0 if x == y; negative if x < y (unsigned); positive if x > y (unsigned)
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Short.html#compareUnsigned(short,short)">Short.compareUnsigned(short, short)</a>
     */
    private static IntValue compareUnsigned(ShortValue x, ShortValue y) {
        SolverContext ctx = x.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();

        BitvectorFormula zero = bvmgr.makeBitvector(32, 0);
        BitvectorFormula one = bvmgr.makeBitvector(32, 1);
        BitvectorFormula negOne = bvmgr.makeBitvector(32, -1);

        // Use unsigned comparison on the 16-bit values
        BooleanFormula isEqual = bvmgr.equal(x.formula, y.formula);
        BooleanFormula isLess = bvmgr.lessThan(x.formula, y.formula, false);  // false = unsigned

        BitvectorFormula result = bmgr.ifThenElse(isEqual, zero,
                bmgr.ifThenElse(isLess, negOne, one));

        return new IntValue(ctx, Short.compareUnsigned(x.concrete, y.concrete), result);
    }

    /**
     * Invocation handler for Short.decode(*), handles argument conversion. ToDo: Implement symbolic
     * handling
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied (should not happen)
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeDecode(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException {
        assert args.length == 1 : "[SWAT] Expected 1 argument(s), got " + args.length;
        return PlaceHolder.instance;
    }

    /**
     * Invocation handler for Short.hashCode(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied (should not happen)
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeHashCode(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 1 : "[SWAT] Expected 1 argument(s), got " + args.length;
        return hashCode(args[0].asShortValue());
    }

    /**
     * Symbolic wrapper for Short.hashCode(short value)
     * The hash code of a short is itself (identity function after int promotion).
     *
     * @param value The short value
     * @return The hash code (same as input, promoted to int)
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Short.html#hashCode(short)">Short.hashCode(short)</a>
     */
    private static IntValue hashCode(ShortValue value) {
        // Short.hashCode(short) returns the value itself (promoted to int)
        return value.asIntValue();
    }

    /**
     * Invocation handler for Short.parseShort(*), handles argument conversion. ToDo: Implement
     * symbolic handling
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied (should not happen)
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeParseShort(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 1 || args.length == 2
                : "[SWAT] Expected 1 or 2 argument(s), got " + args.length;
        if (args.length == 1 && args[0] instanceof StringValue sv) {
            return parseShort(sv);
        }
        // parseShort with radix not yet supported
        return PlaceHolder.instance;
    }

    /**
     * Symbolic wrapper for Short.parseShort(String s)
     * Parses the string argument as a signed decimal short.
     *
     * @param s The String containing the short representation to be parsed
     * @return The short value represented by the argument
     */
    private static ShortValue parseShort(StringValue s) {
        SolverContext ctx = s.context;
        StringFormulaManager sfm = ctx.getFormulaManager().getStringFormulaManager();
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        return new ShortValue(ctx, Short.parseShort(s.concrete),
                bvmgr.makeBitvector(16, sfm.toIntegerFormula(s.formula)));
    }

    /**
     * Invocation handler for Short.reverseBytes(*), handles argument conversion. ToDo: Implement
     * symbolic handling
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied (should not happen)
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeReverseBytes(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 1 : "[SWAT] Expected 1 argument(s), got " + args.length;
        return reverseBytes(args[0].asShortValue());
    }

    /**
     * Symbolic wrapper for Short.reverseBytes(short i)
     * Returns the value obtained by reversing the order of the bytes in the
     * two's complement representation of the specified short value.
     *
     * @param i The short value whose bytes are to be reversed
     * @return The value obtained by reversing the bytes
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Short.html#reverseBytes(short)">Short.reverseBytes(short)</a>
     */
    private static ShortValue reverseBytes(ShortValue i) {
        SolverContext ctx = i.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        // Extract high byte (bits 15-8) and low byte (bits 7-0), then swap
        BitvectorFormula highByte = bvmgr.extract(i.formula, 15, 8);  // bits 15-8
        BitvectorFormula lowByte = bvmgr.extract(i.formula, 7, 0);    // bits 7-0
        // Concatenate in reverse order: lowByte becomes high, highByte becomes low
        BitvectorFormula reversed = bvmgr.concat(lowByte, highByte);
        return new ShortValue(ctx, Short.reverseBytes(i.concrete), reversed);
    }

    /**
     * Invocation handler for Short.toString(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied (should not happen)
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeToString(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 1 : "[SWAT] Expected 1 argument(s), got " + args.length;
        if (args[0] instanceof ShortValue sv) {
            return toString(sv);
        }
        return PlaceHolder.instance;
    }

    /**
     * Symbolic wrapper for Short.toString(short s)
     * Returns a String object representing the specified short.
     *
     * Note: Z3's str.from_int only works with non-negative integers (returns "" for negative).
     * To handle negative numbers, we use: ite(i < 0, str.++ "-" (str.from_int |i|), str.from_int i)
     */
    private static StringValue toString(ShortValue s) {
        SolverContext ctx = s.context;
        StringFormulaManager sfm = ctx.getFormulaManager().getStringFormulaManager();
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        IntegerFormulaManager imgr = ctx.getFormulaManager().getIntegerFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();

        BitvectorFormula zero16 = bvmgr.makeBitvector(16, 0);

        // Check if value is negative (signed comparison)
        BooleanFormula isNegative = bvmgr.lessThan(s.formula, zero16, true);

        // Get unsigned interpretation of the bitvector
        NumeralFormula.IntegerFormula unsignedVal = bvmgr.toIntegerFormula(s.formula, false);

        // For non-negative: just convert unsigned value to string
        StringFormula nonNegStr = sfm.toStringFormula(unsignedVal);

        // For negative: absolute value = 2^16 - unsigned_value, then prepend "-"
        NumeralFormula.IntegerFormula twoTo16 = imgr.makeNumber(65536L);
        NumeralFormula.IntegerFormula absVal = imgr.subtract(twoTo16, unsignedVal);
        StringFormula negStr = sfm.concat(sfm.makeString("-"), sfm.toStringFormula(absVal));

        // Combine with if-then-else
        StringFormula resultFormula = bmgr.ifThenElse(isNegative, negStr, nonNegStr);

        return new StringValue(ctx, s.concrete.toString(), resultFormula, ObjectValue.ADDRESS_UNKNOWN);
    }

    /**
     * Invocation handler for Short.toUnsignedInt(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied (should not happen)
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeToUnsignedInt(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 1 : "[SWAT] Expected 1 argument(s), got " + args.length;
        return toUnsignedInt(args[0].asShortValue());
    }

    /**
     * Symbolic wrapper for Short.toUnsignedInt(short x)
     * Converts the argument to an int by an unsigned conversion.
     *
     * @param x The short value to convert
     * @return The argument converted to int by unsigned conversion
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Short.html#toUnsignedInt(short)">Short.toUnsignedInt(short)</a>
     */
    private static IntValue toUnsignedInt(ShortValue x) {
        SolverContext ctx = x.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        // Zero-extend 16-bit to 32-bit (unsigned conversion)
        BitvectorFormula bv32 = bvmgr.extend(x.formula, 16, false);  // false = zero extend
        return new IntValue(ctx, Short.toUnsignedInt(x.concrete), bv32);
    }

    /**
     * Invocation handler for Short.toUnsignedLong(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied (should not happen)
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeToUnsignedLong(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException, NotImplementedException {
        assert args.length == 1 : "[SWAT] Expected 1 argument(s), got " + args.length;
        return toUnsignedLong(args[0].asShortValue());
    }

    /**
     * Symbolic wrapper for Short.toUnsignedLong(short x)
     * Converts the argument to a long by an unsigned conversion.
     *
     * @param x The short value to convert
     * @return The argument converted to long by unsigned conversion
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Short.html#toUnsignedLong(short)">Short.toUnsignedLong(short)</a>
     */
    private static LongValue toUnsignedLong(ShortValue x) {
        SolverContext ctx = x.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        // Zero-extend 16-bit to 64-bit (unsigned conversion)
        BitvectorFormula bv64 = bvmgr.extend(x.formula, 48, false);  // false = zero extend
        return new LongValue(ctx, Short.toUnsignedLong(x.concrete), bv64);
    }

    /**
     * Invocation handler for Short.valueOf(*), handles argument conversion.
     *
     * @param args The invocation arguments from the shadow stack
     * @return The resulting Value
     * @throws IndexOutOfBoundsException If not enough arguments are supplied (should not happen)
     * @throws ValueConversionException Thrown if the argument cannot be converted to the desired
     *     type, likely a missing implementation
     */
    private static Value<?, ?> invokeValueOf(Value<?, ?>[] args)
            throws IndexOutOfBoundsException, ValueConversionException {
        assert args.length == 1 || args.length == 2
                : "Expected 1 or 2 argument(s), got " + args.length;
        if (args.length == 1 && args[0] instanceof ShortValue val) {
            return valueOf(val);
        }
        return PlaceHolder.instance;
    }

    /**
     * Symbolic wrapper for Short.valueOf(short s)
     *
     * @param s The primitive short
     * @return The boxed short
     * @see <a
     *     href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Short.html#valueOf(short)">Short.valueOf(short
     *     s)</a>
     */
    private static ShortObjectValue valueOf(ShortValue s) {
        return new ShortObjectValue(s.context, s, ObjectValue.ADDRESS_UNKNOWN);
    }
}
