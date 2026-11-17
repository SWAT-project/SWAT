// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:1a
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sosy_lab.sv_benchmarks.Verifier;
// The non-matching side of Matcher.matches() on a symbolic string.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        Pattern p = Pattern.compile("[0-9]{2}");
        Matcher m = p.matcher(s);
        if (!m.matches() && s.length() == 2 && s.charAt(0) >= '0' && s.charAt(0) <= '9') {
            if (s.charAt(1) == 'a') assert false;
        }
    }
}
