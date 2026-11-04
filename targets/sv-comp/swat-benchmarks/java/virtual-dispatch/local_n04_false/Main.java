// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:3 int:1001 int:2996 int:0 int:0
import org.sosy_lab.sv_benchmarks.Verifier;

// virtual-dispatch/local: cmd selects one of 4 subclasses, only C3.run() reaches the assert.
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
            default: return -1;
        }
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
