package de.uzl.its.swat.symbolic.invoke.java.lang;

import de.uzl.its.swat.symbolic.invoke.java.lang.StringCallBranches.Branch;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.ByteValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.LongValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.ShortValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormulaManager;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.RegexFormula;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.StringFormula;
import org.sosy_lab.java_smt.api.StringFormulaManager;

/**
 * Models of parsing a number from a String: {@code Integer.parseInt}, {@code Long.parseLong},
 * {@code Short.parseShort}, {@code Byte.parseByte} (with or without a radix), {@code
 * Float.parseFloat}, {@code Double.parseDouble}, and the {@code valueOf(String)} variants.
 *
 * <p>Whether such a call throws a NumberFormatException is a branch on the string (see {@link
 * #branches}), which the visitor records on both sides of the call, so the solver can ask for a
 * parseable string or for one that is not.
 *
 * <p>Integers are exact for strings of any length. A string is parseable iff it is an optional
 * sign followed by at least one digit ({@link Character#digit(char, int)}, which accepts every
 * Unicode decimal digit and the fullwidth Latin letters), and its value is in range. A value in
 * range has at most K significant digits (r^K exceeds the range), so all digits but the last K are
 * zeros, and the value is the sum over the last K digits.
 *
 * <p>Floats are exact where the decimal-to-binary rounding is a single IEEE operation: no
 * exponent, no surrounding whitespace, the digits as an integer M exactly representable (M &lt;
 * 2^24 for float, 2^53 for double) and at most 10 (22) digits after the point, so that the value
 * is the correctly rounded quotient M / 10^k of two exact operands. NaN and Infinity are exact as
 * well. For the other parseable strings (exponents, hexadecimal, many digits, whitespace) the value
 * is concrete and the call records precision loss; whether the string is parseable at all is exact
 * for every string.
 */
public final class NumberParsing {

    /** An integral type: its range, and the name of its parse method. */
    private enum Integral {
        BYTE(Byte.MIN_VALUE, Byte.MAX_VALUE),
        SHORT(Short.MIN_VALUE, Short.MAX_VALUE),
        INT(Integer.MIN_VALUE, Integer.MAX_VALUE),
        LONG(Long.MIN_VALUE, Long.MAX_VALUE);

        final long min;
        final long max;

        Integral(long min, long max) {
            this.min = min;
            this.max = max;
        }

        /** The number of significant digits a value in range has at most in this radix. */
        int maxDigits(int radix) {
            BigInteger limit = BigInteger.valueOf(min).negate();
            BigInteger p = BigInteger.ONE;
            int k = 0;
            while (p.compareTo(limit) <= 0) {
                p = p.multiply(BigInteger.valueOf(radix));
                k++;
            }
            return k;
        }

