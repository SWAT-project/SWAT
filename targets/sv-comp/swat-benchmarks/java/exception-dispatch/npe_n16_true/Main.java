// Expected verdict: SAFE. target() needs a > 1000, b == 3 * a - 7 and b < a, i.e. a < 3.5: unsatisfiable. a < 100000 rules out overflow in 3 * a.
// Paired VIOLATION inputs (must not fail here): int:-16 int:1001 int:2996 int:0 int:0
import org.sosy_lab.sv_benchmarks.Verifier;

// exception-dispatch/npe: the command is parsed from t; for t < 0 parsing throws
// (NullPointerException) and the handler derives the command as -t - 1. Only command 15 reaches the assert.
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
            case 1: r = handler1(a, b, c, d); break;
            case 2: r = handler2(a, b, c, d); break;
            case 3: r = handler3(a, b, c, d); break;
            case 4: r = handler4(a, b, c, d); break;
            case 5: r = handler5(a, b, c, d); break;
            case 6: r = handler6(a, b, c, d); break;
            case 7: r = handler7(a, b, c, d); break;
            case 8: r = handler8(a, b, c, d); break;
            case 9: r = handler9(a, b, c, d); break;
            case 10: r = handler10(a, b, c, d); break;
            case 11: r = handler11(a, b, c, d); break;
            case 12: r = handler12(a, b, c, d); break;
            case 13: r = handler13(a, b, c, d); break;
            case 14: r = handler14(a, b, c, d); break;
            case 15: target(a, b); r = 0; break;
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
    static int handler3(int a, int b, int c, int d) {
        int r = 3;
        if (a > 3) r += 1;
        if (b > 6) r += 2;
        if (c < -3) r += 4;
        if (d == 8) r += 8;
        return r;
    }
    static int handler4(int a, int b, int c, int d) {
        int r = 4;
        if (a > 4) r += 1;
        if (b > 8) r += 2;
        if (c < -4) r += 4;
        if (d == 9) r += 8;
        return r;
    }
    static int handler5(int a, int b, int c, int d) {
        int r = 5;
        if (a > 5) r += 1;
        if (b > 10) r += 2;
        if (c < -5) r += 4;
        if (d == 10) r += 8;
        return r;
    }
    static int handler6(int a, int b, int c, int d) {
        int r = 6;
        if (a > 6) r += 1;
        if (b > 12) r += 2;
        if (c < -6) r += 4;
        if (d == 11) r += 8;
        return r;
    }
    static int handler7(int a, int b, int c, int d) {
        int r = 7;
        if (a > 7) r += 1;
        if (b > 14) r += 2;
        if (c < -7) r += 4;
        if (d == 12) r += 8;
        return r;
    }
    static int handler8(int a, int b, int c, int d) {
        int r = 8;
        if (a > 8) r += 1;
        if (b > 16) r += 2;
        if (c < -8) r += 4;
        if (d == 13) r += 8;
        return r;
    }
    static int handler9(int a, int b, int c, int d) {
        int r = 9;
        if (a > 9) r += 1;
        if (b > 18) r += 2;
        if (c < -9) r += 4;
        if (d == 14) r += 8;
        return r;
    }
    static int handler10(int a, int b, int c, int d) {
        int r = 10;
        if (a > 10) r += 1;
        if (b > 20) r += 2;
        if (c < -10) r += 4;
        if (d == 15) r += 8;
        return r;
    }
    static int handler11(int a, int b, int c, int d) {
        int r = 11;
        if (a > 11) r += 1;
        if (b > 22) r += 2;
        if (c < -11) r += 4;
        if (d == 16) r += 8;
        return r;
    }
    static int handler12(int a, int b, int c, int d) {
        int r = 12;
        if (a > 12) r += 1;
        if (b > 24) r += 2;
        if (c < -12) r += 4;
        if (d == 17) r += 8;
        return r;
    }
    static int handler13(int a, int b, int c, int d) {
        int r = 13;
        if (a > 13) r += 1;
        if (b > 26) r += 2;
        if (c < -13) r += 4;
        if (d == 18) r += 8;
        return r;
    }
    static int handler14(int a, int b, int c, int d) {
        int r = 14;
        if (a > 14) r += 1;
        if (b > 28) r += 2;
        if (c < -14) r += 4;
        if (d == 19) r += 8;
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
