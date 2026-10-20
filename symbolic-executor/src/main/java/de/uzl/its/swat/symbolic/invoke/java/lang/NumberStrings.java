package de.uzl.its.swat.symbolic.invoke.java.lang;

import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.thread.ThreadHandler;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.StringFormula;
import org.sosy_lab.java_smt.api.StringFormulaManager;

/**
 * Exact encodings of the integral number-to-string conversions: Integer/Long/Short/Byte toString,
 * toUnsignedString and their radix variants, and the string conversion of numbers in string
 * concatenation. Every method takes a bit-vector of the given width and returns the string the JDK
 * produces for it, digit for digit.
 *
 * <p>A digit string that only has decimal digits (radix 10 and below) is the str.from_int of the
 * integer whose decimal digits it has. Z3 decides str.from_int directly, whereas a concatenation of
 * one string per digit, cut to a symbolic length, makes even an equality with a constant time out
 * or yield a model that violates the constraint. Only radixes above 10 need per-digit characters.
 */
public final class NumberStrings {

    private NumberStrings() {}

    /**
     * The decimal string of {@code x}, read as a signed or an unsigned number of {@code width} bits.
     * str.from_int is undefined ("") for negative numbers, so a negative value is "-" followed by
     * the digits of its magnitude 2^width - x.
     */
    public static StringFormula decimal(SolverContext ctx, BitvectorFormula x, int width, boolean signed) {
        FormulaManager fmgr = ctx.getFormulaManager();
        BitvectorFormulaManager bv = fmgr.getBitvectorFormulaManager();
        StringFormulaManager smgr = fmgr.getStringFormulaManager();
        IntegerFormulaManager imgr = fmgr.getIntegerFormulaManager();
        IntegerFormula unsigned = bv.toIntegerFormula(x, false);
        if (!signed) {
            return smgr.toStringFormula(unsigned);
        }
        IntegerFormula magnitude = imgr.subtract(imgr.makeNumber(BigInteger.ONE.shiftLeft(width)), unsigned);
        return fmgr.getBooleanFormulaManager().ifThenElse(
                bv.lessThan(x, bv.makeBitvector(width, 0), true),
                smgr.concat(smgr.makeString("-"), smgr.toStringFormula(magnitude)),
                smgr.toStringFormula(unsigned));
    }

    /**
     * The string of {@code x} in {@code radix} (2 to 36, see {@link #normaliseRadix}), as
     * Integer/Long.toString(x, radix) for a signed and toUnsignedString(x, radix) for an unsigned
     * {@code x}: the digits of the magnitude without leading zeros ("0" for zero), digits above 9 as
     * the letters a to z, and "-" in front of a negative value.
     */
    public static StringFormula toString(SolverContext ctx, BitvectorFormula x, int width, boolean signed, int radix) {
        assert radix >= Character.MIN_RADIX && radix <= Character.MAX_RADIX : "[SWAT] Radix out of range: " + radix;
        if (radix == 10) {
            return decimal(ctx, x, width, signed);
        }
        if (!signed) {
            return unsignedDigits(ctx, x, width, radix);
        }
        FormulaManager fmgr = ctx.getFormulaManager();
        BitvectorFormulaManager bv = fmgr.getBitvectorFormulaManager();
        BooleanFormulaManager bmgr = fmgr.getBooleanFormulaManager();
        StringFormulaManager smgr = fmgr.getStringFormulaManager();
        BooleanFormula negative = bv.lessThan(x, bv.makeBitvector(width, 0), true);
        // as an unsigned number, -MIN_VALUE is the magnitude of MIN_VALUE
        BitvectorFormula magnitude = bmgr.ifThenElse(negative, bv.negate(x), x);
        StringFormula digits = unsignedDigits(ctx, magnitude, width, radix);
        return bmgr.ifThenElse(negative, smgr.concat(smgr.makeString("-"), digits), digits);
    }

    /** The radix the JDK uses for {@code radix}: radixes outside 2 to 36 mean 10. */
    public static int normaliseRadix(int radix) {
        return radix < Character.MIN_RADIX || radix > Character.MAX_RADIX ? 10 : radix;
    }

