// Expected verdict: SAFE. "x" lower-cases to "x", not "i".
// Paired VIOLATION inputs (must not fail here): string:İ
import org.sosy_lab.sv_benchmarks.Verifier;
// A Turkish locale maps I with dot above to the ASCII "i": not modelled (approximate).
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (s.length() == 1 && s.toLowerCase(new java.util.Locale("tr")).equals("i")) {
            if (s.equals("x")) assert false;
        }
    }
}
