import de.uzl.its.swat.annotations.Symbolic;

/**
 * Level-2 target for G4 primitive-return UF support: an instrumented method calls un-instrumented,
 * pure, UNMODELED java.lang methods whose return type is a PRIMITIVE, on symbolic inputs, and branches
 * on each result. Under the real agent each result is modeled as a generic {@code pure_<sig>} UF over
 * its inputs (not concretized), so the branches stay symbolic with NO context loss and NO precision
 * loss. Covers int, long, double, float, char and boolean returns (short/byte have no pure unmodeled
 * JDK method and are pinned at L0 by PureUFPrimitiveRecoverySpec). See docs/heap-redesign-g4-* .
 */
public class PurePrimReturnTarget {

    public static void main(String[] args) {
        test(7, 3.0);
    }

    public static int test(@Symbolic int x, @Symbolic double d) {
        int fd = Math.floorDiv(x, 3);              // int
        int fm = Math.floorMod(x, 3);              // int
        long fdl = Math.floorDiv((long) x, 3L);    // long
        double cb = Math.cbrt(d);                  // double
        float fb = Float.intBitsToFloat(x);        // float
        char lc = Character.toLowerCase((char) x); // char
        boolean dig = Character.isDigit((char) x); // boolean

        int r = 0;
        if (fd > 0) r++;
        if (fm > 0) r++;
        if (fdl > 0L) r++;
        if (cb > 0.0) r++;
        if (fb > 0.0f) r++;
        if (lc > 0) r++;
        if (dig) r++;
        return r;
    }
}
