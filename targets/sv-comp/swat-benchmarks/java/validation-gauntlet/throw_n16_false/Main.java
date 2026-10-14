// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:2996 int:0 int:1 int:2 int:3 int:4 int:5 int:6 int:7 int:8 int:9 int:10 int:11 int:12 int:13 int:14 int:15
import org.sosy_lab.sv_benchmarks.Verifier;

// validation-gauntlet/throw: 16 checks that each leave early, then the only assert.
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
        try {
            run(a, b, v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15);
        } catch (IllegalArgumentException e) {
            return;
        }
    }

    static void run(int a, int b, int v0, int v1, int v2, int v3, int v4, int v5, int v6, int v7, int v8, int v9, int v10, int v11, int v12, int v13, int v14, int v15) {
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
                if (b > a) {
                    assert false;
                }
            }
        }
    }
}
