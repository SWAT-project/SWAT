import de.uzl.its.swat.annotations.Symbolic;

/**
 * Drives pure, unmodeled, value-typed JDK methods from the purity whitelist on symbolic inputs into
 * branches. The methods span several owners and every return sort, including the String-to-float parse
 * bridge. Each result is modeled as a generic pure_<sig> uninterpreted function. The methods are kept
 * small and separate because large mixed-type method bodies trip a frame-analysis limitation in the
 * instrumenter.
 */
public class WhitelistAuditAgentTarget {

    public static void main(String[] args) {
        ma(2.0);
        mb(5);
        mc(5, 2.0);
        md("12.5");
    }

    // Math/StrictMath, double returns.
    public static int ma(@Symbolic double d) {
        double a = Math.tan(d), b = Math.pow(d, 2.0), c = Math.log(d), e = StrictMath.cbrt(d), f = Math.exp(d);
        int r = 0;
        if (a > 0) r++;
        if (b > 0) r++;
        if (c > 0) r++;
        if (e > 0) r++;
        if (f > 0) r++;
        return r;
    }

    // Math exact-arithmetic + Integer/Byte, int returns.
    public static int mb(@Symbolic int x) {
        int a = Math.addExact(x, 3), b = Math.multiplyExact(x, 2), c = Math.floorMod(x, 3),
                e = Integer.bitCount(x), f = Byte.hashCode((byte) x);
        int r = 0;
        if (a > 0) r++;
        if (b > 0) r++;
        if (c > 0) r++;
        if (e > 0) r++;
        if (f > 0) r++;
        return r;
    }

    // Double / Character (boolean) / Objects, mixed return sorts.
    public static int mc(@Symbolic int x, @Symbolic double d) {
        long a = Double.doubleToLongBits(d);
        double b = Double.max(d, 2.0);
        boolean c = Character.isLetter((char) x);
        int e = java.util.Objects.checkIndex(x, 100);
        int r = 0;
        if (a != 0) r++;
        if (b > 0) r++;
        if (c) r++;
        if (e >= 0) r++;
        return r;
    }

    // String -> float parse bridge.
    public static int md(@Symbolic String s) {
        float p = Float.parseFloat(s);
        return p > 0 ? 1 : 0;
    }
}
