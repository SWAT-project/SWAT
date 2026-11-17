// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:ß
import org.sosy_lab.sv_benchmarks.Verifier;
// toUpperCase may change the length: "\u00df" (sharp s) upper-cases to "SS".
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (s.length() == 1) {
            String u = s.toUpperCase();
            if (u.length() != 1) assert false;
        }
    }
}
