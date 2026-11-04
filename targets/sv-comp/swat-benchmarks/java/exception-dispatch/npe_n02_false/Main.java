// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:-2 int:1001 int:2996 int:0 int:0
import org.sosy_lab.sv_benchmarks.Verifier;

// exception-dispatch/npe: the command is parsed from t; for t < 0 parsing throws
// (NullPointerException) and the handler derives the command as -t - 1. Only command 1 reaches the assert.
public class Main {
    static class Holder {
        int base = 0;
    }

    static final Holder HOLDER = new Holder();

    static int parse(int t) {
        Holder h = t < 0 ? null : HOLDER;
        return h.base + t;
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
        } catch (NullPointerException e) {
            cmd = -t - 1;
        }
        dispatch(cmd, a, b, c, d);
    }

    static int dispatch(int cmd, int a, int b, int c, int d) {
        int r;
        switch (cmd) {
            case 0: r = handler0(a, b, c, d); break;
            case 1: target(a, b); r = 0; break;
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
