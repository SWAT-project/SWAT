import de.uzl.its.swat.annotations.Symbolic;

/**
 * Level-2 target for V-1 / R-1: a symbolic, concretely already-lowercase String is passed to an
 * unmodeled {@code toLowerCase()} that returns {@code this}. Run under the real SWAT agent
 * (ANNOTATION mode, the default), this exercises the identity-recovery / aliasing path.
 *
 * {@code @Symbolic} is on the parameter (the annotation transformer handles parameters cleanly;
 * a local needs the {@code -g} debug table). Compiled against the agent jar, so it has no external
 * dependency. See docs/test-architecture.md (Level L2).
 */
public class ToLowerCaseTarget {

    public static void main(String[] args) {
        test("abc");
    }

    public static String test(@Symbolic String s) {
        String r = s.toLowerCase();
        if (r.equals("abc")) { // branch on the (currently aliased) result
            return "eq";
        }
        return "ne";
    }
}
