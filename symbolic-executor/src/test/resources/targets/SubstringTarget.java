import de.uzl.its.swat.annotations.Symbolic;

/**
 * Level-2 target for the G4 arg-taking shape: a symbolic String is passed to an unmodeled but
 * WHITELISTED pure method that TAKES AN ARGUMENT ({@code String.substring(int)}), whose result drives
 * a branch. Exercises the mixed-sort generic UF ({@code pure_String_substring_int(String, int)}) end
 * to end: the branch should reference the input through the UF, and NEITHER soundness flag should
 * fire. Complements TrimTarget (the no-arg shape). See docs/test-architecture.md (Level L2).
 */
public class SubstringTarget {

    public static void main(String[] args) {
        test("abcd");
    }

    public static String test(@Symbolic String s) {
        String r = s.substring(1); // whitelisted pure + unmodeled, arg-taking -> pure_String_substring_int(s, 1)
        if (r.equals("bcd")) { // the UF result reaches a branch
            return "eq";
        }
        return "ne";
    }
}
