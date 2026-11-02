// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:97
import org.sosy_lab.sv_benchmarks.Verifier;
// loop-summary/count-even: a nested condition per iteration (i % 2 on a constant divisor).
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int even = 0;
        int odd = 0;
        for (int i = 0; i < 100; i++) {
            if (i < val) {
                if (i % 2 == 0) {
                    even++;
                } else {
                    odd++;
                }
            }
        }
        if (even == 49) assert false;
    }
}
