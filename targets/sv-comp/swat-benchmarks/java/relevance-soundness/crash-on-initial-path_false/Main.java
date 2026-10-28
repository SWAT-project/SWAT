// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:10 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// The initial run (val = 0) ends with an uncaught exception inside the region, before the assert.
// By design, the all-zero run ends early.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int[] a = new int[5];
        for (int i = 0; i < 10; i++) {
            if (i >= val) {
                a[i] = 1;
            }
        }
        if (b > 1000 && b < 100000) assert false;
    }
}
