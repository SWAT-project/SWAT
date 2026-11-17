// Expected verdict: SAFE. only K, k and U+212A (KELVIN SIGN) lower-case to "k".
// Paired VIOLATION inputs (must not fail here): string:K
import org.sosy_lab.sv_benchmarks.Verifier;
// A violation only a char beyond ASCII reaches: the Kelvin sign lower-cases to the ASCII "k".
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (s.length() == 1 && s.toLowerCase().equals("k")) {
            char c = s.charAt(0);
            if (c != 'K' && c != 'k' && c != '\u212a') assert false;
        }
    }
}
