// Expected verdict: SAFE. target() needs a > 1000, b == 3 * a - 7 and b < a, i.e. a < 3.5: unsatisfiable. a < 100000 rules out overflow in 3 * a.
// Paired VIOLATION inputs (must not fail here): int:7 int:1001 int:2996 int:0 int:0
import org.sosy_lab.sv_benchmarks.Verifier;

// virtual-dispatch/local: cmd selects one of 8 subclasses, only C7.run() reaches the assert.
public class Main {
    static abstract class Cmd {
        abstract int run(int a, int b, int c, int d);
    }

    static class C0 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 0;
            if (a > 0) r += 1;
            if (b > 0) r += 2;
            if (c < 0) r += 4;
            if (d == 5) r += 8;
            return r;
        }
    }

    static class C1 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 1;
            if (a > 1) r += 1;
            if (b > 2) r += 2;
            if (c < -1) r += 4;
            if (d == 6) r += 8;
            return r;
        }
    }

    static class C2 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 2;
            if (a > 2) r += 1;
            if (b > 4) r += 2;
            if (c < -2) r += 4;
            if (d == 7) r += 8;
            return r;
        }
    }

    static class C3 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 3;
            if (a > 3) r += 1;
            if (b > 6) r += 2;
            if (c < -3) r += 4;
            if (d == 8) r += 8;
            return r;
        }
    }

    static class C4 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 4;
            if (a > 4) r += 1;
            if (b > 8) r += 2;
            if (c < -4) r += 4;
            if (d == 9) r += 8;
            return r;
        }
    }

    static class C5 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 5;
            if (a > 5) r += 1;
            if (b > 10) r += 2;
            if (c < -5) r += 4;
            if (d == 10) r += 8;
            return r;
        }
    }

    static class C6 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 6;
            if (a > 6) r += 1;
            if (b > 12) r += 2;
            if (c < -6) r += 4;
            if (d == 11) r += 8;
            return r;
        }
    }

    static class C7 extends Cmd {
        int run(int a, int b, int c, int d) {
            target(a, b);
            return 0;
        }
    }

    public static void main(String[] args) {
        int cmd = Verifier.nondetInt();
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int c = Verifier.nondetInt();
        int d = Verifier.nondetInt();
        dispatch(cmd, a, b, c, d);
    }

    static int dispatch(int cmd, int a, int b, int c, int d) {
        switch (cmd) {
            case 0: return new C0().run(a, b, c, d);
            case 1: return new C1().run(a, b, c, d);
            case 2: return new C2().run(a, b, c, d);
            case 3: return new C3().run(a, b, c, d);
            case 4: return new C4().run(a, b, c, d);
            case 5: return new C5().run(a, b, c, d);
            case 6: return new C6().run(a, b, c, d);
            case 7: return new C7().run(a, b, c, d);
            default: return -1;
        }
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
