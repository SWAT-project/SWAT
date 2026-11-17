// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:5 int:5
// The identity of what remove returns and of the values left, with possibly equal symbolic
// Integer keys. SAFE: remove(y) returns v1 iff y == x or x == 1 and y == 1.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        int x = Verifier.nondetInt();
        int y = Verifier.nondetInt();
        Object v0 = new Object();
        Object v1 = new Object();
        Map<Integer, Object> m = new HashMap<>();
        m.put(1, v0);
        m.put(x, v1);
        Object removed = m.remove(y);
        if (removed == v1 && m.get(1) == v0 && x == 5) assert false;
    }
}
