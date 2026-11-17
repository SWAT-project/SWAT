// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sosy_lab.sv_benchmarks.Verifier;
// A capturing Comparator is called back by Collections.sort (library code).
public class Main {
    public static void main(String[] args) {
        int x = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        List<Integer> list = new ArrayList<>();
        list.add(2);
        list.add(1);
        if (b == 7) {
            Collections.sort(list, (p, q) -> {
                if (x > 1000 && x < 100000) assert false;
                return p - q;
            });
        }
    }
}
