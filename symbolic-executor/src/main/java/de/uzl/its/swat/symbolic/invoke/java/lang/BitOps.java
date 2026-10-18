package de.uzl.its.swat.symbolic.invoke.java.lang;

import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
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

    /** The number of one-bits of {@code x} (SWAR popcount, as in Integer/Long.bitCount), width of x. */
    static BitvectorFormula bitCount(SolverContext ctx, BitvectorFormula x, int width) {
        BitvectorFormulaManager bv = ctx.getFormulaManager().getBitvectorFormulaManager();
        BitvectorFormula i = x;
        i = bv.subtract(i, bv.and(bv.shiftRight(i, c(bv, width, 1), false), c(bv, width, 0x5555555555555555L)));
        i = bv.add(bv.and(i, c(bv, width, 0x3333333333333333L)),
                bv.and(bv.shiftRight(i, c(bv, width, 2), false), c(bv, width, 0x3333333333333333L)));
        i = bv.and(bv.add(i, bv.shiftRight(i, c(bv, width, 4), false)), c(bv, width, 0x0f0f0f0f0f0f0f0fL));
        for (int s = 8; s < width; s <<= 1) {
            i = bv.add(i, bv.shiftRight(i, c(bv, width, s), false));
        }
        return bv.and(i, c(bv, width, 0x7f));
    }

    /** {@code x} with every bit below its highest one-bit set (logical shifts). */
    private static BitvectorFormula smearRight(BitvectorFormulaManager bv, BitvectorFormula x, int width) {
        BitvectorFormula v = x;
        for (int s = 1; s < width; s <<= 1) {
            v = bv.or(v, bv.shiftRight(v, c(bv, width, s), false));
        }
        return v;
    }

    /** numberOfLeadingZeros = width - bitCount(smear(x)); width of x. */
    static BitvectorFormula numberOfLeadingZeros(SolverContext ctx, BitvectorFormula x, int width) {
        BitvectorFormulaManager bv = ctx.getFormulaManager().getBitvectorFormulaManager();
        return bv.subtract(c(bv, width, width), bitCount(ctx, smearRight(bv, x, width), width));
    }

    /** numberOfTrailingZeros = bitCount(~x & (x - 1)); width of x. */
    static BitvectorFormula numberOfTrailingZeros(SolverContext ctx, BitvectorFormula x, int width) {
        BitvectorFormulaManager bv = ctx.getFormulaManager().getBitvectorFormulaManager();
        BitvectorFormula belowLowest = bv.and(bv.not(x), bv.subtract(x, c(bv, width, 1)));
        return bitCount(ctx, belowLowest, width);
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
     * Integer/Long.toBinaryString (shift 1), toOctalString (shift 3) and toHexString (shift 4).
     */
    static StringFormula toUnsignedPowerOfTwoString(SolverContext ctx, BitvectorFormula x, int width, int shift) {
        BitvectorFormulaManager bv = ctx.getFormulaManager().getBitvectorFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        StringFormulaManager smgr = ctx.getFormulaManager().getStringFormulaManager();
        int digits = (width + shift - 1) / shift;
        // suffix = the digits k..0; result = the suffix starting at the highest non-zero digit
        StringFormula suffix = null;
        StringFormula result = null;
        for (int k = 0; k < digits; k++) {
            int lo = k * shift;
            int hi = Math.min(lo + shift, width) - 1;
            BitvectorFormula digit = bv.extract(x, hi, lo);
            int digitWidth = hi - lo + 1;
            StringFormula ch = smgr.makeString("0");
            for (int d = 1; d < (1 << digitWidth); d++) {
                ch = bmgr.ifThenElse(bv.equal(digit, c(bv, digitWidth, d)),
                        smgr.makeString(Character.toString(Character.forDigit(d, 1 << shift))), ch);
            }
            suffix = suffix == null ? ch : smgr.concat(ch, suffix);
            result = result == null
                    ? suffix
                    : bmgr.ifThenElse(bv.equal(digit, c(bv, digitWidth, 0)), result, suffix);
        }
        return result;
    }
}
