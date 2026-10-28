// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:2996 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0
import org.sosy_lab.sv_benchmarks.Verifier;

// error-handling/arith: 16 steps that can fail with ArithmeticException; each catch block logs and returns.
public class Main {
    static int logged;

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int x0 = Verifier.nondetInt();
        int x1 = Verifier.nondetInt();
        int x2 = Verifier.nondetInt();
        int x3 = Verifier.nondetInt();
        int x4 = Verifier.nondetInt();
        int x5 = Verifier.nondetInt();
        int x6 = Verifier.nondetInt();
        int x7 = Verifier.nondetInt();
        int x8 = Verifier.nondetInt();
        int x9 = Verifier.nondetInt();
        int x10 = Verifier.nondetInt();
        int x11 = Verifier.nondetInt();
        int x12 = Verifier.nondetInt();
        int x13 = Verifier.nondetInt();
        int x14 = Verifier.nondetInt();
        int x15 = Verifier.nondetInt();
        int acc = 0;
        try {
            acc += step0(x0);
        } catch (ArithmeticException e) {
            log(0, x0);
            return;
        }
        try {
            acc += step1(x1);
        } catch (ArithmeticException e) {
            log(1, x1);
            return;
        }
        try {
            acc += step2(x2);
        } catch (ArithmeticException e) {
            log(2, x2);
            return;
        }
        try {
            acc += step3(x3);
        } catch (ArithmeticException e) {
            log(3, x3);
            return;
        }
        try {
            acc += step4(x4);
        } catch (ArithmeticException e) {
            log(4, x4);
            return;
        }
        try {
            acc += step5(x5);
        } catch (ArithmeticException e) {
            log(5, x5);
            return;
        }
        try {
            acc += step6(x6);
        } catch (ArithmeticException e) {
            log(6, x6);
            return;
        }
        try {
            acc += step7(x7);
        } catch (ArithmeticException e) {
            log(7, x7);
            return;
        }
        try {
            acc += step8(x8);
        } catch (ArithmeticException e) {
            log(8, x8);
            return;
        }
        try {
            acc += step9(x9);
        } catch (ArithmeticException e) {
            log(9, x9);
            return;
        }
        try {
            acc += step10(x10);
        } catch (ArithmeticException e) {
            log(10, x10);
            return;
        }
        try {
            acc += step11(x11);
        } catch (ArithmeticException e) {
            log(11, x11);
            return;
        }
        try {
            acc += step12(x12);
        } catch (ArithmeticException e) {
            log(12, x12);
            return;
        }
        try {
            acc += step13(x13);
        } catch (ArithmeticException e) {
            log(13, x13);
            return;
        }
        try {
            acc += step14(x14);
        } catch (ArithmeticException e) {
            log(14, x14);
            return;
        }
        try {
            acc += step15(x15);
        } catch (ArithmeticException e) {
            log(15, x15);
            return;
        }
        target(a, b);
    }

    static int step0(int x) {
        return 100 / (x - 0 - 1);
    }
    static int step1(int x) {
        return 100 / (x - 1 - 1);
    }
    static int step2(int x) {
        return 100 / (x - 2 - 1);
    }
    static int step3(int x) {
        return 100 / (x - 3 - 1);
    }
    static int step4(int x) {
        return 100 / (x - 4 - 1);
    }
    static int step5(int x) {
        return 100 / (x - 5 - 1);
    }
    static int step6(int x) {
        return 100 / (x - 6 - 1);
    }
    static int step7(int x) {
        return 100 / (x - 7 - 1);
    }
    static int step8(int x) {
        return 100 / (x - 8 - 1);
    }
    static int step9(int x) {
        return 100 / (x - 9 - 1);
    }
    static int step10(int x) {
        return 100 / (x - 10 - 1);
    }
    static int step11(int x) {
        return 100 / (x - 11 - 1);
    }
    static int step12(int x) {
        return 100 / (x - 12 - 1);
    }
    static int step13(int x) {
        return 100 / (x - 13 - 1);
    }
    static int step14(int x) {
        return 100 / (x - 14 - 1);
    }
    static int step15(int x) {
        return 100 / (x - 15 - 1);
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
                if (b > a) {
                    assert false;
                }
            }
        }
    }
}
