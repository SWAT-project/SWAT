// Expected verdict: SAFE. a string of digits only does not start with "a".
// Paired VIOLATION inputs (must not fail here): string:ab
import java.util.regex.Pattern;
import org.sosy_lab.sv_benchmarks.Verifier;
// The static Pattern.matches(regex, s), which is s.matches(regex).
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (Pattern.matches("[0-9]+", s)) {
            if (s.startsWith("a")) assert false;
        }
    }
}
