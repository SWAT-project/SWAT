package de.uzl.its.swat.symbolic.invoke.java.lang;

import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.ByteValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.LongValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.ByteObjectValue;
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

public class ByteInvocation {

    public static Value<?, ?> invokeStaticMethod(
            String name,
            Value<?, ?>[] args,
            Type[] desc,
            SymbolicTraceHandler symbolicStateHandler) throws NotImplementedException, ValueConversionException {
        return switch (name) {
            case "compare" -> invokeCompare(args);
            case "parseByte" -> invokeParseByte(args);
            case "toString" -> invokeToString(args);
            case "toUnsignedInt" -> invokeToUnsignedInt(args);
            case "toUnsignedLong" -> invokeToUnsignedLong(args);
            case "valueOf" -> invokeValueOf(args);
            default -> PlaceHolder.instance;
        };
    }

    /**
     * Invocation handler for Byte.compare(byte, byte).
     */
    private static Value<?, ?> invokeCompare(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        assert args.length == 2 : "Expected 2 arguments for compare(), got " + args.length;
        return compare(args[0].asByteValue(), args[1].asByteValue());
    }

    /**
     * Symbolic wrapper for Byte.compare(byte x, byte y)
     * Compares two byte values numerically.
     * Returns the value x - y.
     *
     * @param x The first byte
     * @param y The second byte
     * @return The value x - y
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Byte.html#compare(byte,byte)">Byte.compare(byte, byte)</a>
     */
    private static IntValue compare(ByteValue x, ByteValue y) {
        SolverContext ctx = x.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        // Byte.compare returns x - y (treating bytes as signed 8-bit values)
        // Need to sign-extend to 32-bit for the result
        return new IntValue(ctx, Byte.compare(x.concrete, y.concrete),
                bvmgr.subtract(
                    bvmgr.extend(x.formula, 24, true),  // sign-extend byte (8-bit) to int (32-bit)
                    bvmgr.extend(y.formula, 24, true)));
    }

    /**
     * Invocation handler for Byte.toUnsignedInt(byte).
     */
    private static Value<?, ?> invokeToUnsignedInt(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        assert args.length == 1 : "Expected 1 argument for toUnsignedInt(), got " + args.length;
        ByteValue b = args[0].asByteValue();
        SolverContext ctx = b.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        // Zero-extend byte (8-bit) to int (32-bit) for unsigned conversion
        return new IntValue(ctx, Byte.toUnsignedInt(b.concrete),
                bvmgr.extend(b.formula, 24, false));  // false = zero-extend
    }

    /**
     * Invocation handler for Byte.toUnsignedLong(byte).
     */
    private static Value<?, ?> invokeToUnsignedLong(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        assert args.length == 1 : "Expected 1 argument for toUnsignedLong(), got " + args.length;
        ByteValue b = args[0].asByteValue();
        SolverContext ctx = b.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        // Zero-extend byte (8-bit) to long (64-bit) for unsigned conversion
        return new LongValue(ctx, Byte.toUnsignedLong(b.concrete),
                bvmgr.extend(b.formula, 56, false));  // false = zero-extend
    }

    /**
     * Invocation handler for Byte.valueOf(byte).
     */
    private static Value<?, ?> invokeValueOf(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        assert args.length == 1 : "Expected 1 argument for valueOf(), got " + args.length;
        ByteValue b = args[0].asByteValue();
        return new ByteObjectValue(b.context, b, ObjectValue.ADDRESS_UNKNOWN);
    }

    /**
     * Invocation handler for Byte.parseByte(String).
     */
    private static Value<?, ?> invokeParseByte(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        assert args.length == 1 || args.length == 2 : "Expected 1 or 2 arguments for parseByte(), got " + args.length;
        if (args.length == 1 && args[0] instanceof StringValue sv) {
            return parseByte(sv);
        }
        return PlaceHolder.instance;
    }

    /**
     * Symbolic wrapper for Byte.parseByte(String s)
     */
    private static ByteValue parseByte(StringValue s) {
        SolverContext ctx = s.context;
        StringFormulaManager sfm = ctx.getFormulaManager().getStringFormulaManager();
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        return new ByteValue(ctx, Byte.parseByte(s.concrete),
                bvmgr.makeBitvector(8, sfm.toIntegerFormula(s.formula)));
    }

    /**
     * Invocation handler for Byte.toString(byte).
     */
    private static Value<?, ?> invokeToString(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        assert args.length == 1 : "Expected 1 argument for toString(), got " + args.length;
        if (args[0] instanceof ByteValue bv) {
            return toString(bv);
        }
        return PlaceHolder.instance;
    }

    /**
     * Symbolic wrapper for Byte.toString(byte b)
     *
     * Note: Z3's str.from_int only works with non-negative integers (returns "" for negative).
     * To handle negative numbers, we use: ite(i < 0, str.++ "-" (str.from_int |i|), str.from_int i)
     */
    private static StringValue toString(ByteValue b) {
        SolverContext ctx = b.context;
        StringFormulaManager sfm = ctx.getFormulaManager().getStringFormulaManager();
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        IntegerFormulaManager imgr = ctx.getFormulaManager().getIntegerFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();

        BitvectorFormula zero8 = bvmgr.makeBitvector(8, 0);

        // Check if value is negative (signed comparison)
        BooleanFormula isNegative = bvmgr.lessThan(b.formula, zero8, true);

        // Get unsigned interpretation of the bitvector
        NumeralFormula.IntegerFormula unsignedVal = bvmgr.toIntegerFormula(b.formula, false);

        // For non-negative: just convert unsigned value to string
        StringFormula nonNegStr = sfm.toStringFormula(unsignedVal);

        // For negative: absolute value = 2^8 - unsigned_value, then prepend "-"
        NumeralFormula.IntegerFormula twoTo8 = imgr.makeNumber(256L);
        NumeralFormula.IntegerFormula absVal = imgr.subtract(twoTo8, unsignedVal);
        StringFormula negStr = sfm.concat(sfm.makeString("-"), sfm.toStringFormula(absVal));

        // Combine with if-then-else
        StringFormula resultFormula = bmgr.ifThenElse(isNegative, negStr, nonNegStr);

        return new StringValue(ctx, Byte.toString(b.concrete), resultFormula, ObjectValue.ADDRESS_UNKNOWN);
    }
}
