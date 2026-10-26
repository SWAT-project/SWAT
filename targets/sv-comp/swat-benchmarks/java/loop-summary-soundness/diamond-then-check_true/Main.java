// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:5 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// Merged counters feed a later input-dependent check.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int even = 0;
        int odd = 0;
        for (int i = 0; i < 10; i++) {
            if (i < val) {
                if (i % 2 == 0) {
                    even++;
                } else {
                    odd++;
                }
            }
        }
        if (even == 3 && odd == 2) {
            if (b > 1000 && b < 1001) assert false;
        }
    }
}
