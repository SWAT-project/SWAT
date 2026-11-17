// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): int:2
// HashSet.remove after adding a symbolic Integer. SAFE: one element is left only if x was
// 1 or 2.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        int x = Verifier.nondetInt();
        Set<Integer> s = new HashSet<>();
        s.add(1);
        s.add(2);
        s.add(x);
        boolean removed = s.remove(2);
        if (removed && s.size() == 1 && x != 1 && x != 2) assert false;
    }
}
