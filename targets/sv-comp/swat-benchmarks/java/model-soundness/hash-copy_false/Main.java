// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1 int:7
// Copy constructor and putAll with symbolic Integer keys: the copies hold the same entries,
// and putAll replaces the value of an equal key. SAFE: only y == 7 puts 2 under 7 (x == 7
// with y == x is excluded).
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        int x = Verifier.nondetInt();
        int y = Verifier.nondetInt();
        Map<Integer, Integer> m = new HashMap<>();
        m.put(x, 1);
        m.put(y, 2);
        Map<Integer, Integer> c = new HashMap<>(m);
        Map<Integer, Integer> d = new HashMap<>();
        d.put(7, 0);
        d.putAll(c);
        if (d.size() == 2 && d.get(7) == 2 && x != 7) assert false;
    }
}
