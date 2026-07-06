import de.uzl.its.swat.annotations.Symbolic;

/**
 * A non-instrumented JDK call that throws as a function of a symbolic input:
 * Float.parseFloat on a symbolic (non-numeric) String. The exception is caught here so the run
 * exits 0; the agent must still record symbolic context loss, because whether the throw happens is
 * control flow the symbolic layer did not model (the whitelist's UF covers only the returned value
 * on a normal return, never the thrown-or-not decision).
 */
public class ExceptionalThrowTarget {

    public static void main(String[] args) {
        test("fixed");
    }

    public static int test(@Symbolic String s) {
        float f = -1.0f;
        try {
            f = Float.parseFloat(s);
        } catch (NumberFormatException e) {
            // swallowed: the run must exit 0
        }
        int r = 0;
        if (f > 0) r++;
        return r;
    }
}
