// Expected verdict: SAFE. only a or A upper-cases to A (and 1, -, ! to themselves), so the lower case is "a1-b!".
// Paired VIOLATION inputs (must not fail here): string:a1-B!
import org.sosy_lab.sv_benchmarks.Verifier;
// Digits and punctuation are kept, letters mapped: upper and lower case of the same string.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (s.toUpperCase().equals("A1-B!")) {
            if (!s.toLowerCase().equals("a1-b!")) assert false;
        }
    }
}
