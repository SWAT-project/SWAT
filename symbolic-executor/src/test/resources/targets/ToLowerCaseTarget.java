import de.uzl.its.swat.annotations.Symbolic;

/**
 * Passes a symbolic, concretely already-lowercase String to an unmodeled {@code toLowerCase()} that
 * returns {@code this}, then branches on the result. This exercises the identity-recovery / aliasing
 * path. The method is not on the purity whitelist, unlike {@code TrimTarget}.
 */
public class ToLowerCaseTarget {

    public static void main(String[] args) {
        test("abc");
    }

    public static String test(@Symbolic String s) {
        String r = s.toLowerCase();
        if (r.equals("abc")) {
            return "eq";
        }
        return "ne";
    }
}
