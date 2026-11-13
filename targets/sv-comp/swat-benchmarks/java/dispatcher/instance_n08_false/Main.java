// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:7 int:1001 int:2996 int:0 int:0
import org.sosy_lab.sv_benchmarks.Verifier;

// dispatcher/instance: cmd picks one of 8 commands, only the last one (cmd == 7) can reach the assert.
public class Main {
    int calls;

    public static void main(String[] args) {
        int cmd = Verifier.nondetInt();
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int c = Verifier.nondetInt();
        int d = Verifier.nondetInt();
        Main m = new Main();
        m.calls = dispatch(m, cmd, a, b, c, d);
    }

    static int dispatch(Main m, int cmd, int a, int b, int c, int d) {
        int r;
        switch (cmd) {
            case 0: r = m.handler0(a, b, c, d); break;
            case 1: r = m.handler1(a, b, c, d); break;
            case 2: r = m.handler2(a, b, c, d); break;
            case 3: r = m.handler3(a, b, c, d); break;
            case 4: r = m.handler4(a, b, c, d); break;
            case 5: r = m.handler5(a, b, c, d); break;
            case 6: r = m.handler6(a, b, c, d); break;
            case 7: m.target(a, b); r = 0; break;
            default: r = -1;
        }
        return r;
    }

    int handler0(int a, int b, int c, int d) {
        int r = 0;
        if (a > 0) r += 1;
        if (b > 0) r += 2;
        if (c < 0) r += 4;
        if (d == 5) r += 8;
        return r;
    }
    int handler1(int a, int b, int c, int d) {
        int r = 1;
        if (a > 1) r += 1;
        if (b > 2) r += 2;
        if (c < -1) r += 4;
        if (d == 6) r += 8;
        return r;
    }
    int handler2(int a, int b, int c, int d) {
        int r = 2;
        if (a > 2) r += 1;
        if (b > 4) r += 2;
        if (c < -2) r += 4;
        if (d == 7) r += 8;
        return r;
    }
    int handler3(int a, int b, int c, int d) {
        int r = 3;
        if (a > 3) r += 1;
        if (b > 6) r += 2;
        if (c < -3) r += 4;
        if (d == 8) r += 8;
        return r;
    }
    int handler4(int a, int b, int c, int d) {
        int r = 4;
        if (a > 4) r += 1;
        if (b > 8) r += 2;
        if (c < -4) r += 4;
        if (d == 9) r += 8;
        return r;
    }
    int handler5(int a, int b, int c, int d) {
        int r = 5;
        if (a > 5) r += 1;
        if (b > 10) r += 2;
        if (c < -5) r += 4;
        if (d == 10) r += 8;
        return r;
    }
    int handler6(int a, int b, int c, int d) {
        int r = 6;
        if (a > 6) r += 1;
        if (b > 12) r += 2;
        if (c < -6) r += 4;
        if (d == 11) r += 8;
        return r;
    }

    // The only assertion in the program.
    void target(int a, int b) {
        if (a > 1000 && a < 100000) {
            if (b == 3 * a - 7) {
                if (b > a) {
                    assert false;
                }
            }
        }
    }
}
