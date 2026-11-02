// Expected verdict: SAFE. s is at most 2n.
// Paired VIOLATION inputs (must not fail here): int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// loop-summary/ternary-sum: s grows by 2 in every iteration with i < val (a conditional expression).
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int s = 0;
        for (int i = 0; i < 10; i++) {
            s = i < val ? s + 2 : s;
        }
        if (s == 22) assert false;
    }
}
