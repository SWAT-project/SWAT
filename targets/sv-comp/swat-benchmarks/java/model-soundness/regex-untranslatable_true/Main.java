// Expected verdict: SAFE. a match of a.c has three chars. "." is not translated: SWAT must say UNKNOWN.
// Paired VIOLATION inputs (must not fail here): string:abc
import java.util.regex.Pattern;
import org.sosy_lab.sv_benchmarks.Verifier;
// A regular expression SWAT does not translate ("."): context loss, SAFE must not be claimed.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (Pattern.compile("a.c").matcher(s).matches()) {
            if (s.length() != 3) assert false;
        }
    }
}
