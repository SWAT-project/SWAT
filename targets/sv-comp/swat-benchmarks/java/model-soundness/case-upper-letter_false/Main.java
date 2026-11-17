// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:Ok
import org.sosy_lab.sv_benchmarks.Verifier;
// A violation only an upper-case letter reaches.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        String l = s.toLowerCase();
        if (s.length() == 2 && l.equals("ok")) {
            if (!s.equals("ok")) assert false;
        }
    }
}
