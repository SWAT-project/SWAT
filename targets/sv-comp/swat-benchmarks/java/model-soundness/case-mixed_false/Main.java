// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:a1-B!
import org.sosy_lab.sv_benchmarks.Verifier;
// Digits and punctuation are kept, letters mapped: upper and lower case of the same string.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (s.toUpperCase().equals("A1-B!")) {
            if (s.charAt(0) == 'a') assert false;
        }
    }
}
