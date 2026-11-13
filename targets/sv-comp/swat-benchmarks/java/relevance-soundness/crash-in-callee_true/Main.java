// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:4 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// The initial run (val = 0) throws an uncaught exception in a callee inside the region.
// By design, the all-zero run ends early.
public class Main {
    static void check(int i, int v) {
        if (i > v + 5) {
            throw new RuntimeException("too far");
        }
    }

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        for (int i = 0; i < 10; i++) {
            if (i != val) {
                check(i, val);
            }
        }
        if (b > 1000 && b < 1001) assert false;
    }
}
