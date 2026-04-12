package de.uzl.its.swat.symbolic.invoke.java.lang;

import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.BooleanValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.CharValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.CharacterObjectValue;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

public class CharacterInvocation {

    public static Value<?, ?> invokeStaticMethod(
            String name,
            Value<?, ?>[] args,
            Type[] desc,
            SymbolicTraceHandler symbolicStateHandler) throws NotImplementedException, ValueConversionException {
        return switch (name) {
            case "charCount" -> invokeCharCount(args);
            case "compare" -> invokeCompare(args);
            case "isBmpCodePoint" -> invokeIsBmpCodePoint(args);
            case "isSupplementaryCodePoint" -> invokeIsSupplementaryCodePoint(args);
            case "isValidCodePoint" -> invokeIsValidCodePoint(args);
            case "toCodePoint" -> invokeToCodePoint(args);
            case "valueOf" -> invokeValueOf(args, desc);
            default -> PlaceHolder.instance;
        };
    }

    /**
     * Invocation handler for Character.compare(char, char).
     */
    private static Value<?, ?> invokeCompare(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        assert args.length == 2 : "Expected 2 arguments for compare(), got " + args.length;
        return compare(args[0].asCharValue(), args[1].asCharValue());
    }

    /**
     * Symbolic wrapper for Character.compare(char x, char y)
     * Compares two char values numerically.
     * Returns the value x - y (as unsigned comparison).
     *
     * @param x The first char
     * @param y The second char
     * @return The value x - y
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Character.html#compare(char,char)">Character.compare(char, char)</a>
     */
    private static IntValue compare(CharValue x, CharValue y) {
        SolverContext ctx = x.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        // Character.compare returns x - y (treating chars as unsigned 16-bit values)
        // Need to sign-extend to 32-bit for the result
        return new IntValue(ctx, Character.compare(x.concrete, y.concrete),
                bvmgr.subtract(
                    bvmgr.extend(x.formula, 16, false),  // zero-extend char (16-bit) to int (32-bit)
                    bvmgr.extend(y.formula, 16, false)));
    }

    private static Value<?, ?> invokeValueOf(Value<?, ?>[] args, Type[] desc) throws NotImplementedException {
        if (args.length == 1) {
            CharValue c = args[0].asCharValue();
            return new CharacterObjectValue(c.context, c, ObjectValue.ADDRESS_UNKNOWN);
        } else {
            return PlaceHolder.instance;
        }
    }

    /**
     * Invocation handler for Character.charCount(int codePoint).
     * Returns 2 if the code point is a supplementary character (>= 0x10000), 1 otherwise.
     */
    private static Value<?, ?> invokeCharCount(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        assert args.length == 1 : "Expected 1 argument for charCount(), got " + args.length;
        IntValue codePoint = args[0].asIntValue();
        SolverContext ctx = codePoint.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();

        // charCount returns 2 if codePoint >= 0x10000, else 1
        BitvectorFormula minSupplementary = bvmgr.makeBitvector(32, 0x10000);
        BooleanFormula isSupplementary = bvmgr.greaterOrEquals(codePoint.formula, minSupplementary, true);
        BitvectorFormula result = bmgr.ifThenElse(isSupplementary,
                bvmgr.makeBitvector(32, 2),
                bvmgr.makeBitvector(32, 1));

        return new IntValue(ctx, Character.charCount(codePoint.concrete), result);
    }

    /**
     * Invocation handler for Character.isValidCodePoint(int codePoint).
     * Returns true if codePoint is in [0, 0x10FFFF].
     */
    private static Value<?, ?> invokeIsValidCodePoint(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        assert args.length == 1 : "Expected 1 argument for isValidCodePoint(), got " + args.length;
        IntValue codePoint = args[0].asIntValue();
        SolverContext ctx = codePoint.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();

        // Valid: 0 <= codePoint <= 0x10FFFF
        BitvectorFormula zero = bvmgr.makeBitvector(32, 0);
        BitvectorFormula maxCodePoint = bvmgr.makeBitvector(32, Character.MAX_CODE_POINT);
        BooleanFormula geZero = bvmgr.greaterOrEquals(codePoint.formula, zero, true);
        BooleanFormula leMax = bvmgr.lessOrEquals(codePoint.formula, maxCodePoint, true);
        BooleanFormula result = bmgr.and(geZero, leMax);

        return new BooleanValue(ctx, Character.isValidCodePoint(codePoint.concrete), result);
    }

