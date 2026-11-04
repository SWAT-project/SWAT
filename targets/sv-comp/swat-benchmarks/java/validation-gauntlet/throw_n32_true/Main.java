// Expected verdict: SAFE. target() needs a > 1000, b == 3 * a - 7 and b < a, i.e. a < 3.5: unsatisfiable. a < 100000 rules out overflow in 3 * a.
// Paired VIOLATION inputs (must not fail here): int:1001 int:2996 int:0 int:1 int:2 int:3 int:4 int:5 int:6 int:7 int:8 int:9 int:10 int:11 int:12 int:13 int:14 int:15 int:16 int:17 int:18 int:19 int:20 int:21 int:22 int:23 int:24 int:25 int:26 int:27 int:28 int:29 int:30 int:31
import org.sosy_lab.sv_benchmarks.Verifier;

// validation-gauntlet/throw: 32 checks that each leave early, then the only assert.
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
        try {
            run(a, b, v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20, v21, v22, v23, v24, v25, v26, v27, v28, v29, v30, v31);
        } catch (IllegalArgumentException e) {
            return;
        }
    }

    static void run(int a, int b, int v0, int v1, int v2, int v3, int v4, int v5, int v6, int v7, int v8, int v9, int v10, int v11, int v12, int v13, int v14, int v15, int v16, int v17, int v18, int v19, int v20, int v21, int v22, int v23, int v24, int v25, int v26, int v27, int v28, int v29, int v30, int v31) {
        if (v0 < 0) throw new IllegalArgumentException();
        if (v1 < 1) throw new IllegalArgumentException();
        if (v2 < 2) throw new IllegalArgumentException();
        if (v3 < 3) throw new IllegalArgumentException();
        if (v4 < 4) throw new IllegalArgumentException();
        if (v5 < 5) throw new IllegalArgumentException();
        if (v6 < 6) throw new IllegalArgumentException();
        if (v7 < 7) throw new IllegalArgumentException();
        if (v8 < 8) throw new IllegalArgumentException();
        if (v9 < 9) throw new IllegalArgumentException();
        if (v10 < 10) throw new IllegalArgumentException();
        if (v11 < 11) throw new IllegalArgumentException();
        if (v12 < 12) throw new IllegalArgumentException();
        if (v13 < 13) throw new IllegalArgumentException();
        if (v14 < 14) throw new IllegalArgumentException();
        if (v15 < 15) throw new IllegalArgumentException();
        if (v16 < 16) throw new IllegalArgumentException();
        if (v17 < 17) throw new IllegalArgumentException();
        if (v18 < 18) throw new IllegalArgumentException();
        if (v19 < 19) throw new IllegalArgumentException();
        if (v20 < 20) throw new IllegalArgumentException();
        if (v21 < 21) throw new IllegalArgumentException();
        if (v22 < 22) throw new IllegalArgumentException();
        if (v23 < 23) throw new IllegalArgumentException();
        if (v24 < 24) throw new IllegalArgumentException();
        if (v25 < 25) throw new IllegalArgumentException();
        if (v26 < 26) throw new IllegalArgumentException();
        if (v27 < 27) throw new IllegalArgumentException();
        if (v28 < 28) throw new IllegalArgumentException();
        if (v29 < 29) throw new IllegalArgumentException();
        if (v30 < 30) throw new IllegalArgumentException();
        if (v31 < 31) throw new IllegalArgumentException();
        stage1(a, b);
    }

    static void stage1(int a, int b) {
        stage2(a + 1, b);
    }

    static void stage2(int a, int b) {
        target(a - 1, b);
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
