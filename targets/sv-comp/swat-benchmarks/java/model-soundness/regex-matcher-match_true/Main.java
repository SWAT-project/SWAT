// Expected verdict: SAFE. a string of digits only does not start with "x".
// Paired VIOLATION inputs (must not fail here): string:12
import java.util.regex.Pattern;
import org.sosy_lab.sv_benchmarks.Verifier;
// The matching side of Matcher.matches() on a symbolic string, the pattern in a static field.
public class Main {
    private static final Pattern DIGITS = Pattern.compile("[0-9]+");

    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (DIGITS.matcher(s).matches()) {
            if (s.startsWith("x")) assert false;
        }
    }
}
