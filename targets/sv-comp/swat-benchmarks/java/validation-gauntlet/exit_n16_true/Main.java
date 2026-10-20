// Expected verdict: SAFE. target() needs a > 1000, b == 3 * a - 7 and b < a, i.e. a < 3.5: unsatisfiable. a < 100000 rules out overflow in 3 * a.
// Paired VIOLATION inputs (must not fail here): int:1001 int:2996 int:0 int:1 int:2 int:3 int:4 int:5 int:6 int:7 int:8 int:9 int:10 int:11 int:12 int:13 int:14 int:15
import org.sosy_lab.sv_benchmarks.Verifier;

// validation-gauntlet/exit: 16 checks that each leave early, then the only assert.
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
        int v8 = Verifier.nondetInt();
        int v9 = Verifier.nondetInt();
        int v10 = Verifier.nondetInt();
        int v11 = Verifier.nondetInt();
        int v12 = Verifier.nondetInt();
        int v13 = Verifier.nondetInt();
        int v14 = Verifier.nondetInt();
        int v15 = Verifier.nondetInt();
        run(a, b, v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15);
    }

    static void run(int a, int b, int v0, int v1, int v2, int v3, int v4, int v5, int v6, int v7, int v8, int v9, int v10, int v11, int v12, int v13, int v14, int v15) {
        if (v0 < 0) { report(0, v0); System.exit(1); }
        if (v1 < 1) { report(1, v1); System.exit(1); }
        if (v2 < 2) { report(2, v2); System.exit(1); }
        if (v3 < 3) { report(3, v3); System.exit(1); }
        if (v4 < 4) { report(4, v4); System.exit(1); }
        if (v5 < 5) { report(5, v5); System.exit(1); }
        if (v6 < 6) { report(6, v6); System.exit(1); }
        if (v7 < 7) { report(7, v7); System.exit(1); }
        if (v8 < 8) { report(8, v8); System.exit(1); }
        if (v9 < 9) { report(9, v9); System.exit(1); }
        if (v10 < 10) { report(10, v10); System.exit(1); }
        if (v11 < 11) { report(11, v11); System.exit(1); }
        if (v12 < 12) { report(12, v12); System.exit(1); }
        if (v13 < 13) { report(13, v13); System.exit(1); }
        if (v14 < 14) { report(14, v14); System.exit(1); }
        if (v15 < 15) { report(15, v15); System.exit(1); }
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
