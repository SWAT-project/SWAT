// Expected verdict: SAFE. a string of two chars that are both digits matches [0-9]{2}.
// Paired VIOLATION inputs (must not fail here): string:1a
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
            if (s.charAt(1) >= '0' && s.charAt(1) <= '9') assert false;
        }
    }
}
