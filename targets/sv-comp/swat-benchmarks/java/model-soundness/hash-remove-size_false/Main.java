// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:3
// remove with a symbolic Integer key, then size and containsKey. SAFE: only x == 3 removes
// the entry of 3.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        int x = Verifier.nondetInt();
        Map<Integer, String> m = new HashMap<>();
        m.put(1, "a");
        m.put(2, "b");
        m.put(3, "c");
        String removed = m.remove(x);
        if (m.size() == 2 && !m.containsKey(3) && removed.equals("c")) assert false;
    }
}
