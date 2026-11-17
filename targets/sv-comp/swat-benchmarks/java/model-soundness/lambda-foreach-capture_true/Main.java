// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an integer x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1000 int:7
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.sv_benchmarks.Verifier;
// A capturing lambda is called back by List.forEach (library code): must stay a context loss unless modelled.
public class Main {
    public static void main(String[] args) {
        int x = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        List<Integer> list = new ArrayList<>();
        list.add(1);
        if (b == 7) {
            list.forEach(v -> {
                int s = x + v;
                if (s > 1000 && s < 1001) assert false;
            });
        }
    }
}
