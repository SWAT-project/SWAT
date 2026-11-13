// Expected verdict: SAFE. target() needs a > 1000, b == 3 * a - 7 and b < a, i.e. a < 3.5: unsatisfiable. a < 100000 rules out overflow in 3 * a.
// Paired VIOLATION inputs (must not fail here): int:1001 int:2996 int:0 int:1 int:2 int:3 int:4 int:5 int:6 int:7
import org.sosy_lab.sv_benchmarks.Verifier;

// validation-gauntlet/exit: 8 checks that each leave early, then the only assert.
public class Main {
    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int v0 = Verifier.nondetInt();
        int v1 = Verifier.nondetInt();
        int v2 = Verifier.nondetInt();
        int v3 = Verifier.nondetInt();
        int v4 = Verifier.nondetInt();
        int v5 = Verifier.nondetInt();
        int v6 = Verifier.nondetInt();
        int v7 = Verifier.nondetInt();
        run(a, b, v0, v1, v2, v3, v4, v5, v6, v7);
    }

    static void run(int a, int b, int v0, int v1, int v2, int v3, int v4, int v5, int v6, int v7) {
        if (v0 < 0) { report(0, v0); System.exit(1); }
        if (v1 < 1) { report(1, v1); System.exit(1); }
        if (v2 < 2) { report(2, v2); System.exit(1); }
        if (v3 < 3) { report(3, v3); System.exit(1); }
        if (v4 < 4) { report(4, v4); System.exit(1); }
        if (v5 < 5) { report(5, v5); System.exit(1); }
        if (v6 < 6) { report(6, v6); System.exit(1); }
        if (v7 < 7) { report(7, v7); System.exit(1); }
        stage1(a, b);
    }

    static void stage1(int a, int b) {
        stage2(a + 1, b);
    }

    static void stage2(int a, int b) {
        target(a - 1, b);
    }

    static int errors;

    // Classifies a rejected value; three branches, no assert.
    static void report(int check, int v) {
        if (v < -1000) errors += 100;
        if (v < -10) errors += 10;
        if (v == check - 1) errors += 1;
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