    /**
     * Invocation handler for Character.isBmpCodePoint(int codePoint).
     * Returns true if codePoint is in the BMP [0, 0xFFFF].
     */
    private static Value<?, ?> invokeIsBmpCodePoint(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        assert args.length == 1 : "Expected 1 argument for isBmpCodePoint(), got " + args.length;
        IntValue codePoint = args[0].asIntValue();
        SolverContext ctx = codePoint.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();

        // BMP: 0 <= codePoint <= 0xFFFF (unsigned comparison since negative ints are not valid)
        // Use unsigned comparison: codePoint <= 0xFFFF (as unsigned)
        BitvectorFormula maxBmp = bvmgr.makeBitvector(32, 0xFFFF);
        BooleanFormula result = bvmgr.lessOrEquals(codePoint.formula, maxBmp, false);  // unsigned

        return new BooleanValue(ctx, Character.isBmpCodePoint(codePoint.concrete), result);
    }

    /**
     * Invocation handler for Character.isSupplementaryCodePoint(int codePoint).
     * Returns true if codePoint is in the supplementary range [0x10000, 0x10FFFF].
     */
    private static Value<?, ?> invokeIsSupplementaryCodePoint(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        assert args.length == 1 : "Expected 1 argument for isSupplementaryCodePoint(), got " + args.length;
        IntValue codePoint = args[0].asIntValue();
        SolverContext ctx = codePoint.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();

        // Supplementary: 0x10000 <= codePoint <= 0x10FFFF
        BitvectorFormula minSupp = bvmgr.makeBitvector(32, Character.MIN_SUPPLEMENTARY_CODE_POINT);
        BitvectorFormula maxCodePoint = bvmgr.makeBitvector(32, Character.MAX_CODE_POINT);
        BooleanFormula geMin = bvmgr.greaterOrEquals(codePoint.formula, minSupp, true);
        BooleanFormula leMax = bvmgr.lessOrEquals(codePoint.formula, maxCodePoint, true);
        BooleanFormula result = bmgr.and(geMin, leMax);

        return new BooleanValue(ctx, Character.isSupplementaryCodePoint(codePoint.concrete), result);
    }

    /**
     * Invocation handler for Character.toCodePoint(char high, char low).
     * Converts a surrogate pair to its supplementary code point.
     * Formula: ((high - 0xD800) << 10) + (low - 0xDC00) + 0x10000
     */
    private static Value<?, ?> invokeToCodePoint(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        assert args.length == 2 : "Expected 2 arguments for toCodePoint(), got " + args.length;
        CharValue high = args[0].asCharValue();
        CharValue low = args[1].asCharValue();
        SolverContext ctx = high.context;
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();

        // Extend chars to 32 bits
        BitvectorFormula high32 = bvmgr.extend(high.formula, 16, false);
        BitvectorFormula low32 = bvmgr.extend(low.formula, 16, false);

        // ((high - 0xD800) << 10) + (low - 0xDC00) + 0x10000
        BitvectorFormula d800 = bvmgr.makeBitvector(32, 0xD800);
        BitvectorFormula dc00 = bvmgr.makeBitvector(32, 0xDC00);
        BitvectorFormula x10000 = bvmgr.makeBitvector(32, 0x10000);

        BitvectorFormula highPart = bvmgr.shiftLeft(bvmgr.subtract(high32, d800), bvmgr.makeBitvector(32, 10));
        BitvectorFormula lowPart = bvmgr.subtract(low32, dc00);
        BitvectorFormula result = bvmgr.add(bvmgr.add(highPart, lowPart), x10000);

        return new IntValue(ctx, Character.toCodePoint(high.concrete, low.concrete), result);
    }
}
