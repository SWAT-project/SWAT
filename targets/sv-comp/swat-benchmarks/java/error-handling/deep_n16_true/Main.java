// Expected verdict: SAFE. target() needs a > 1000, b == 3 * a - 7 and b < a, i.e. a < 3.5: unsatisfiable. a < 100000 rules out overflow in 3 * a.
// Paired VIOLATION inputs (must not fail here): int:1001 int:2996 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0
import org.sosy_lab.sv_benchmarks.Verifier;

// error-handling/deep: 16 steps that can fail with IllegalArgumentException; each catch block logs and returns.
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
        try {
            acc += step4(x4);
        } catch (IllegalArgumentException e) {
            log(4, x4);
            return;
        }
        try {
            acc += step5(x5);
        } catch (IllegalArgumentException e) {
            log(5, x5);
            return;
        }
        try {
            acc += step6(x6);
        } catch (IllegalArgumentException e) {
            log(6, x6);
            return;
        }
        try {
            acc += step7(x7);
        } catch (IllegalArgumentException e) {
            log(7, x7);
            return;
        }
        try {
            acc += step8(x8);
        } catch (IllegalArgumentException e) {
            log(8, x8);
            return;
        }
        try {
            acc += step9(x9);
        } catch (IllegalArgumentException e) {
            log(9, x9);
            return;
        }
        try {
            acc += step10(x10);
        } catch (IllegalArgumentException e) {
            log(10, x10);
            return;
        }
        try {
            acc += step11(x11);
        } catch (IllegalArgumentException e) {
            log(11, x11);
            return;
        }
        try {
            acc += step12(x12);
        } catch (IllegalArgumentException e) {
            log(12, x12);
            return;
        }
        try {
            acc += step13(x13);
        } catch (IllegalArgumentException e) {
            log(13, x13);
            return;
        }
        try {
            acc += step14(x14);
        } catch (IllegalArgumentException e) {
            log(14, x14);
            return;
        }
        try {
            acc += step15(x15);
        } catch (IllegalArgumentException e) {
            log(15, x15);
            return;
        }
        target(a, b);
    }

    static int step0(int x) {
        return check0(x) + 0;
    }

    static int check0(int x) {
        if (x < -1) throw new IllegalArgumentException();
        return x;
    }
    static int step1(int x) {
        return check1(x) + 1;
    }

    static int check1(int x) {
        if (x < -2) throw new IllegalArgumentException();
        return x;
    }
    static int step2(int x) {
        return check2(x) + 2;
    }

    static int check2(int x) {
        if (x < -3) throw new IllegalArgumentException();
        return x;
    }
    static int step3(int x) {
        return check3(x) + 3;
    }

    static int check3(int x) {
        if (x < -4) throw new IllegalArgumentException();
        return x;
    }
    static int step4(int x) {
        return check4(x) + 4;
    }

    static int check4(int x) {
        if (x < -5) throw new IllegalArgumentException();
        return x;
    }
    static int step5(int x) {
        return check5(x) + 5;
    }

    static int check5(int x) {
        if (x < -6) throw new IllegalArgumentException();
        return x;
    }
    static int step6(int x) {
        return check6(x) + 6;
    }

    static int check6(int x) {
        if (x < -7) throw new IllegalArgumentException();
        return x;
    }
    static int step7(int x) {
        return check7(x) + 7;
    }

    static int check7(int x) {
        if (x < -8) throw new IllegalArgumentException();
        return x;
    }
    static int step8(int x) {
        return check8(x) + 8;
    }

    static int check8(int x) {
        if (x < -9) throw new IllegalArgumentException();
        return x;
    }
    static int step9(int x) {
        return check9(x) + 9;
    }

    static int check9(int x) {
        if (x < -10) throw new IllegalArgumentException();
        return x;
    }
    static int step10(int x) {
        return check10(x) + 10;
    }

    static int check10(int x) {
        if (x < -11) throw new IllegalArgumentException();
        return x;
    }
    static int step11(int x) {
        return check11(x) + 11;
    }

    static int check11(int x) {
        if (x < -12) throw new IllegalArgumentException();
        return x;
    }
    static int step12(int x) {
        return check12(x) + 12;
    }

    static int check12(int x) {
        if (x < -13) throw new IllegalArgumentException();
        return x;
    }
    static int step13(int x) {
        return check13(x) + 13;
    }

    static int check13(int x) {
        if (x < -14) throw new IllegalArgumentException();
        return x;
    }
    static int step14(int x) {
        return check14(x) + 14;
    }

    static int check14(int x) {
        if (x < -15) throw new IllegalArgumentException();
        return x;
    }
    static int step15(int x) {
        return check15(x) + 15;
    }

    static int check15(int x) {
        if (x < -16) throw new IllegalArgumentException();
        return x;
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
