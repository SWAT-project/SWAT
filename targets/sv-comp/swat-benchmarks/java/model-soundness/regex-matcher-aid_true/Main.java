// Expected verdict: SAFE. a match of the pattern starts with at least 10 hex digits.
// Paired VIOLATION inputs (must not fail here): string:0123456789#
import java.util.regex.Pattern;
import org.sosy_lab.sv_benchmarks.Verifier;
// The pattern of argv-mhe's AidGroup (escaped '*' and '#', a bounded repetition).
public class Main {
    private static final Pattern AID = Pattern.compile("[0-9A-Fa-f]{10,32}\\*?\\#?");

    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (AID.matcher(s).matches()) {
            if (s.length() < 10) assert false;
        }
    }
}
