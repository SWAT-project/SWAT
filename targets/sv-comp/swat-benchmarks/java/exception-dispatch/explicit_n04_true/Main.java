// Expected verdict: SAFE. target() needs a > 1000, b == 3 * a - 7 and b < a, i.e. a < 3.5: unsatisfiable. a < 100000 rules out overflow in 3 * a.
// Paired VIOLATION inputs (must not fail here): int:-4 int:1001 int:2996 int:0 int:0
import org.sosy_lab.sv_benchmarks.Verifier;

// exception-dispatch/explicit: the command is parsed from t; for t < 0 parsing throws
// (IllegalArgumentException) and the handler derives the command as -t - 1. Only command 3 reaches the assert.
public class Main {
    static class Holder {
        int base = 0;
    }

    static final Holder HOLDER = new Holder();

    static int parse(int t) {
        if (t < 0) throw new IllegalArgumentException();
        return HOLDER.base + t;
    }

    public static void main(String[] args) {
        int t = Verifier.nondetInt();
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int c = Verifier.nondetInt();
        int d = Verifier.nondetInt();
        int cmd;
        try {
            cmd = parse(t);
        } catch (IllegalArgumentException e) {
            cmd = -t - 1;
        }
        dispatch(cmd, a, b, c, d);
    }

    static int dispatch(int cmd, int a, int b, int c, int d) {
        int r;
        switch (cmd) {
            case 0: r = handler0(a, b, c, d); break;
            case 1: r = handler1(a, b, c, d); break;
            case 2: r = handler2(a, b, c, d); break;
            case 3: target(a, b); r = 0; break;
            default: r = -1;
        }
        return r;
    }

    static int handler0(int a, int b, int c, int d) {
        int r = 0;
        if (a > 0) r += 1;
        if (b > 0) r += 2;
        if (c < 0) r += 4;
        if (d == 5) r += 8;
        return r;
    }
    static int handler1(int a, int b, int c, int d) {
        int r = 1;
        if (a > 1) r += 1;
        if (b > 2) r += 2;
        if (c < -1) r += 4;
        if (d == 6) r += 8;
        return r;
    }
    static int handler2(int a, int b, int c, int d) {
        int r = 2;
        if (a > 2) r += 1;
        if (b > 4) r += 2;
        if (c < -2) r += 4;
        if (d == 7) r += 8;
        return r;
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
