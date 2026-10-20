// Expected verdict: SAFE. target() needs a > 1000, b == 3 * a - 7 and b < a, i.e. a < 3.5: unsatisfiable. a < 100000 rules out overflow in 3 * a.
// Paired VIOLATION inputs (must not fail here): int:1001 int:2996 int:0 int:0 int:0 int:0
import org.sosy_lab.sv_benchmarks.Verifier;

// error-handling/explicit: 4 steps that can fail with IllegalArgumentException; each catch block logs and returns.
public class Main {
    static int logged;

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int x0 = Verifier.nondetInt();
        int x1 = Verifier.nondetInt();
        int x2 = Verifier.nondetInt();
        int x3 = Verifier.nondetInt();
        int acc = 0;
        try {
            acc += step0(x0);
        } catch (IllegalArgumentException e) {
            log(0, x0);
            return;
        }
        try {
            acc += step1(x1);
        } catch (IllegalArgumentException e) {
            log(1, x1);
            return;
        }
        try {
            acc += step2(x2);
        } catch (IllegalArgumentException e) {
            log(2, x2);
            return;
        }
        try {
            acc += step3(x3);
        } catch (IllegalArgumentException e) {
            log(3, x3);
            return;
        }
        target(a, b);
    }

    static int step0(int x) {
        if (x < -1) throw new IllegalArgumentException();
        return x + 0;
    }
    static int step1(int x) {
        if (x < -2) throw new IllegalArgumentException();
        return x + 1;
    }
    static int step2(int x) {
        if (x < -3) throw new IllegalArgumentException();
        return x + 2;
    }
    static int step3(int x) {
        if (x < -4) throw new IllegalArgumentException();
        return x + 3;
    }

    // Logs a failed step; three branches, no assert.
    static void log(int step, int x) {
        if (x < -1000) logged += 100;
        if (x > 1000) logged += 10;
        if (x == step) logged += 1;
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
