// Expected verdict: SAFE. even counts the even i < min(val, n): at most n / 2.
// Paired VIOLATION inputs (must not fail here): int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// loop-summary/count-even: a nested condition per iteration (i % 2 on a constant divisor).
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
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
        if (even == 6) assert false;
    }
}