    /**
     * The radix a call with the radix argument {@code radix} converts to, after {@link
     * #normaliseRadix}. A symbolic radix is pinned to its concrete value by choice branches: a
     * binary search over 2 to 36 on the normalised radix, one branch {@code radix < mid} per step,
     * each taken as the concrete radix says. The conversion is then exact on this path with a
     * constant radix, and the explorer reaches the other radixes by flipping the branches. (A
     * formula that chooses between the radixes itself is exact too, but no solver query on it
     * finishes.)
     *
     * @param radix The radix argument
     * @return The normalised concrete radix
     */
    public static int selectRadix(IntValue radix) throws NoThreadContextException {
        int concrete = normaliseRadix(radix.concrete);
        SolverContext ctx = radix.context;
        FormulaManager fmgr = ctx.getFormulaManager();
        try {
            if (fmgr.extractVariables(fmgr.simplify(radix.formula)).isEmpty()) {
                return concrete;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        BitvectorFormulaManager bv = fmgr.getBitvectorFormulaManager();
        BooleanFormulaManager bmgr = fmgr.getBooleanFormulaManager();
        BitvectorFormula r = radix.formula;
        BitvectorFormula normalised = bmgr.ifThenElse(
                bmgr.or(bv.lessThan(r, bv.makeBitvector(32, Character.MIN_RADIX), true),
                        bv.greaterThan(r, bv.makeBitvector(32, Character.MAX_RADIX), true)),
                bv.makeBitvector(32, 10), r);
        long threadId = Thread.currentThread().getId();
        // the call site of the conversion: its frame is the active one while the model runs
        long iid = ThreadHandler.getSymbolicVisitor(threadId).getStack().getActiveFrame().getCallSite();
        int lo = Character.MIN_RADIX;
        int hi = Character.MAX_RADIX + 1;
        int step = 0;
        while (hi - lo > 1) {
            int mid = lo + (hi - lo) / 2;
            boolean below = concrete < mid;
            BooleanFormula constraint = bv.lessThan(normalised, bv.makeBitvector(32, mid), true);
            ThreadHandler.getSymbolicTraceHandler(threadId).addChoiceBranch(below, constraint, iid, "#radix" + step++);
            if (below) {
                hi = mid;
            } else {
                lo = mid;
            }
        }
        return concrete;
    }

    /**
     * The digits of the unsigned value of {@code m} in {@code radix} (2 to 36, not 10 for a value
     * that str.from_int handles directly), without leading zeros ("0" for zero).
     */
    static StringFormula unsignedDigits(SolverContext ctx, BitvectorFormula m, int width, int radix) {
        int shift = Integer.numberOfTrailingZeros(radix);
        boolean powerOfTwo = Integer.bitCount(radix) == 1;
        if (radix == 10) {
            return decimal(ctx, m, width, false);
        }
        if (radix < 10) {
            return decimalDigitString(ctx, digits(ctx, m, width, radix));
        }
        if (powerOfTwo) {
            return BitOps.toUnsignedPowerOfTwoString(ctx, m, width, shift);
        }
        return letterDigitString(ctx, m, width, radix, digits(ctx, m, width, radix));
    }

    /** The number of digits of the largest unsigned value of {@code width} bits in {@code radix}. */
    private static int digitCount(int width, int radix) {
        BigInteger max = BigInteger.ONE.shiftLeft(width).subtract(BigInteger.ONE);
        BigInteger r = BigInteger.valueOf(radix);
        int n = 1;
        for (BigInteger p = r; p.compareTo(max) <= 0; p = p.multiply(r)) {
            n++;
        }
        return n;
    }

    /**
     * The digits of the unsigned value of {@code m} in {@code radix}, least significant first, each
     * a bit-vector of width at most 6 (the number of digits {@link #digitCount}). Power-of-two
     * digits are bit fields; the others come from a chain of divisions by the constant radix, which
     * repeats no subterm (q_k only appears in q_k+1 and in digit k).
     */
    private static List<BitvectorFormula> digits(SolverContext ctx, BitvectorFormula m, int width, int radix) {
        BitvectorFormulaManager bv = ctx.getFormulaManager().getBitvectorFormulaManager();
        int n = digitCount(width, radix);
        List<BitvectorFormula> digits = new ArrayList<>(n);
        if (Integer.bitCount(radix) == 1) {
            int shift = Integer.numberOfTrailingZeros(radix);
            for (int k = 0; k < n; k++) {
                int lo = k * shift;
                digits.add(bv.extract(m, Math.min(lo + shift, width) - 1, lo));
            }
            return digits;
        }
        BitvectorFormula r = bv.makeBitvector(width, radix);
        BitvectorFormula q = m;
        for (int k = 0; k < n; k++) {
            digits.add(bv.extract(bv.remainder(q, r, false), 5, 0));
            if (k < n - 1) {
                q = bv.divide(q, r, false);
            }
        }
        return digits;
    }

    /**
     * The string of decimal digits {@code digits} (least significant first, each below 10) without
     * leading zeros: str.from_int of the sum of digit_k * 10^k.
     */
    private static StringFormula decimalDigitString(SolverContext ctx, List<BitvectorFormula> digits) {
        FormulaManager fmgr = ctx.getFormulaManager();
        BitvectorFormulaManager bv = fmgr.getBitvectorFormulaManager();
        IntegerFormulaManager imgr = fmgr.getIntegerFormulaManager();
        List<IntegerFormula> terms = new ArrayList<>(digits.size());
        BigInteger power = BigInteger.ONE;
        for (BitvectorFormula digit : digits) {
            terms.add(imgr.multiply(bv.toIntegerFormula(digit, false), imgr.makeNumber(power)));
            power = power.multiply(BigInteger.TEN);
        }
        return fmgr.getStringFormulaManager().toStringFormula(imgr.sum(terms));
    }

    /**
     * The string of the radix-{@code radix} digits {@code digits} of {@code m} (least significant
     * first), digits above 9 as the letters a to z: all digits as characters, from the most
     * significant non-zero one on.
     */
    private static StringFormula letterDigitString(SolverContext ctx, BitvectorFormula m, int width, int radix,
                                                   List<BitvectorFormula> digits) {
        FormulaManager fmgr = ctx.getFormulaManager();
        BitvectorFormulaManager bv = fmgr.getBitvectorFormulaManager();
        BooleanFormulaManager bmgr = fmgr.getBooleanFormulaManager();
        IntegerFormulaManager imgr = fmgr.getIntegerFormulaManager();
        StringFormulaManager smgr = fmgr.getStringFormulaManager();
        int n = digits.size();
        List<StringFormula> chars = new ArrayList<>(n);
        // index (from the left) of the first character of the result
        IntegerFormula start = imgr.makeNumber(n - 1);
        BigInteger power = BigInteger.ONE;
        for (int k = 0; k < n; k++) {
            // the character code in 8 bits: digit + '0' or digit - 10 + 'a'
            BitvectorFormula digit = bv.extend(digits.get(k), 8 - bv.getLength(digits.get(k)), false);
            BitvectorFormula code = bv.add(digit, bmgr.ifThenElse(
                    bv.greaterThan(digit, bv.makeBitvector(8, 9), false),
                    bv.makeBitvector(8, 'a' - 10), bv.makeBitvector(8, '0')));
            chars.add(0, smgr.fromCodePoint(bv.toIntegerFormula(code, false)));
            if (k > 0) {
                // m >= radix^k: digit k is not a leading zero
                start = bmgr.ifThenElse(
                        bv.greaterOrEquals(m, bv.makeBitvector(width, power), false),
                        imgr.makeNumber(n - 1 - k), start);
            }
            power = power.multiply(BigInteger.valueOf(radix));
        }
        return smgr.substring(smgr.concat(chars), start, imgr.subtract(imgr.makeNumber(n), start));
    }
}
