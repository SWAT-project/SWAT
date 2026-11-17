// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:İ
import org.sosy_lab.sv_benchmarks.Verifier;
// toLowerCase may change the length: "\u0130" (I with dot above) lower-cases to two chars.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (s.length() == 1) {
            String l = s.toLowerCase();
            if (l.length() != 1) assert false;
        }
    }
}
