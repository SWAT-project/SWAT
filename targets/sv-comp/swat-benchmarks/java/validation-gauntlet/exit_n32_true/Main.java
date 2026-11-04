// Expected verdict: SAFE. target() needs a > 1000, b == 3 * a - 7 and b < a, i.e. a < 3.5: unsatisfiable. a < 100000 rules out overflow in 3 * a.
// Paired VIOLATION inputs (must not fail here): int:1001 int:2996 int:0 int:1 int:2 int:3 int:4 int:5 int:6 int:7 int:8 int:9 int:10 int:11 int:12 int:13 int:14 int:15 int:16 int:17 int:18 int:19 int:20 int:21 int:22 int:23 int:24 int:25 int:26 int:27 int:28 int:29 int:30 int:31
import org.sosy_lab.sv_benchmarks.Verifier;

// validation-gauntlet/exit: 32 checks that each leave early, then the only assert.
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
        int v16 = Verifier.nondetInt();
        int v17 = Verifier.nondetInt();
        int v18 = Verifier.nondetInt();
        int v19 = Verifier.nondetInt();
        int v20 = Verifier.nondetInt();
        int v21 = Verifier.nondetInt();
        int v22 = Verifier.nondetInt();
        int v23 = Verifier.nondetInt();
        int v24 = Verifier.nondetInt();
        int v25 = Verifier.nondetInt();
        int v26 = Verifier.nondetInt();
        int v27 = Verifier.nondetInt();
        int v28 = Verifier.nondetInt();
        int v29 = Verifier.nondetInt();
        int v30 = Verifier.nondetInt();
        int v31 = Verifier.nondetInt();
        run(a, b, v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20, v21, v22, v23, v24, v25, v26, v27, v28, v29, v30, v31);
    }

    static void run(int a, int b, int v0, int v1, int v2, int v3, int v4, int v5, int v6, int v7, int v8, int v9, int v10, int v11, int v12, int v13, int v14, int v15, int v16, int v17, int v18, int v19, int v20, int v21, int v22, int v23, int v24, int v25, int v26, int v27, int v28, int v29, int v30, int v31) {
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
        if (v16 < 16) { report(16, v16); System.exit(1); }
        if (v17 < 17) { report(17, v17); System.exit(1); }
        if (v18 < 18) { report(18, v18); System.exit(1); }
        if (v19 < 19) { report(19, v19); System.exit(1); }
        if (v20 < 20) { report(20, v20); System.exit(1); }
        if (v21 < 21) { report(21, v21); System.exit(1); }
        if (v22 < 22) { report(22, v22); System.exit(1); }
        if (v23 < 23) { report(23, v23); System.exit(1); }
        if (v24 < 24) { report(24, v24); System.exit(1); }
        if (v25 < 25) { report(25, v25); System.exit(1); }
        if (v26 < 26) { report(26, v26); System.exit(1); }
        if (v27 < 27) { report(27, v27); System.exit(1); }
        if (v28 < 28) { report(28, v28); System.exit(1); }
        if (v29 < 29) { report(29, v29); System.exit(1); }
        if (v30 < 30) { report(30, v30); System.exit(1); }
        if (v31 < 31) { report(31, v31); System.exit(1); }
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
