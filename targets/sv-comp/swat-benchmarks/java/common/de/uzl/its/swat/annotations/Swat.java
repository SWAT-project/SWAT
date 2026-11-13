package de.uzl.its.swat.annotations;

/**
 * Marker calls for hand-written facts about statements and loops, which Java annotations cannot
 * carry (they are not stored for statements). They compile to calls at exactly the right place
 * and do nothing at run time; SWAT recognizes them when user facts are on ({@code
 * -DuserFacts=true}, explorer {@code --user-facts}) and ignores them otherwise.
 */
public final class Swat {

    private Swat() {}

    /**
     * Starts a region whose branches cannot change whether a target is reached, nor any value a
     * target or a relevant branch reads (as {@link Irrelevant} for a method). The region is the
     * code between this call and the next {@link #endIrrelevant()} in the same method, in bytecode
     * order (a block or a loop, as written), and everything called from there. Regions do not nest.
     */
    public static void beginIrrelevant() {}

    /** Ends the region started by {@link #beginIrrelevant()}. */
    public static void endIrrelevant() {}

    /**
     * A hand-written loop summary, written as plain Java:
     *
     * <pre>
     * if (Swat.summary()) {
     *     s = s + (n * (n - 1) - i * (i - 1)) / 2;   // what the loop computes, in closed form
     *     i = n;
     * } else {
     *     while (i &lt; n) { s += i; i++; }
     * }
     * </pre>
     *
     * Returns false, so the program runs the loop. With user facts on, SWAT makes it return true,
     * so the executor runs the summary instead: one path with the closed form, instead of one path
     * per number of iterations, and SWAT's static analysis treats the loop's side as dead. The
     * summary is trusted: if it does not compute what the loop does, SWAT's verdict can be wrong. A
     * SAFE verdict that relied on it says so.
     */
    public static boolean summary() {
        return false;
    }
}
