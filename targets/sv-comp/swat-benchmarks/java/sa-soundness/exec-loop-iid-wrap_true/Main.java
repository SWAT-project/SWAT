// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1001 int:66
import org.sosy_lab.sv_benchmarks.Verifier;
// A branch in a loop is reported once per iteration, and its iid carries the iteration count,
// which wraps at 64: iterations 2 and 66 share an iid. Solving iteration 66 must not use iteration
// 2's constraint (the explorer's constraint cache was keyed by iid).
public class Main {
    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int hit = -1;
        for (int i = 0; i < 70; i++) {
            if (b == i) {
                hit = i;
            }
        }
        if (hit == 66) {
            if (a > 1000 && a < 1001) assert false;
        }
    }
}
