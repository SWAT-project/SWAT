// Expected verdict: VIOLATION. Violating inputs (nondet call order): char:b
// Character keys (boxed chars compare by value). SAFE: only 'b' maps to 2.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        char c = Verifier.nondetChar();
        Map<Character, Integer> m = new HashMap<>();
        m.put('a', 1);
        m.put('b', 2);
        Integer v = m.get(c);
        if (v != null && v == 2) assert false;
    }
}
