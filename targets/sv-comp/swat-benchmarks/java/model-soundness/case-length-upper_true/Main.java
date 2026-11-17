// Expected verdict: SAFE. the upper case of one char has at most three chars (e.g. U+0390).
// Paired VIOLATION inputs (must not fail here): string:ß
import org.sosy_lab.sv_benchmarks.Verifier;
// toUpperCase may change the length: "\u00df" (sharp s) upper-cases to "SS".
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (s.length() == 1) {
            String u = s.toUpperCase();
            if (u.length() > 3) assert false;
        }
    }
}
