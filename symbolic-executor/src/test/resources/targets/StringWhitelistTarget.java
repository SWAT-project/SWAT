import de.uzl.its.swat.annotations.Symbolic;

/**
 * Level-2 regression target for the String purity-audit additions (the StringValue stubs that the
 * audit confirmed are unmodeled, so the generic pure_<sig> UF fires). Drives a representative few -
 * String, int and boolean returns - on a symbolic String receiver into branches. Single String
 * parameter only (a category-2 double followed by a reference param trips an unrelated frame-analysis
 * fragility in the instrumenter). See docs/heap-redesign-g4-whitelist-survey.md (Level L2).
 */
public class StringWhitelistTarget {

    public static void main(String[] args) {
        test("hello world 42");
    }

    public static int test(@Symbolic String s) {
        int h = s.hashCode();            // int
        boolean m = s.matches(".*");     // boolean
        int c = s.compareTo("x");        // int
        String rp = s.repeat(2);         // String
        boolean b = s.isBlank();         // boolean
        int r = 0;
        if (h != 0) r++;
        if (m) r++;
        if (c != 0) r++;
        if (rp.length() > 0) r++;
        if (b) r++;
        return r;
    }
}
