// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:2996 int:0 int:1
import org.sosy_lab.sv_benchmarks.Verifier;

// validation-gauntlet/report: 2 checks that each leave early, then the only assert.
public class Main {
    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int v0 = Verifier.nondetInt();
        int v1 = Verifier.nondetInt();
        run(a, b, v0, v1);
    }

    static void run(int a, int b, int v0, int v1) {
        if (v0 < 0) { report(0, v0); return; }
        if (v1 < 1) { report(1, v1); return; }
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
                if (b > a) {
                    assert false;
                }
            }
        }
    }
}
