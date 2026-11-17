// Expected verdict: SAFE. "x" lower-cases to "x", which has no "i".
// Paired VIOLATION inputs (must not fail here): string:İ
import org.sosy_lab.sv_benchmarks.Verifier;
// The lower case of "\u0130" is "i\u0307", which contains "i": a comparison with a string
// that has a char of a multi-char mapping is not modelled over the original string.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (s.length() == 1 && s.toLowerCase().contains("i")) {
            if (s.equals("x")) assert false;
        }
    }
}
