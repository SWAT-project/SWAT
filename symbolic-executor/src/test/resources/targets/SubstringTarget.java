import de.uzl.its.swat.annotations.Symbolic;

/**
 * Passes a symbolic String to an unmodeled but whitelisted pure method that takes an argument
 * ({@code String.substring(int)}) and branches on the result. This exercises the mixed-sort generic
 * uninterpreted function {@code pure_String_substring_int(String, int)}, so the branch references the
 * input through the UF. Complements {@code TrimTarget}, which uses a no-argument method.
 */
public class SubstringTarget {

    public static void main(String[] args) {
        test("abcd");
    }

    public static String test(@Symbolic String s) {
        String r = s.substring(1); // modeled as pure_String_substring_int(s, 1)
        if (r.equals("bcd")) {
            return "eq";
        }
        return "ne";
    }
}
