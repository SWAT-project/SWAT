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
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.ByteValue;
import java.util.Map;
import java.util.function.IntUnaryOperator;
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
            case "digit" -> invokeDigit(args, desc);
            case "toString" -> args.length == 1 && desc[0].getSort() == Type.CHAR
                    ? args[0].asCharValue().asStringValue() : PlaceHolder.instance;
            default -> invokeTabulated(name, args, desc);
        };
    }

    /** What a tabulated method returns: its result type's bit width (1 for boolean). */
    private enum Result {
        BOOLEAN(1), BYTE(8), CHAR(16), INT(32);
        final int width;
        Result(int width) { this.width = width; }
    }

    /**
     * A Character method over one char or code point, with its char and int overloads (either may
     * be null) and what they return. Modelled exactly by tabulating it, see {@link CodePointTables}.
     */
    private record Tabulated(IntUnaryOperator onChar, Result charResult, IntUnaryOperator onInt, Result intResult) {}

    private static Tabulated predicate(IntUnaryOperator onChar, IntUnaryOperator onInt) {
        return new Tabulated(onChar, Result.BOOLEAN, onInt, Result.BOOLEAN);
    }

    private static int b(boolean value) { return value ? 1 : 0; }

    private static final Map<String, Tabulated> TABULATED = Map.ofEntries(
            Map.entry("isDigit", predicate(c -> b(Character.isDigit((char) c)), c -> b(Character.isDigit(c)))),
            Map.entry("isLetter", predicate(c -> b(Character.isLetter((char) c)), c -> b(Character.isLetter(c)))),
            Map.entry("isLetterOrDigit", predicate(c -> b(Character.isLetterOrDigit((char) c)), c -> b(Character.isLetterOrDigit(c)))),
            Map.entry("isAlphabetic", predicate(null, c -> b(Character.isAlphabetic(c)))),
            Map.entry("isUpperCase", predicate(c -> b(Character.isUpperCase((char) c)), c -> b(Character.isUpperCase(c)))),
            Map.entry("isLowerCase", predicate(c -> b(Character.isLowerCase((char) c)), c -> b(Character.isLowerCase(c)))),
            Map.entry("isTitleCase", predicate(c -> b(Character.isTitleCase((char) c)), c -> b(Character.isTitleCase(c)))),
            Map.entry("isWhitespace", predicate(c -> b(Character.isWhitespace((char) c)), c -> b(Character.isWhitespace(c)))),
            Map.entry("isSpaceChar", predicate(c -> b(Character.isSpaceChar((char) c)), c -> b(Character.isSpaceChar(c)))),
            Map.entry("isDefined", predicate(c -> b(Character.isDefined((char) c)), c -> b(Character.isDefined(c)))),
            Map.entry("isIdeographic", predicate(null, c -> b(Character.isIdeographic(c)))),
            Map.entry("isMirrored", predicate(c -> b(Character.isMirrored((char) c)), c -> b(Character.isMirrored(c)))),
            Map.entry("isISOControl", predicate(c -> b(Character.isISOControl((char) c)), c -> b(Character.isISOControl(c)))),
            Map.entry("isJavaIdentifierStart", predicate(c -> b(Character.isJavaIdentifierStart((char) c)), c -> b(Character.isJavaIdentifierStart(c)))),
            Map.entry("isJavaIdentifierPart", predicate(c -> b(Character.isJavaIdentifierPart((char) c)), c -> b(Character.isJavaIdentifierPart(c)))),
            Map.entry("isUnicodeIdentifierStart", predicate(c -> b(Character.isUnicodeIdentifierStart((char) c)), c -> b(Character.isUnicodeIdentifierStart(c)))),
            Map.entry("isUnicodeIdentifierPart", predicate(c -> b(Character.isUnicodeIdentifierPart((char) c)), c -> b(Character.isUnicodeIdentifierPart(c)))),
            Map.entry("isIdentifierIgnorable", predicate(c -> b(Character.isIdentifierIgnorable((char) c)), c -> b(Character.isIdentifierIgnorable(c)))),
            Map.entry("isHighSurrogate", predicate(c -> b(Character.isHighSurrogate((char) c)), null)),
            Map.entry("isLowSurrogate", predicate(c -> b(Character.isLowSurrogate((char) c)), null)),
            Map.entry("isSurrogate", predicate(c -> b(Character.isSurrogate((char) c)), null)),
            Map.entry("isJavaLetter", predicate(c -> b(Character.isJavaLetter((char) c)), null)),
            Map.entry("isJavaLetterOrDigit", predicate(c -> b(Character.isJavaLetterOrDigit((char) c)), null)),
            Map.entry("isSpace", predicate(c -> b(Character.isSpace((char) c)), null)),
            Map.entry("toLowerCase", new Tabulated(c -> Character.toLowerCase((char) c), Result.CHAR, Character::toLowerCase, Result.INT)),
            Map.entry("toUpperCase", new Tabulated(c -> Character.toUpperCase((char) c), Result.CHAR, Character::toUpperCase, Result.INT)),
            Map.entry("toTitleCase", new Tabulated(c -> Character.toTitleCase((char) c), Result.CHAR, Character::toTitleCase, Result.INT)),
            Map.entry("getType", new Tabulated(c -> Character.getType((char) c), Result.INT, Character::getType, Result.INT)),
            Map.entry("getNumericValue", new Tabulated(c -> Character.getNumericValue((char) c), Result.INT, Character::getNumericValue, Result.INT)),
            Map.entry("getDirectionality", new Tabulated(c -> Character.getDirectionality((char) c), Result.BYTE, Character::getDirectionality, Result.BYTE)));

    private static Value<?, ?> invokeTabulated(String name, Value<?, ?>[] args, Type[] desc) throws NotImplementedException, ValueConversionException {
        Tabulated t = TABULATED.get(name);
        if (t == null || args.length != 1 || desc.length != 1) return PlaceHolder.instance;
        boolean onChar = desc[0].getSort() == Type.CHAR;
        IntUnaryOperator f = onChar ? t.onChar() : t.onInt();
        if (f == null) return PlaceHolder.instance;
        return tabulated(name + (onChar ? "/C" : "/I"), f, onChar ? t.charResult() : t.intResult(), args[0], onChar);
    }

    /**
     * Invocation handler for Character.digit(char, int) and Character.digit(int, int). Modelled for
     * a concrete radix only, which is the usual case; a symbolic radix is left unmodelled.
     */
    private static Value<?, ?> invokeDigit(Value<?, ?>[] args, Type[] desc) throws NotImplementedException, ValueConversionException {
        if (args.length != 2 || args[1].isSymbolic()) return PlaceHolder.instance;
        int radix = args[1].asIntValue().concrete;
        boolean onChar = desc[0].getSort() == Type.CHAR;
        IntUnaryOperator f = onChar ? c -> Character.digit((char) c, radix) : c -> Character.digit(c, radix);
        return tabulated("digit/" + radix + (onChar ? "/C" : "/I"), f, Result.INT, args[0], onChar);
    }

    /** Applies the tabulated function {@code f}, cached under {@code key}, to the char or code point {@code arg}. */
    private static Value<?, ?> tabulated(String key, IntUnaryOperator f, Result result, Value<?, ?> arg, boolean onChar) throws NotImplementedException, ValueConversionException {
        SolverContext ctx;
        BitvectorFormula x;
        int in;
        if (onChar) {
            CharValue c = arg.asCharValue();
            ctx = c.context;
            x = c.formula;
            in = c.concrete;
        } else {
            IntValue i = arg.asIntValue();
            ctx = i.context;
            x = i.formula;
            in = i.concrete;
        }
        int out = f.applyAsInt(in);
        if (!arg.isSymbolic()) {
            return switch (result) {
                case BOOLEAN -> new BooleanValue(ctx, out != 0);
                case BYTE -> new ByteValue(ctx, (byte) out);
                case CHAR -> new CharValue(ctx, (char) out);
                case INT -> new IntValue(ctx, out);
            };
        }
        int inWidth = onChar ? 16 : 32;
        CodePointTables.Table table = CodePointTables.table(key, f, onChar ? Character.MAX_VALUE : Character.MAX_CODE_POINT,
                result.width, result != Result.BOOLEAN, !onChar);
        if (table == null) return PlaceHolder.instance;
        if (result == Result.BOOLEAN) {
            return new BooleanValue(ctx, out != 0, CodePointTables.applyPredicate(ctx, table, x, inWidth));
        }
        BitvectorFormula formula = CodePointTables.apply(ctx, table, x, inWidth, result.width);
        return switch (result) {
            case BYTE -> new ByteValue(ctx, (byte) out, formula);
            case CHAR -> new CharValue(ctx, (char) out, formula);
            default -> new IntValue(ctx, out, formula);
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
