import de.uzl.its.swat.annotations.Symbolic;

/**
 * Calls pure, unmodeled java.lang methods with primitive return types on symbolic inputs and branches
 * on each result. Each result is modeled as a generic {@code pure_<sig>} uninterpreted function over
 * its inputs rather than concretized, so the branches stay symbolic. Covers int, long, double, float,
 * char and boolean returns (short and byte have no suitable pure unmodeled JDK method).
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
