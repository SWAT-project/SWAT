// Expected verdict: SAFE. with CASE_INSENSITIVE a match is letters only, so it has no digit "1". Flags are not modelled: UNKNOWN.
// Paired VIOLATION inputs (must not fail here): string:AB
import java.util.regex.Pattern;
import org.sosy_lab.sv_benchmarks.Verifier;
// A pattern with flags (CASE_INSENSITIVE): not modelled, context loss. A model that ignored the
// flags would hide the violation (an upper-case match).
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        Pattern p = Pattern.compile("[a-z]+", Pattern.CASE_INSENSITIVE);
        if (p.matcher(s).matches()) {
            if (s.startsWith("1")) assert false;
        }
    }
}
