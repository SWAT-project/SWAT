// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:12
import java.util.regex.Pattern;
import org.sosy_lab.sv_benchmarks.Verifier;
// The matching side of Matcher.matches() on a symbolic string, the pattern in a static field.
public class Main {
    private static final Pattern DIGITS = Pattern.compile("[0-9]+");

    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (DIGITS.matcher(s).matches()) {
            if (s.startsWith("1")) assert false;
        }
    }
}
