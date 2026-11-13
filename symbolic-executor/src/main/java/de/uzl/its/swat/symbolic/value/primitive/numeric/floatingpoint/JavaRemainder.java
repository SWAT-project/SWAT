package de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint;

import de.uzl.its.swat.symbolic.invoke.java.lang.SampledMath;
import de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

/**
 * Java's floating-point remainder ({@code x % y}, DREM/FREM) as an approximate model.
 *
 * <p>It is exact where the quotient is 0 or 1 in magnitude, which is the common case (an angle
 * modulo 360, a phase modulo a period):
 *
 * <ul>
 *   <li>|x| &lt; |y|: the remainder is x (also for a zero x and for an infinite y);
 *   <li>|y| &lt;= |x| &lt; 2|y|: it is |x| - |y| with the sign of x, which is exact (Sterbenz);
 * </ul>
 *
 * and the concrete result of this run anywhere else (NaN operands, a zero divisor, an infinite
 * dividend included). So it is a model like those of the transcendental Math functions: a fresh
 * function, declared approximate (SampledMath#define), so that a branch over it is a precision
 * loss and an exhausted search proves nothing.
 *
 * <p>The exact SMT encoding, {@code fp.rem} (the IEEE remainder, quotient rounded to the nearest
 * integer) adjusted by one |y| where its sign differs from x's, is correct but far too slow: Z3 did
 * not decide {@code angle % 360 < -180} for a symbolic double within 120 s, while this model takes
 * milliseconds. Before this model the remainder had no formula at all, so every branch on it looked
 * concrete and a search that never ran into the violation reported SAFE (sv-benchmarks PieSegment).
 */
final class JavaRemainder {

    private JavaRemainder() {}

    /** The model of x % y, for operands of which at least one is symbolic. */
    static FloatingPointFormula approximate(SolverContext context, FloatingPointFormula x, FloatingPointFormula y,
                                            FloatingPointFormula concreteResult, SymbolicTraceHandler sth) {
        return SampledMath.define(context, "rem", sth, exactPieces(context, x, y, concreteResult), x, y);
    }

    /** x % y where the quotient is at most 1 in magnitude, `otherwise` elsewhere. */
    static FloatingPointFormula exactPieces(SolverContext context, FloatingPointFormula x, FloatingPointFormula y,
                                            FloatingPointFormula otherwise) {
        FloatingPointFormulaManager fp = context.getFormulaManager().getFloatingPointFormulaManager();
        var b = context.getFormulaManager().getBooleanFormulaManager();
        FloatingPointFormula ax = fp.abs(x);
        FloatingPointFormula ay = fp.abs(y);
        // -(|x| - |y|) rather than x + |y|: for x = -|y| Java gives -0.0, and the sum would be +0.0
        FloatingPointFormula diff = fp.subtract(ax, ay);
        FloatingPointFormula second = b.ifThenElse(fp.isNegative(x), fp.negate(diff), diff);
        FloatingPointFormula twoAy = fp.add(ay, ay); // exact, or infinite where 2|y| is not representable
        return b.ifThenElse(fp.lessThan(ax, ay), x, b.ifThenElse(fp.lessThan(ax, twoAy), second, otherwise));
    }
}
