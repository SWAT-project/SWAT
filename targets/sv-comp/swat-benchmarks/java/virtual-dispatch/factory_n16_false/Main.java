// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:15 int:1001 int:2996 int:0 int:0
import org.sosy_lab.sv_benchmarks.Verifier;

// virtual-dispatch/factory: cmd selects one of 16 subclasses, only C15.run() reaches the assert.
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
            target(a, b);
            return 0;
        }
    }

    static Cmd make(int cmd) {
        switch (cmd) {
            case 0: return new C0();
            case 1: return new C1();
            case 2: return new C2();
            case 3: return new C3();
            case 4: return new C4();
            case 5: return new C5();
            case 6: return new C6();
            case 7: return new C7();
            case 8: return new C8();
            case 9: return new C9();
            case 10: return new C10();
            case 11: return new C11();
            case 12: return new C12();
            case 13: return new C13();
            case 14: return new C14();
            case 15: return new C15();
            default: return null;
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
        Cmd command = make(cmd);
        if (command == null) return -1;
        return command.run(a, b, c, d);
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
