import de.uzl.its.swat.annotations.Symbolic;

/**
 * Passes a symbolic String to an unmodeled but whitelisted pure method ({@code String.trim()}) and
 * branches on the result. This exercises the generic uninterpreted-function path: the result is
 * modeled as {@code pure_String_trim(s)}, so the branch constraint references the input through the
 * UF. Contrasts with {@code ToLowerCaseTarget}, whose method is not whitelisted.
 */
public class TrimTarget {

    public static void main(String[] args) {
        test("abc");
    }

    public static String test(@Symbolic String s) {
        String r = s.trim(); // modeled as pure_String_trim(s)
        if (r.equals("abc")) {
            return "eq";
        }
        return "ne";
    }
}
