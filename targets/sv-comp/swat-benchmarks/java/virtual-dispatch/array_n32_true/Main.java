// Expected verdict: SAFE. target() needs a > 1000, b == 3 * a - 7 and b < a, i.e. a < 3.5: unsatisfiable. a < 100000 rules out overflow in 3 * a.
// Paired VIOLATION inputs (must not fail here): int:31 int:1001 int:2996 int:0 int:0
import org.sosy_lab.sv_benchmarks.Verifier;

// virtual-dispatch/array: cmd selects one of 32 subclasses, only C31.run() reaches the assert.
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
            int r = 7;
            if (a > 7) r += 1;
            if (b > 14) r += 2;
            if (c < -7) r += 4;
            if (d == 12) r += 8;
            return r;
        }
    }

    static class C8 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 8;
            if (a > 8) r += 1;
            if (b > 16) r += 2;
            if (c < -8) r += 4;
            if (d == 13) r += 8;
            return r;
        }
    }

    static class C9 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 9;
            if (a > 9) r += 1;
            if (b > 18) r += 2;
            if (c < -9) r += 4;
            if (d == 14) r += 8;
            return r;
        }
    }

    static class C10 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 10;
            if (a > 10) r += 1;
            if (b > 20) r += 2;
            if (c < -10) r += 4;
            if (d == 15) r += 8;
            return r;
        }
    }

    static class C11 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 11;
            if (a > 11) r += 1;
            if (b > 22) r += 2;
            if (c < -11) r += 4;
            if (d == 16) r += 8;
            return r;
        }
    }

    static class C12 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 12;
            if (a > 12) r += 1;
            if (b > 24) r += 2;
            if (c < -12) r += 4;
            if (d == 17) r += 8;
            return r;
        }
    }

    static class C13 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 13;
            if (a > 13) r += 1;
            if (b > 26) r += 2;
            if (c < -13) r += 4;
            if (d == 18) r += 8;
            return r;
        }
    }

    static class C14 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 14;
            if (a > 14) r += 1;
            if (b > 28) r += 2;
            if (c < -14) r += 4;
            if (d == 19) r += 8;
            return r;
        }
    }

    static class C15 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 15;
            if (a > 15) r += 1;
            if (b > 30) r += 2;
            if (c < -15) r += 4;
            if (d == 20) r += 8;
            return r;
        }
    }

    static class C16 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 16;
            if (a > 16) r += 1;
            if (b > 32) r += 2;
            if (c < -16) r += 4;
            if (d == 21) r += 8;
            return r;
        }
    }

    static class C17 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 17;
            if (a > 17) r += 1;
            if (b > 34) r += 2;
            if (c < -17) r += 4;
            if (d == 22) r += 8;
            return r;
        }
    }

    static class C18 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 18;
            if (a > 18) r += 1;
            if (b > 36) r += 2;
            if (c < -18) r += 4;
            if (d == 23) r += 8;
            return r;
        }
    }

    static class C19 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 19;
            if (a > 19) r += 1;
            if (b > 38) r += 2;
            if (c < -19) r += 4;
            if (d == 24) r += 8;
            return r;
        }
    }

    static class C20 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 20;
            if (a > 20) r += 1;
            if (b > 40) r += 2;
            if (c < -20) r += 4;
            if (d == 25) r += 8;
            return r;
        }
    }

    static class C21 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 21;
            if (a > 21) r += 1;
            if (b > 42) r += 2;
            if (c < -21) r += 4;
            if (d == 26) r += 8;
            return r;
        }
    }

    static class C22 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 22;
            if (a > 22) r += 1;
            if (b > 44) r += 2;
            if (c < -22) r += 4;
            if (d == 27) r += 8;
            return r;
        }
    }

    static class C23 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 23;
            if (a > 23) r += 1;
            if (b > 46) r += 2;
            if (c < -23) r += 4;
            if (d == 28) r += 8;
            return r;
        }
    }

    static class C24 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 24;
            if (a > 24) r += 1;
            if (b > 48) r += 2;
            if (c < -24) r += 4;
            if (d == 29) r += 8;
            return r;
        }
    }

    static class C25 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 25;
            if (a > 25) r += 1;
            if (b > 50) r += 2;
            if (c < -25) r += 4;
            if (d == 30) r += 8;
            return r;
        }
    }

    static class C26 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 26;
            if (a > 26) r += 1;
            if (b > 52) r += 2;
            if (c < -26) r += 4;
            if (d == 31) r += 8;
            return r;
        }
    }

    static class C27 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 27;
            if (a > 27) r += 1;
            if (b > 54) r += 2;
            if (c < -27) r += 4;
            if (d == 32) r += 8;
            return r;
        }
    }

    static class C28 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 28;
            if (a > 28) r += 1;
            if (b > 56) r += 2;
            if (c < -28) r += 4;
            if (d == 33) r += 8;
            return r;
        }
    }

    static class C29 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 29;
            if (a > 29) r += 1;
            if (b > 58) r += 2;
            if (c < -29) r += 4;
            if (d == 34) r += 8;
            return r;
        }
    }

    static class C30 extends Cmd {
        int run(int a, int b, int c, int d) {
            int r = 30;
            if (a > 30) r += 1;
            if (b > 60) r += 2;
            if (c < -30) r += 4;
            if (d == 35) r += 8;
            return r;
        }
    }

    static class C31 extends Cmd {
        int run(int a, int b, int c, int d) {
            target(a, b);
            return 0;
        }
    }

    static final Cmd[] TABLE = {new C0(), new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), new C7(), new C8(), new C9(), new C10(), new C11(), new C12(), new C13(), new C14(), new C15(), new C16(), new C17(), new C18(), new C19(), new C20(), new C21(), new C22(), new C23(), new C24(), new C25(), new C26(), new C27(), new C28(), new C29(), new C30(), new C31()};

    public static void main(String[] args) {
        int cmd = Verifier.nondetInt();
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int c = Verifier.nondetInt();
        int d = Verifier.nondetInt();
        dispatch(cmd, a, b, c, d);
    }

    static int dispatch(int cmd, int a, int b, int c, int d) {
        if (cmd < 0 || cmd >= TABLE.length) return -1;
        return TABLE[cmd].run(a, b, c, d);
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