        /** Java's result of parsing {@code s}, or null if it throws a NumberFormatException. */
        Long parse(String s, int radix) {
            try {
                return switch (this) {
                    case BYTE -> (long) Byte.parseByte(s, radix);
                    case SHORT -> (long) Short.parseShort(s, radix);
                    case INT -> (long) Integer.parseInt(s, radix);
                    case LONG -> Long.parseLong(s, radix);
                };
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    private NumberParsing() {}

    // ---- the calls ----

    /** The integral type a static call parses to, or null if it is not a modelled parse call. */
    private static Integral integral(String owner, String name, String desc) {
        if (!desc.startsWith("(Ljava/lang/String;)") && !desc.startsWith("(Ljava/lang/String;I)")) return null;
        return switch (owner + "." + name) {
            case "java/lang/Integer.parseInt", "java/lang/Integer.valueOf" -> Integral.INT;
            case "java/lang/Long.parseLong", "java/lang/Long.valueOf" -> Integral.LONG;
            case "java/lang/Short.parseShort", "java/lang/Short.valueOf" -> Integral.SHORT;
            case "java/lang/Byte.parseByte", "java/lang/Byte.valueOf" -> Integral.BYTE;
            default -> null;
        };
    }

    /** Whether a static call parses a float (false) or a double (true); null if it is not one. */
    private static Boolean floating(String owner, String name, String desc) {
        if (!desc.startsWith("(Ljava/lang/String;)")) return null;
        return switch (owner + "." + name) {
            case "java/lang/Float.parseFloat", "java/lang/Float.valueOf" -> false;
            case "java/lang/Double.parseDouble", "java/lang/Double.valueOf" -> true;
            default -> null;
        };
    }

    /**
     * The branches of the static call {@code owner.name desc}, in order: empty if it is not a parse
     * call or its string is not symbolic. The last one holds iff the call does not throw a
     * NumberFormatException; a pin of a symbolic radix comes first.
     */
    public static List<Branch> branches(String owner, String name, String desc, Value<?, ?>[] args) {
        try {
            if (args.length == 0 || !(args[0] instanceof StringValue s) || !s.isSymbolic()) return List.of();
            Integral type = integral(owner, name, desc);
            if (type != null) {
                List<Branch> branches = new ArrayList<>();
                Integer radix = radix(s.context, args, branches);
                if (radix == null) return branches;
                branches.add(new Branch(intValid(s.context, s.formula, radix, type), type.parse(s.concrete, radix) != null));
                return branches;
            }
            Boolean dbl = floating(owner, name, desc);
            if (dbl != null) {
                return List.of(new Branch(floatValid(s.context, s.formula), parseFloating(s.concrete, dbl) != null));
            }
        } catch (Exception e) {
            // Arguments of unexpected types: the model returns a PlaceHolder for them as well.
        }
        return List.of();
    }

    /**
     * The branches a parse call that returned normally takes after those of {@link #branches}: for
     * floats, whether the value is modelled exactly. It may hold or not.
     */
    public static List<Branch> returnBranches(String owner, String name, String desc, Value<?, ?>[] args) {
        if (args.length == 0 || !(args[0] instanceof StringValue s) || !s.isSymbolic()) return List.of();
        Boolean dbl = floating(owner, name, desc);
        if (dbl == null || parseFloating(s.concrete, dbl) == null) return List.of();
        return List.of(new Branch(floatExact(s.context, s.formula, dbl), floatExactConcrete(s.concrete, dbl)));
    }

    /**
     * The radix of a parse call (10 without one), with its branches added: if it is symbolic, the
     * range check Java does first, and a pin of its concrete value. Null if the radix is out of
     * range, so the call throws whatever the string.
     */
    private static Integer radix(SolverContext ctx, Value<?, ?>[] args, List<Branch> branches) throws Exception {
        if (args.length < 2) return 10;
        IntValue r = args[1].asIntValue();
        boolean inRange = r.concrete >= Character.MIN_RADIX && r.concrete <= Character.MAX_RADIX;
        if (r.isSymbolic()) {
            IntegerFormulaManager imgr = ctx.getFormulaManager().getIntegerFormulaManager();
            BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
            IntegerFormula f = r.asIntegerFormula();
            branches.add(new Branch(bmgr.and(imgr.greaterOrEquals(f, imgr.makeNumber(Character.MIN_RADIX)),
                    imgr.lessOrEquals(f, imgr.makeNumber(Character.MAX_RADIX))), inRange));
            if (inRange) branches.add(new Branch(imgr.equal(f, imgr.makeNumber(r.concrete)), true));
        } else if (!inRange) {
            // Throws for every string: a branch without variables, which fails.
            branches.add(new Branch(ctx.getFormulaManager().getBooleanFormulaManager().makeFalse(), false));
        }
        return inRange ? r.concrete : null;
    }

    /** The radix a parse call that returned used. */
    private static int radix(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        return args.length < 2 ? 10 : args[1].asIntValue().concrete;
    }

    // ---- the models ----

    /** Integer.parseInt(String[, int]) that returned normally: exact, given the call's branches held. */
    public static IntValue parseInt(StringValue s, Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        int radix = radix(args);
        int v = Integer.parseInt(s.concrete, radix);
        if (!s.isSymbolic()) return new IntValue(s.context, v);
        return new IntValue(s.context, v, intValue(s.context, s.formula, radix, Integral.INT));
    }

    /** Long.parseLong(String[, int]) that returned normally. */
    public static LongValue parseLong(StringValue s, Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        int radix = radix(args);
        long v = Long.parseLong(s.concrete, radix);
        if (!s.isSymbolic()) return new LongValue(s.context, v);
        BitvectorFormulaManager bvmgr = s.context.getFormulaManager().getBitvectorFormulaManager();
        return new LongValue(s.context, v, bvmgr.makeBitvector(64, intValue(s.context, s.formula, radix, Integral.LONG)));
    }

    /** Short.parseShort(String[, int]) that returned normally. */
    public static ShortValue parseShort(StringValue s, Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        int radix = radix(args);
        short v = Short.parseShort(s.concrete, radix);
        if (!s.isSymbolic()) return new ShortValue(s.context, v);
        return new ShortValue(s.context, v, intValue(s.context, s.formula, radix, Integral.SHORT));
    }

    /** Byte.parseByte(String[, int]) that returned normally. */
    public static ByteValue parseByte(StringValue s, Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        int radix = radix(args);
        byte v = Byte.parseByte(s.concrete, radix);
        if (!s.isSymbolic()) return new ByteValue(s.context, v);
        return new ByteValue(s.context, v, intValue(s.context, s.formula, radix, Integral.BYTE));
    }

    /**
     * Float.parseFloat(String) that returned normally: exact if {@link #floatExact} holds, which
     * the call's branches record; otherwise concrete, with precision loss.
     */
    public static FloatValue parseFloat(StringValue s, SymbolicTraceHandler sth) {
        float v = Float.parseFloat(s.concrete);
        if (!s.isSymbolic()) return new FloatValue(s.context, v);
        if (!floatExactConcrete(s.concrete, false)) {
            sth.recordSymbolicPrecisionLoss();
            return new FloatValue(s.context, v);
        }
        return new FloatValue(s.context, v, floatValue(s.context, s.formula, false));
    }

    /** Double.parseDouble(String) that returned normally, as {@link #parseFloat}. */
    public static DoubleValue parseDouble(StringValue s, SymbolicTraceHandler sth) {
        double v = Double.parseDouble(s.concrete);
        if (!s.isSymbolic()) return new DoubleValue(s.context, v);
        if (!floatExactConcrete(s.concrete, true)) {
            sth.recordSymbolicPrecisionLoss();
            return new DoubleValue(s.context, v);
        }
        return new DoubleValue(s.context, v, floatValue(s.context, s.formula, true));
    }

    // ---- integers ----

    private static final Map<String, List<int[]>> CHAR_CLASSES = new ConcurrentHashMap<>();
    private static final Map<Integer, CodePointTables.Table> DIGIT_TABLES = new ConcurrentHashMap<>();

    /** The parts of a string parsed as an integer, as formulas. */
    private record IntParts(IntegerFormula signLength, BooleanFormula negative, IntegerFormula digitCount, IntegerFormula value) {}

    private static IntParts intParts(SolverContext ctx, StringFormula s, int radix, Integral type) {
        FormulaManager fmgr = ctx.getFormulaManager();
        StringFormulaManager smgr = fmgr.getStringFormulaManager();
        IntegerFormulaManager imgr = fmgr.getIntegerFormulaManager();
        BooleanFormulaManager bmgr = fmgr.getBooleanFormulaManager();
        IntegerFormula len = smgr.length(s);
        BooleanFormula negative = smgr.prefix(smgr.makeString("-"), s);
        BooleanFormula signed = bmgr.or(negative, smgr.prefix(smgr.makeString("+"), s));
        IntegerFormula signLength = bmgr.ifThenElse(signed, imgr.makeNumber(1), imgr.makeNumber(0));
        IntegerFormula digitCount = imgr.subtract(len, signLength);
        CodePointTables.Table digits = digitTable(radix);
        List<IntegerFormula> terms = new ArrayList<>();
        BigInteger weight = BigInteger.ONE;
        for (int j = 0; j < type.maxDigits(radix); j++) {
            IntegerFormula pos = imgr.subtract(len, imgr.makeNumber(j + 1));
            IntegerFormula digit = digitValue(imgr, bmgr, digits.segments(), 0, digits.segments().length,
                    smgr.toCodePoint(smgr.charAt(s, pos)));
            IntegerFormula term = bmgr.ifThenElse(imgr.greaterThan(digitCount, imgr.makeNumber(j)), digit, imgr.makeNumber(0));
            terms.add(weight.equals(BigInteger.ONE) ? term : imgr.multiply(term, imgr.makeNumber(weight)));
            weight = weight.multiply(BigInteger.valueOf(radix));
        }
        return new IntParts(signLength, negative, digitCount, imgr.sum(terms));
    }

    /**
     * Whether {@code s} parses as an integer of {@code type} in {@code radix} (in [2, 36]): an
     * optional sign, then at least one digit, of which all but the last K are zeros, and the value
     * in range.
     */
    private static BooleanFormula intValid(SolverContext ctx, StringFormula s, int radix, Integral type) {
        FormulaManager fmgr = ctx.getFormulaManager();
        StringFormulaManager smgr = fmgr.getStringFormulaManager();
        IntegerFormulaManager imgr = fmgr.getIntegerFormulaManager();
        BooleanFormulaManager bmgr = fmgr.getBooleanFormulaManager();
        IntParts p = intParts(ctx, s, radix, type);
        RegexFormula digits = smgr.concat(smgr.closure(digitRegex(smgr, radix, true)),
                upTo(smgr, digitRegex(smgr, radix, false), type.maxDigits(radix)));
        IntegerFormula bound = bmgr.ifThenElse(p.negative(),
                imgr.makeNumber(BigInteger.valueOf(type.min).negate()), imgr.makeNumber(type.max));
        return bmgr.and(
                imgr.greaterOrEquals(p.digitCount(), imgr.makeNumber(1)),
                smgr.in(smgr.substring(s, p.signLength(), p.digitCount()), digits),
                imgr.lessOrEquals(p.value(), bound));
    }

    /** The value of {@code s} parsed as an integer of {@code type}, exact if {@link #intValid} holds. */
    private static IntegerFormula intValue(SolverContext ctx, StringFormula s, int radix, Integral type) {
        IntParts p = intParts(ctx, s, radix, type);
        IntegerFormulaManager imgr = ctx.getFormulaManager().getIntegerFormulaManager();
        return ctx.getFormulaManager().getBooleanFormulaManager().ifThenElse(p.negative(), imgr.negate(p.value()), p.value());
    }

    /** For the tests: {@link #intValid} for the type named int, long, short or byte. */
    public static BooleanFormula intValid(SolverContext ctx, StringFormula s, int radix, String type) {
        return intValid(ctx, s, radix, Integral.valueOf(type.toUpperCase()));
    }

    /** For the tests: {@link #intValue} for the type named int, long, short or byte. */
    public static IntegerFormula intValue(SolverContext ctx, StringFormula s, int radix, String type) {
        return intValue(ctx, s, radix, Integral.valueOf(type.toUpperCase()));
    }

    /** Character.digit(char, radix), tabulated over all chars: constant or offset segments. */
    private static CodePointTables.Table digitTable(int radix) {
        return DIGIT_TABLES.computeIfAbsent(radix, r -> CodePointTables.table("digit/" + r + "/C",
                c -> Character.digit((char) c, r), Character.MAX_VALUE, 32, true, false));
    }

    /** The digit value of code point x (-1 for a non-digit) as a balanced if-then-else tree. */
    private static IntegerFormula digitValue(IntegerFormulaManager imgr, BooleanFormulaManager bmgr,
                                             CodePointTables.Segment[] segs, int from, int to, IntegerFormula x) {
        if (to - from == 1) {
            CodePointTables.Segment seg = segs[from];
            IntegerFormula value = imgr.makeNumber((int) seg.value());
            return seg.offset() ? imgr.add(x, value) : value;
        }
        int mid = (from + to) >>> 1;
        return bmgr.ifThenElse(imgr.lessThan(x, imgr.makeNumber(segs[mid].lo())),
                digitValue(imgr, bmgr, segs, from, mid, x),
                digitValue(imgr, bmgr, segs, mid, to, x));
    }

    /** The chars that are digits in the radix ({@code zero}: whose value is 0), as a regex. */
    private static RegexFormula digitRegex(StringFormulaManager smgr, int radix, boolean zero) {
        List<int[]> ranges = CHAR_CLASSES.computeIfAbsent(radix + "/" + zero, k -> charRanges(zero
                ? c -> Character.digit((char) c, radix) == 0
                : c -> Character.digit((char) c, radix) >= 0));
        RegexFormula result = null;
        for (int[] r : ranges) {
            RegexFormula range = smgr.range((char) r[0], (char) r[1]);
            result = result == null ? range : smgr.union(result, range);
        }
        return result == null ? smgr.none() : result;
    }

    /** The maximal ranges of chars satisfying {@code p}. */
    private static List<int[]> charRanges(IntPredicate p) {
        List<int[]> ranges = new ArrayList<>();
        int c = 0;
        while (c <= Character.MAX_VALUE) {
            if (!p.test(c)) {
                c++;
                continue;
            }
            int lo = c;
            while (c + 1 <= Character.MAX_VALUE && p.test(c + 1)) c++;
            ranges.add(new int[] {lo, c});
            c++;
        }
        return ranges;
    }

    /** r{0,n} */
    private static RegexFormula upTo(StringFormulaManager smgr, RegexFormula r, int n) {
        RegexFormula result = smgr.makeRegex("");
        for (int i = 0; i < n; i++) result = smgr.optional(smgr.concat(r, result));
        return result;
    }

    // ---- floats ----

    /** The decimal forms modelled exactly: sign, integer digits, fraction digits. */
    private static final Pattern EXACT_DECIMAL = Pattern.compile("[+-]?(?:([0-9]+)(?:\\.([0-9]*))?|\\.([0-9]+))[fFdD]?");
    private static final Pattern EXACT_SPECIAL = Pattern.compile("[+-]?(?:NaN|Infinity)");

    /** Java's result of parsing {@code s} as a float or double, or null if that throws. */
    private static Double parseFloating(String s, boolean dbl) {
        try {
            return dbl ? Double.parseDouble(s) : (double) Float.parseFloat(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** The largest number of fraction digits whose power of ten the type holds exactly. */
    private static int maxFractionDigits(boolean dbl) {
        return dbl ? 22 : 10;
    }

    /** 2^p, p the type's precision: the digits as an integer must be below it. */
    private static BigInteger mantissaBound(boolean dbl) {
        return BigInteger.ONE.shiftLeft(dbl ? 53 : 24);
    }

    /** Whether the value of the parseable string {@code s} is modelled exactly (concretely). */
    public static boolean floatExactConcrete(String s, boolean dbl) {
        if (EXACT_SPECIAL.matcher(s).matches()) return true;
        Matcher m = EXACT_DECIMAL.matcher(s);
        if (!m.matches()) return false;
        String integer = m.group(1) == null ? "" : m.group(1);
        String fraction = m.group(3) != null ? m.group(3) : m.group(2) == null ? "" : m.group(2);
        return fraction.length() <= maxFractionDigits(dbl)
                && new BigInteger(integer + fraction).compareTo(mantissaBound(dbl)) < 0;
    }

    /**
     * Whether {@code s} parses as a float or double, i.e. matches the grammar of {@link
     * Double#valueOf(String)}, surrounding chars up to U+0020 included.
     */
    public static BooleanFormula floatValid(SolverContext ctx, StringFormula s) {
        StringFormulaManager smgr = ctx.getFormulaManager().getStringFormulaManager();
        RegexFormula ws = smgr.closure(smgr.range('\u0000', ' '));
        RegexFormula digits = smgr.cross(smgr.range('0', '9'));
        RegexFormula hexDigits = smgr.cross(smgr.union(smgr.range('0', '9'), smgr.union(smgr.range('a', 'f'), smgr.range('A', 'F'))));
        RegexFormula sign = smgr.optional(smgr.union(smgr.makeRegex("+"), smgr.makeRegex("-")));
        RegexFormula point = smgr.makeRegex(".");
        RegexFormula exp = smgr.concat(smgr.union(smgr.makeRegex("e"), smgr.makeRegex("E")), sign, digits);
        RegexFormula hexPrefix = smgr.concat(smgr.makeRegex("0"), smgr.union(smgr.makeRegex("x"), smgr.makeRegex("X")));
        RegexFormula decimal = smgr.union(
                smgr.concat(digits, smgr.optional(point), smgr.optional(digits), smgr.optional(exp)),
                smgr.concat(point, digits, smgr.optional(exp)));
        RegexFormula hex = smgr.concat(
                smgr.union(
                        smgr.concat(hexPrefix, hexDigits, smgr.optional(point)),
                        smgr.concat(hexPrefix, smgr.optional(hexDigits), point, hexDigits)),
                smgr.union(smgr.makeRegex("p"), smgr.makeRegex("P")), sign, digits);
        RegexFormula suffix = smgr.optional(smgr.union(smgr.union(smgr.makeRegex("f"), smgr.makeRegex("F")),
                smgr.union(smgr.makeRegex("d"), smgr.makeRegex("D"))));
        RegexFormula number = smgr.union(
                smgr.union(smgr.makeRegex("NaN"), smgr.makeRegex("Infinity")),
                smgr.concat(smgr.union(decimal, hex), suffix));
        return smgr.in(s, smgr.concat(ws, sign, number, ws));
    }

    /** The parts of a decimal string without exponent, as formulas. */
    private record DecimalParts(BooleanFormula form, BooleanFormula negative, IntegerFormula mantissa, IntegerFormula fractionDigits) {}

    private static DecimalParts decimalParts(SolverContext ctx, StringFormula s) {
        FormulaManager fmgr = ctx.getFormulaManager();
        StringFormulaManager smgr = fmgr.getStringFormulaManager();
        IntegerFormulaManager imgr = fmgr.getIntegerFormulaManager();
        BooleanFormulaManager bmgr = fmgr.getBooleanFormulaManager();
        RegexFormula digits = smgr.cross(smgr.range('0', '9'));
        RegexFormula point = smgr.makeRegex(".");
        RegexFormula suffixChar = smgr.union(smgr.union(smgr.makeRegex("f"), smgr.makeRegex("F")),
                smgr.union(smgr.makeRegex("d"), smgr.makeRegex("D")));
        RegexFormula form = smgr.concat(
                smgr.optional(smgr.union(smgr.makeRegex("+"), smgr.makeRegex("-"))),
                smgr.union(smgr.concat(digits, smgr.optional(smgr.concat(point, smgr.closure(smgr.range('0', '9'))))),
                        smgr.concat(point, digits)),
                smgr.optional(suffixChar));
        IntegerFormula zero = imgr.makeNumber(0);
        IntegerFormula one = imgr.makeNumber(1);
        IntegerFormula len = smgr.length(s);
        BooleanFormula negative = smgr.prefix(smgr.makeString("-"), s);
        IntegerFormula start = bmgr.ifThenElse(bmgr.or(negative, smgr.prefix(smgr.makeString("+"), s)), one, zero);
        IntegerFormula end = bmgr.ifThenElse(smgr.in(smgr.charAt(s, imgr.subtract(len, one)), suffixChar),
                imgr.subtract(len, one), len);
        IntegerFormula dot = smgr.indexOf(s, smgr.makeString("."), zero);
        BooleanFormula hasDot = imgr.greaterOrEquals(dot, zero);
        IntegerFormula integerEnd = bmgr.ifThenElse(hasDot, dot, end);
        IntegerFormula fractionStart = imgr.add(dot, one);
        IntegerFormula fractionDigits = bmgr.ifThenElse(hasDot, imgr.subtract(end, fractionStart), zero);
        StringFormula integer = smgr.substring(s, start, imgr.subtract(integerEnd, start));
        StringFormula fraction = smgr.substring(s, fractionStart, fractionDigits);
        StringFormula all = smgr.concat(integer, bmgr.ifThenElse(hasDot, fraction, smgr.makeString("")));
        return new DecimalParts(smgr.in(s, form), negative, smgr.toIntegerFormula(all), fractionDigits);
    }

    private static RegexFormula special(StringFormulaManager smgr, String name) {
        return smgr.concat(smgr.optional(smgr.union(smgr.makeRegex("+"), smgr.makeRegex("-"))), smgr.makeRegex(name));
    }

    /**
     * Whether the value of a parseable {@code s} is modelled exactly: NaN, Infinity, or a decimal
     * without exponent and whitespace whose digits form an integer below 2^p, with at most 10 (22)
     * of them after the point.
     */
    public static BooleanFormula floatExact(SolverContext ctx, StringFormula s, boolean dbl) {
        FormulaManager fmgr = ctx.getFormulaManager();
        StringFormulaManager smgr = fmgr.getStringFormulaManager();
        IntegerFormulaManager imgr = fmgr.getIntegerFormulaManager();
        BooleanFormulaManager bmgr = fmgr.getBooleanFormulaManager();
        DecimalParts p = decimalParts(ctx, s);
        return bmgr.or(
                smgr.in(s, special(smgr, "NaN")),
                smgr.in(s, special(smgr, "Infinity")),
                bmgr.and(p.form(),
                        imgr.lessThan(p.mantissa(), imgr.makeNumber(mantissaBound(dbl))),
                        imgr.lessOrEquals(p.fractionDigits(), imgr.makeNumber(maxFractionDigits(dbl)))));
    }

    /** The value of {@code s} parsed as a float or double: exact if {@link #floatExact} holds. */
    public static FloatingPointFormula floatValue(SolverContext ctx, StringFormula s, boolean dbl) {
        FormulaManager fmgr = ctx.getFormulaManager();
        StringFormulaManager smgr = fmgr.getStringFormulaManager();
        IntegerFormulaManager imgr = fmgr.getIntegerFormulaManager();
        BooleanFormulaManager bmgr = fmgr.getBooleanFormulaManager();
        BitvectorFormulaManager bvmgr = fmgr.getBitvectorFormulaManager();
        FloatingPointFormulaManager fpmgr = fmgr.getFloatingPointFormulaManager();
        FormulaType.FloatingPointType type = dbl
                ? FormulaType.getDoublePrecisionFloatingPointType()
                : FormulaType.getSinglePrecisionFloatingPointType();
        DecimalParts p = decimalParts(ctx, s);
        // M below 2^p converts exactly; 10^k for k up to the maximum is exact too, so the division
        // rounds once, as Java's conversion of the exact decimal value does.
        FloatingPointFormula mantissa = fpmgr.castFrom(bvmgr.makeBitvector(dbl ? 64 : 32, p.mantissa()), true, type);
        FloatingPointFormula divisor = fpmgr.makeNumber(1, type);
        for (int k = 1; k <= maxFractionDigits(dbl); k++) {
            divisor = bmgr.ifThenElse(imgr.equal(p.fractionDigits(), imgr.makeNumber(k)),
                    fpmgr.makeNumber(BigInteger.TEN.pow(k).toString(), type), divisor);
        }
        FloatingPointFormula magnitude = fpmgr.divide(mantissa, divisor);
        FloatingPointFormula decimal = bmgr.ifThenElse(p.negative(), fpmgr.negate(magnitude), magnitude);
        FloatingPointFormula infinity = bmgr.ifThenElse(p.negative(), fpmgr.makeMinusInfinity(type), fpmgr.makePlusInfinity(type));
        return bmgr.ifThenElse(smgr.in(s, special(smgr, "NaN")), fpmgr.makeNaN(type),
                bmgr.ifThenElse(smgr.in(s, special(smgr, "Infinity")), infinity, decimal));
    }
}
