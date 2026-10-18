package de.uzl.its.swat.symbolic.invoke.java.lang;

import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.StringFormula;
import org.sosy_lab.java_smt.api.StringFormulaManager;

/**
 * Exact bit-vector encodings of the bit-manipulation and radix-conversion methods shared by
 * {@link Integer} and {@link Long}. Every method takes a bit-vector of width 32 or 64 and mirrors
 * the JDK implementation (or its specification) bit for bit.
 */
final class BitOps {

    private BitOps() {}

    private static BitvectorFormula c(BitvectorFormulaManager bv, int width, long value) {
        // the 64-bit masks are truncated to the width of the operand
        return bv.makeBitvector(width, width < 64 ? value & ((1L << width) - 1) : value);
    }

    /*
     * The encodings below avoid terms that are shared by several parents (as in the shift cascades
     * of the JDK implementations): the branch constraints are simplified and then traversed as trees,
     * and if-then-else lifting turns shared subterms into an exponential blow-up.
     */

    /** The number of one-bits of {@code x} as the sum of its bits; width of x. */
    static BitvectorFormula bitCount(SolverContext ctx, BitvectorFormula x, int width) {
        BitvectorFormulaManager bv = ctx.getFormulaManager().getBitvectorFormulaManager();
        BitvectorFormula sum = bv.extend(bv.extract(x, 0, 0), width - 1, false);
        for (int k = 1; k < width; k++) {
            sum = bv.add(sum, bv.extend(bv.extract(x, k, k), width - 1, false));
        }
        return sum;
    }

    private static BooleanFormulaManager bools(SolverContext ctx) {
        return ctx.getFormulaManager().getBooleanFormulaManager();
    }

    private static BooleanFormula bitSet(BitvectorFormulaManager bv, BitvectorFormula x, int k) {
        return bv.equal(bv.extract(x, k, k), bv.makeBitvector(1, 1));
    }

    /** The number of zero bits above the highest one-bit (width for zero); width of x. */
    static BitvectorFormula numberOfLeadingZeros(SolverContext ctx, BitvectorFormula x, int width) {
        BitvectorFormulaManager bv = ctx.getFormulaManager().getBitvectorFormulaManager();
        BitvectorFormula result = c(bv, width, width);
        for (int k = 0; k < width; k++) {
            result = bools(ctx).ifThenElse(bitSet(bv, x, k), c(bv, width, width - 1 - k), result);
        }
        return result;
    }

    /** The number of zero bits below the lowest one-bit (width for zero); width of x. */
    static BitvectorFormula numberOfTrailingZeros(SolverContext ctx, BitvectorFormula x, int width) {
        BitvectorFormulaManager bv = ctx.getFormulaManager().getBitvectorFormulaManager();
        BitvectorFormula result = c(bv, width, width);
        for (int k = width - 1; k >= 0; k--) {
            result = bools(ctx).ifThenElse(bitSet(bv, x, k), c(bv, width, k), result);
        }
        return result;
    }

    /** The bits of {@code x} in reverse order: bit 0 becomes the most significant bit. */
    static BitvectorFormula reverse(SolverContext ctx, BitvectorFormula x, int width) {
        BitvectorFormulaManager bv = ctx.getFormulaManager().getBitvectorFormulaManager();
        BitvectorFormula result = bv.extract(x, 0, 0);
        for (int k = 1; k < width; k++) {
            result = bv.concat(result, bv.extract(x, k, k));
        }
        return result;
    }

    /** -1, 0 or 1 as a 32-bit vector, comparing x and y as signed or unsigned values. */
    static BitvectorFormula compare(SolverContext ctx, BitvectorFormula x, BitvectorFormula y, boolean signed) {
        BitvectorFormulaManager bv = ctx.getFormulaManager().getBitvectorFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        return bmgr.ifThenElse(bv.lessThan(x, y, signed), c(bv, 32, -1),
                bmgr.ifThenElse(bv.equal(x, y), c(bv, 32, 0), c(bv, 32, 1)));
    }

    /**
     * The unsigned value of {@code x} in radix 2^shift without leading zeros ("0" for zero), as
     * Integer/Long.toBinaryString (shift 1), toOctalString (shift 3) and toHexString (shift 4): the
     * string of all digits (each one str.from_code of its value), from the highest non-zero digit on.
     * Radixes up to 8 only have decimal digits and use the str.from_int encoding of {@link
     * NumberStrings}, which the solver handles far better.
     */
    static StringFormula toUnsignedPowerOfTwoString(SolverContext ctx, BitvectorFormula x, int width, int shift) {
        if (shift <= 3) {
            return NumberStrings.unsignedDigits(ctx, x, width, 1 << shift);
        }
        BitvectorFormulaManager bv = ctx.getFormulaManager().getBitvectorFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        StringFormulaManager smgr = ctx.getFormulaManager().getStringFormulaManager();
        IntegerFormulaManager imgr = ctx.getFormulaManager().getIntegerFormulaManager();
        int digits = (width + shift - 1) / shift;
        List<StringFormula> chars = new ArrayList<>();
        // index (from the left) of the first character of the result
        IntegerFormula start = imgr.makeNumber(digits - 1);
        for (int k = 0; k < digits; k++) {
            int lo = k * shift;
            int hi = Math.min(lo + shift, width) - 1;
            IntegerFormula digit = bv.toIntegerFormula(bv.extract(x, hi, lo), false);
            IntegerFormula code = imgr.add(digit, bmgr.ifThenElse(imgr.greaterThan(digit, imgr.makeNumber(9)),
                    imgr.makeNumber('a' - 10), imgr.makeNumber('0')));
            chars.add(0, smgr.fromCodePoint(code));
            if (k > 0) {
                start = bmgr.ifThenElse(bv.equal(bv.extract(x, hi, lo), c(bv, hi - lo + 1, 0)),
                        start, imgr.makeNumber(digits - 1 - k));
            }
        }
        return smgr.substring(smgr.concat(chars), start, imgr.subtract(imgr.makeNumber(digits), start));
    }
}
