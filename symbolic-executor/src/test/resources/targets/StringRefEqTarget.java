import de.uzl.its.swat.annotations.Symbolic;

/**
 * Compares a symbolic String to a string literal with reference equality ({@code ==}). The symbolic
 * input is de-interned, so it and the interned literal are distinct references and {@code ==} is
 * false. Reference equality is modeled without changing reference semantics.
 */
public class StringRefEqTarget {

    public static void main(String[] args) {
        test("seed");
    }

    public static String test(@Symbolic String s) {
        if (s == "MATCH") { // distinct references -> false
            return "eq";
        }
        return "ne";
    }
}
