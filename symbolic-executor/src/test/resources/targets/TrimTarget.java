import de.uzl.its.swat.annotations.Symbolic;

/**
 * Level-2 target for the G4 end-to-end anchor: a symbolic String is passed to an unmodeled but
 * WHITELISTED pure method ({@code String.trim()}), whose result then drives a branch. Run under the
 * real SWAT agent, this exercises the generic-UF path ({@code pure_String_trim}) end to end: the
 * branch constraint should reference the input through the UF, and NEITHER soundness flag
 * (context-loss, precision-loss) should fire - so SAFE would be preserved through the call.
 *
 * Mirrors {@code ToLowerCaseTarget} (the non-whitelisted contrast, which still flags context loss).
 * {@code @Symbolic} is on the parameter. See docs/test-architecture.md (Level L2).
 */
public class TrimTarget {

    public static void main(String[] args) {
        test("abc");
    }

    public static String test(@Symbolic String s) {
        String r = s.trim(); // whitelisted pure + unmodeled -> modeled as pure_String_trim(s)
        if (r.equals("abc")) { // the UF result reaches a branch
            return "eq";
        }
        return "ne";
    }
}
