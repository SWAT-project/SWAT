// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:ab
import java.util.regex.Pattern;
import org.sosy_lab.sv_benchmarks.Verifier;
// The static Pattern.matches(regex, s), which is s.matches(regex).
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (Pattern.matches("[a-z]+", s)) {
            if (s.startsWith("a")) assert false;
        }
    }
}
