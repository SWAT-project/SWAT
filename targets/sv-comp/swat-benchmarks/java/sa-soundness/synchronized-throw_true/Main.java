// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// An ArithmeticException leaves a synchronized block (through its catch-any handler, which
// rethrows) and is caught outside, where the assert is.
public class Main {
    static final Object LOCK = new Object();

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int r;
        try {
            synchronized (LOCK) {
                r = 100 / (b - 7);
                if (r > 3) r--;
            }
        } catch (ArithmeticException e) {
            if (a > 1000 && a < 1001) assert false;
            r = 0;
        }
        if (r > 50) r--;
    }
}
