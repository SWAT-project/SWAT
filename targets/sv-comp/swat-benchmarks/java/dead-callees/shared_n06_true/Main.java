// Expected verdict: SAFE. target() needs a > 1000, b == 3 * a - 7 and b < a, i.e. a < 3.5: unsatisfiable. a < 100000 rules out overflow in 3 * a.
// Paired VIOLATION inputs (must not fail here): int:0 int:100 int:1001 int:2996 int:0 int:0 int:0 int:0 int:0 int:0
import org.sosy_lab.sv_benchmarks.Verifier;

// dead-callees/shared: 6 helper calls that cannot reach the assert.
public class Main {
    static int statistics;

    public static void main(String[] args) {
        int mode = Verifier.nondetInt();
        int p = Verifier.nondetInt();
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int c0 = Verifier.nondetInt();
        int c1 = Verifier.nondetInt();
        int c2 = Verifier.nondetInt();
        int c3 = Verifier.nondetInt();
        int c4 = Verifier.nondetInt();
        int c5 = Verifier.nondetInt();
        if (mode == 1) {
            int stat = 0;
            stat += classify(c0);
            stat += classify(c1);
            stat += classify(c2);
            stat += classify(c3);
            stat += classify(c4);
            stat += classify(c5);
            statistics = stat;
            return;
        }
        if (classify(p) == 2) {
            target(a, b);
        }
    }

    // Two branches, three paths.
    static int classify(int x) {
        if (x < 0) return 0;
        if (x < 100) return 1;
        return 2;
    }

    // The only assertion in the program.
    static void target(int a, int b) {
        if (a > 1000 && a < 100000) {
            if (b == 3 * a - 7) {
                if (b < a) {
                    assert false;
                }
            }
        }
    }
}
