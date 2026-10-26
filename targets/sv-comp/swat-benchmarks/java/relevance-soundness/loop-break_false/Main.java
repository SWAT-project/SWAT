// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:6 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// The branch leaves the loop early, which decides the counter read after it.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int i;
        for (i = 0; i < 10; i++) {
            if (i == val) {
                break;
            }
        }
        if (i == 6) {
            if (b > 1000 && b < 100000) assert false;
        }
    }
}
