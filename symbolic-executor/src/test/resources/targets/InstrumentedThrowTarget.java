import de.uzl.its.swat.annotations.Symbolic;

/**
 * A throw raised inside an INSTRUMENTED callee, dependent on a symbolic input. The branch guarding
 * the throw is itself symbolically tracked, so the throw decision IS modeled - no context loss may
 * be recorded. Pins the exception-path gate: only non-instrumented callees (where the next trace
 * instruction after the invoke is INVOKEMETHOD_EXCEPTION) flag; an instrumented callee's body is
 * stepped through instead. The exception uses the no-arg constructor so no unmodeled call with a
 * symbolic argument occurs anywhere on the path.
 */
public class InstrumentedThrowTarget {

    public static void main(String[] args) {
        test(5);
    }

    public static int test(@Symbolic int x) {
        int r;
        try {
            r = helper(x);
        } catch (IllegalStateException e) {
            r = -1; // swallowed: the run must exit 0
        }
        if (r > 0) r++;
        return r;
    }

    static int helper(int x) {
        if (x >= 0) {
            throw new IllegalStateException();
        }
        return x;
    }
}
