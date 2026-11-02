// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:97
import org.sosy_lab.sv_benchmarks.Verifier;
// loop-summary/ternary-sum: s grows by 2 in every iteration with i < val (a conditional expression).
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int s = 0;
        for (int i = 0; i < 100; i++) {
            s = i < val ? s + 2 : s;
        }
        if (s == 194) assert false;
    }
}
