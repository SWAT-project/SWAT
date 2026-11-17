// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:7
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sosy_lab.sv_benchmarks.Verifier;
// matches() called twice on one matcher (it does not depend on the earlier match).
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        Matcher m = Pattern.compile("[0-9]").matcher(s);
        boolean first = m.matches();
        boolean second = m.matches();
        if (first && second) assert false;
    }
}
