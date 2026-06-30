import de.uzl.its.swat.annotations.Symbolic;

/**
 * Level-2 acceptance for G3-B: a symbolic String compared to a literal with {@code ==} (rewritten to
 * UtilInstrumented.refEquals, now root-based). The de-interned input and the literal have different
 * roots, so {@code ==} is reference-false - and, crucially, modeling it through Provenance.root must
 * fire NEITHER soundness flag (no symbolic-context-loss from the stepped refEquals body, no
 * reference-semantic-change), unlike the old value-equality refEquals which fired both. See G3-B.
 */
public class StringRefEqTarget {

    public static void main(String[] args) {
        test("seed");
    }

    public static String test(@Symbolic String s) {
        if (s == "MATCH") { // de-interned ==: root(s) vs interned "MATCH" -> distinct -> false, no flags
            return "eq";
        }
        return "ne";
    }
}
