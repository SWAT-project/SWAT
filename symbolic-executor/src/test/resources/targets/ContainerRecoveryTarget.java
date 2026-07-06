import de.uzl.its.swat.annotations.Symbolic;
import java.util.HashMap;

/**
 * Stores a symbolic String in an unmodeled container (HashMap) and retrieves it via a concrete key,
 * then branches on the retrieved value. The stored value is first derived via a String operation, so
 * it is a tracked (registered) symbolic value by the time it enters the container; recovering its
 * shadow on the way out keeps the branch dependent on the symbolic input rather than concretizing it.
 * A single String parameter avoids an unrelated instrumenter frame-analysis limitation on mixed
 * category-2/reference signatures.
 * NOTE: the concrete seed values here must NOT make any whitelisted call throw - a throwing
 * call takes the exception path, which records context loss by design and would (correctly)
 * flip this target's no-context-loss assertion.
 */
public class ContainerRecoveryTarget {

    public static void main(String[] args) {
        test("hello");
    }

    public static int test(@Symbolic String s) {
        String name = s.substring(1); // a derived, tracked symbolic String ("ello")
        HashMap<String, String> m = new HashMap<>();
        m.put("k", name);
        String v = m.get("k");
        if (v.charAt(0) == 'e') {
            return 1;
        }
        return 0;
    }
}
