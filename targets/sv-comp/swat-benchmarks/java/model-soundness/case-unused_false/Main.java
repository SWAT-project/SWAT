// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:xxxx
import org.sosy_lab.sv_benchmarks.Verifier;
// The lower case is computed but never branched on (as in securibench, where it is only printed):
// the model's pins are irrelevant, the search need not try every length, and SAFE is expected.
public class Main {
    static String sink;

    public static void main(String[] args) {
        String s = Verifier.nondetString();
        sink = s.toLowerCase(java.util.Locale.ROOT);
        if (s.length() > 3) {
            if (s.charAt(0) == 'x') assert false;
        }
    }
}
