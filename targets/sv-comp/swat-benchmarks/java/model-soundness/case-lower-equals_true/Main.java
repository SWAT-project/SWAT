// Expected verdict: SAFE. only "abc", "ABC" and the like lower-case to "abc" (no char beyond ASCII has a, b or c as its lower case).
// Paired VIOLATION inputs (must not fail here): string:Abc
import org.sosy_lab.sv_benchmarks.Verifier;
// A symbolic string whose lower case equals a constant.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (s.toLowerCase().equals("abc")) {
            if (s.length() != 3) assert false;
        }
    }
}
