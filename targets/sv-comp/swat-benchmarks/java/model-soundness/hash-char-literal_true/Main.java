// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): char:a
// A Character key from a char constant, looked up with a symbolic char that is not 0. (SWAT's
// shadow of the boxed constant was the default '\0' when this was written: a model that
// compares such a shadow puts c == 0 on the path instead of c == 'a', and the VIOLATION task
// becomes a false SAFE.) Must stay UNKNOWN unless the shadow is right. The all-zero run ends early.
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
