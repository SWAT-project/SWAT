// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// The assert is inside a synchronized block (monitorexit exception handler) and a synchronized
// method.
public class Main {
    static final Object LOCK = new Object();
    static int cnt;

    static synchronized void bump(int a) {
        if (a > 3) cnt++;
    }

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        bump(a);
        synchronized (LOCK) {
            if (b == 7) {
                if (a > 1000 && a < 1001) assert false;
            }
        }
        if (a > 5) cnt++;
    }
}
