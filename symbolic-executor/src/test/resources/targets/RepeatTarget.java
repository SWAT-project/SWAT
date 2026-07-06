import de.uzl.its.swat.annotations.Symbolic;

/**
 * Passes a symbolic String receiver plus an int argument to a whitelisted pure unmodeled method
 * ({@code String.repeat(int)}) and branches on the result. This exercises the mixed-sort generic
 * uninterpreted function {@code pure_String_repeat_int(String, int)}, so the branch references the
 * input through the UF. Complements {@code TrimTarget}, which uses a no-argument method.
 * NOTE: the concrete seed values here must NOT make any whitelisted call throw - a throwing
 * call takes the exception path, which records context loss by design and would (correctly)
 * flip this target's no-context-loss assertion.
 */
public class RepeatTarget {

    public static void main(String[] args) {
        test("abcd");
    }

    public static int test(@Symbolic String s) {
        String r = s.repeat(2);
        int n = 0;
        if (r.length() > 4) n++; // length is modeled; the branch depends on the UF result
        return n;
    }
}
