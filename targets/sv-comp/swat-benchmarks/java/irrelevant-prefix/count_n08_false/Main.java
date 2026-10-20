// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:2996 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0
import org.sosy_lab.sv_benchmarks.Verifier;

// irrelevant-prefix/count: 8 independent branches before the only assert (control: nothing to prune).
public class Main {
    static int count;

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int x0 = Verifier.nondetInt();
        int x1 = Verifier.nondetInt();
        int x2 = Verifier.nondetInt();
        int x3 = Verifier.nondetInt();
        int x4 = Verifier.nondetInt();
        int x5 = Verifier.nondetInt();
        int x6 = Verifier.nondetInt();
        int x7 = Verifier.nondetInt();
        if (x0 > 0) count++;
        if (x1 > 0) count++;
        if (x2 > 0) count++;
        if (x3 > 0) count++;
        if (x4 > 0) count++;
        if (x5 > 0) count++;
        if (x6 > 0) count++;
        if (x7 > 0) count++;
        target(a, b);
    }

    // The only assertion in the program.
    static void target(int a, int b) {
        if (a > 1000 && a < 100000) {
            if (b == 3 * a - 7) {
                if (b > a) {
                    assert false;
                }
            }
        }
    }
}
