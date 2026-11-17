// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): char:a
// A Character key from a char constant, looked up with a symbolic char that is not 0. The
// shadow of the boxed constant must be 'a': with the default '\0' (as before the
// constructor models took int-typed constants) a model that compares it puts c == 0 on the
// path instead of c == 'a', and the VIOLATION task becomes a false SAFE. SAFE: only 'a' is
// a key. The all-zero run ends early.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        char c = Verifier.nondetChar();
        if (c == 0) return;
        Map<Character, Integer> m = new HashMap<>();
        m.put('a', 1);
        if (m.containsKey(c) && c != 'a') assert false;
    }
}
