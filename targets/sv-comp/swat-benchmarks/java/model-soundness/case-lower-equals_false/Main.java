// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:Abc
import org.sosy_lab.sv_benchmarks.Verifier;
// A symbolic string whose lower case equals a constant.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (s.toLowerCase().equals("abc")) {
            if (!s.equals("abc")) assert false;
        }
    }
}
