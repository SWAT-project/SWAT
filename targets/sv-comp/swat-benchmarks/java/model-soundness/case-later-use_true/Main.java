// Expected verdict: SAFE. "xyzw" lower-cases to "xyzw", not "abcd".
// Paired VIOLATION inputs (must not fail here): string:Abcd
import org.sosy_lab.sv_benchmarks.Verifier;
// The lower case is branched on only where the string has another length than in the first run
// (the pins of the first run are irrelevant there, those of the run of length 4 are not).
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        String l = s.toLowerCase();
        if (s.length() == 4) {
            if (l.equals("abcd") && s.equals("xyzw")) assert false;
        }
    }
}
