// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:- int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// Z3 may solve s.charAt(0) == '-' with a supplementary code point (e.g. U+2002D): the executor
// encodes charAt as the code point truncated to 16 bits, and String inputs have no range bound,
// so the next run diverges.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        int a = Verifier.nondetInt();
        if (s.length() == 1 && s.charAt(0) == '-') {
            if (a > 1000 && a < 100000) assert false;
        }
    }
}
