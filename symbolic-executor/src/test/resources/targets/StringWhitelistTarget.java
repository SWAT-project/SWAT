import de.uzl.its.swat.annotations.Symbolic;

/**
 * Drives unmodeled pure String methods on a symbolic String receiver into branches, covering String,
 * int and boolean returns. Each result is modeled as a generic pure_<sig> uninterpreted function.
 * Uses a single String parameter: a category-2 parameter (double or long) followed by a reference
 * parameter trips a frame-analysis limitation in the instrumenter.
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
