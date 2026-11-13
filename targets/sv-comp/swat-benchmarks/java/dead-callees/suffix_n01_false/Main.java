// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:0 int:2 int:1001 int:2996 int:0
import org.sosy_lab.sv_benchmarks.Verifier;

// dead-callees/suffix: 1 helper calls that cannot reach the assert.
public class Main {
    static int statistics;

    public static void main(String[] args) {
        int mode = Verifier.nondetInt();
        int p = Verifier.nondetInt();
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int c0 = Verifier.nondetInt();
        target(a, b);
        int stat = 0;
        stat += helper0(c0);
        statistics = stat;
    }

    static int helper0(int x) {
        if (x < 0) return 0;
        if (x < 100) return 1;
        return 2;
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
