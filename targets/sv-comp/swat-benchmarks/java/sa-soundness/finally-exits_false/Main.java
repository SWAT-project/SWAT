// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// try/finally with return and throw inside try: javac copies the finally block (with its branch)
// onto each exit path. The assert is behind the throwing exit.
public class Main {
    static int cnt;

    static int f(int a, int b) {
        try {
            if (b == 8) return 1;
            if (b == 7) throw new IllegalStateException();
            return 0;
        } finally {
            cnt++;
            if (a > 50) cnt++;
        }
    }

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int r;
        try {
            r = f(a, b);
        } catch (IllegalStateException e) {
            r = 2;
        }
        if (r == 2) {
            if (a > 1000 && a < 100000) assert false;
        }
    }
}
