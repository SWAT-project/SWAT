package de.uzl.its.swat.symbolic.invoke.java.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntUnaryOperator;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

/**
 * Exact symbolic models of functions over chars or code points, such as the Unicode predicates and
 * case mappings of {@link Character}, built by tabulating the function in the running JVM.
 *
 * <p>The domain (all chars, or all valid code points) is split into maximal segments on which the
 * function is either constant or a constant offset from its input (as with most case mappings).
 * The model is a balanced if-then-else tree over the segment bounds. Because the table comes from
 * the very JDK that runs the target, the model matches its Unicode version exactly.
 *
 * <p>For int inputs, every value outside [0, max] must behave the same (a constant or an offset),
 * which holds for the {@code Character} methods since they treat all invalid code points alike.
 * This is checked on sample values; a function failing the check gets no table.
 */
final class CodePointTables {

    /** Starting at {@code lo}, the function is {@code value}, or input + {@code value} if offset. */
    record Segment(int lo, long value, boolean offset) {}

    /** Segments covering [0, max], and the behaviour outside that range (null for char input). */
    record Table(Segment[] segments, int max, Segment outside) {}

    private static final Map<String, Table> CACHE = new ConcurrentHashMap<>();

    private static final int[] OUTSIDE_SAMPLES = {
        -1, -2, -0x10000, Integer.MIN_VALUE, Character.MAX_CODE_POINT + 1, 0x7FFF_0000, Integer.MAX_VALUE
    };

    private CodePointTables() {}

    /**
     * The table of {@code f} over [0, max], with results taken modulo 2^outWidth, cached under
     * {@code key}. Returns null if {@code checkOutside} and the values outside [0, max] disagree.
     */
    static Table table(String key, IntUnaryOperator f, int max, int outWidth, boolean allowOffset, boolean checkOutside) {
        Table cached = CACHE.get(key);
        if (cached != null) return cached;
        long mask = outWidth == 64 ? -1L : (1L << outWidth) - 1;
        List<Segment> segments = new ArrayList<>();
        int v = 0;
        while (v <= max) {
            int lo = v;
            long c = f.applyAsInt(lo) & mask;
            long d = (c - lo) & mask;
            int mode = 0; // 0: undecided, 1: constant, 2: offset
            v++;
            while (v <= max) {
                long fv = f.applyAsInt(v) & mask;
                if (mode != 2 && fv == c) mode = 1;
                else if (mode != 1 && allowOffset && ((fv - v) & mask) == d) mode = 2;
                else break;
                v++;
            }
            segments.add(mode == 2 ? new Segment(lo, d, true) : new Segment(lo, c, false));
        }
        Segment outside = null;
        if (checkOutside) {
            outside = outsideSegment(f, mask, allowOffset);
            if (outside == null) return null;
        }
        Table table = new Table(segments.toArray(new Segment[0]), max, outside);
        CACHE.put(key, table);
        return table;
    }

    private static Segment outsideSegment(IntUnaryOperator f, long mask, boolean allowOffset) {
        boolean constant = true;
        boolean offset = allowOffset;
        long c = f.applyAsInt(OUTSIDE_SAMPLES[0]) & mask;
        long d = (c - OUTSIDE_SAMPLES[0]) & mask;
        for (int s : OUTSIDE_SAMPLES) {
            long fs = f.applyAsInt(s) & mask;
            constant &= fs == c;
            offset &= ((fs - s) & mask) == d;
        }
        if (constant) return new Segment(0, c, false);
        if (offset) return new Segment(0, d, true);
        return null;
    }

    /** The table as a bitvector of width {@code outWidth}, applied to {@code x} of width {@code inWidth}. */
    static BitvectorFormula apply(SolverContext ctx, Table t, BitvectorFormula x, int inWidth, int outWidth) {
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        // The input as seen by an offset segment: zero-extended or truncated to the output width.
        BitvectorFormula conv = inWidth == outWidth ? x
                : inWidth < outWidth ? bvmgr.extend(x, outWidth - inWidth, false)
                : bvmgr.extract(x, outWidth - 1, 0);
        BitvectorFormula tree = tree(bvmgr, bmgr, t.segments(), 0, t.segments().length, x, inWidth, conv, outWidth);
        if (t.outside() == null) return tree;
        BooleanFormula isOutside = bvmgr.greaterThan(x, bvmgr.makeBitvector(inWidth, t.max()), false); // unsigned: negatives too
        return bmgr.ifThenElse(isOutside, leaf(bvmgr, t.outside(), conv, outWidth), tree);
    }

    /** The table of a predicate (values 0 and 1) as a boolean formula. */
    static BooleanFormula applyPredicate(SolverContext ctx, Table t, BitvectorFormula x, int inWidth) {
        BitvectorFormulaManager bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        return bvmgr.equal(apply(ctx, t, x, inWidth, 1), bvmgr.makeBitvector(1, 1));
    }

    private static BitvectorFormula tree(BitvectorFormulaManager bvmgr, BooleanFormulaManager bmgr, Segment[] segs,
                                         int from, int to, BitvectorFormula x, int inWidth, BitvectorFormula conv, int outWidth) {
        if (to - from == 1) return leaf(bvmgr, segs[from], conv, outWidth);
        int mid = (from + to) >>> 1;
        BooleanFormula below = bvmgr.lessThan(x, bvmgr.makeBitvector(inWidth, segs[mid].lo()), false);
        return bmgr.ifThenElse(below,
                tree(bvmgr, bmgr, segs, from, mid, x, inWidth, conv, outWidth),
                tree(bvmgr, bmgr, segs, mid, to, x, inWidth, conv, outWidth));
    }

    private static BitvectorFormula leaf(BitvectorFormulaManager bvmgr, Segment s, BitvectorFormula conv, int outWidth) {
        BitvectorFormula value = bvmgr.makeBitvector(outWidth, s.value());
        return s.offset() ? bvmgr.add(conv, value) : value;
    }
}
