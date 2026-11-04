// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:2996 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0
import org.sosy_lab.sv_benchmarks.Verifier;

// error-handling/deep: 32 steps that can fail with IllegalArgumentException; each catch block logs and returns.
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
        int x16 = Verifier.nondetInt();
        int x17 = Verifier.nondetInt();
        int x18 = Verifier.nondetInt();
        int x19 = Verifier.nondetInt();
        int x20 = Verifier.nondetInt();
        int x21 = Verifier.nondetInt();
        int x22 = Verifier.nondetInt();
        int x23 = Verifier.nondetInt();
        int x24 = Verifier.nondetInt();
        int x25 = Verifier.nondetInt();
        int x26 = Verifier.nondetInt();
        int x27 = Verifier.nondetInt();
        int x28 = Verifier.nondetInt();
        int x29 = Verifier.nondetInt();
        int x30 = Verifier.nondetInt();
        int x31 = Verifier.nondetInt();
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
        try {
            acc += step16(x16);
        } catch (IllegalArgumentException e) {
            log(16, x16);
            return;
        }
        try {
            acc += step17(x17);
        } catch (IllegalArgumentException e) {
            log(17, x17);
            return;
        }
        try {
            acc += step18(x18);
        } catch (IllegalArgumentException e) {
            log(18, x18);
            return;
        }
        try {
            acc += step19(x19);
        } catch (IllegalArgumentException e) {
            log(19, x19);
            return;
        }
        try {
            acc += step20(x20);
        } catch (IllegalArgumentException e) {
            log(20, x20);
            return;
        }
        try {
            acc += step21(x21);
        } catch (IllegalArgumentException e) {
            log(21, x21);
            return;
        }
        try {
            acc += step22(x22);
        } catch (IllegalArgumentException e) {
            log(22, x22);
            return;
        }
        try {
            acc += step23(x23);
        } catch (IllegalArgumentException e) {
            log(23, x23);
            return;
        }
        try {
            acc += step24(x24);
        } catch (IllegalArgumentException e) {
            log(24, x24);
            return;
        }
        try {
            acc += step25(x25);
        } catch (IllegalArgumentException e) {
            log(25, x25);
            return;
        }
        try {
            acc += step26(x26);
        } catch (IllegalArgumentException e) {
            log(26, x26);
            return;
        }
        try {
            acc += step27(x27);
        } catch (IllegalArgumentException e) {
            log(27, x27);
            return;
        }
        try {
            acc += step28(x28);
        } catch (IllegalArgumentException e) {
            log(28, x28);
            return;
        }
        try {
            acc += step29(x29);
        } catch (IllegalArgumentException e) {
            log(29, x29);
            return;
        }
        try {
            acc += step30(x30);
        } catch (IllegalArgumentException e) {
            log(30, x30);
            return;
        }
        try {
            acc += step31(x31);
        } catch (IllegalArgumentException e) {
            log(31, x31);
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
    static int step16(int x) {
        return check16(x) + 16;
    }

    static int check16(int x) {
        if (x < -17) throw new IllegalArgumentException();
        return x;
    }
    static int step17(int x) {
        return check17(x) + 17;
    }

    static int check17(int x) {
        if (x < -18) throw new IllegalArgumentException();
        return x;
    }
    static int step18(int x) {
        return check18(x) + 18;
    }

    static int check18(int x) {
        if (x < -19) throw new IllegalArgumentException();
        return x;
    }
    static int step19(int x) {
        return check19(x) + 19;
    }

    static int check19(int x) {
        if (x < -20) throw new IllegalArgumentException();
        return x;
    }
    static int step20(int x) {
        return check20(x) + 20;
    }

    static int check20(int x) {
        if (x < -21) throw new IllegalArgumentException();
        return x;
    }
    static int step21(int x) {
        return check21(x) + 21;
    }

    static int check21(int x) {
        if (x < -22) throw new IllegalArgumentException();
        return x;
    }
    static int step22(int x) {
        return check22(x) + 22;
    }

    static int check22(int x) {
        if (x < -23) throw new IllegalArgumentException();
        return x;
    }
    static int step23(int x) {
        return check23(x) + 23;
    }

    static int check23(int x) {
        if (x < -24) throw new IllegalArgumentException();
        return x;
    }
    static int step24(int x) {
        return check24(x) + 24;
    }

    static int check24(int x) {
        if (x < -25) throw new IllegalArgumentException();
        return x;
    }
    static int step25(int x) {
        return check25(x) + 25;
    }

    static int check25(int x) {
        if (x < -26) throw new IllegalArgumentException();
        return x;
    }
    static int step26(int x) {
        return check26(x) + 26;
    }

    static int check26(int x) {
        if (x < -27) throw new IllegalArgumentException();
        return x;
    }
    static int step27(int x) {
        return check27(x) + 27;
    }

    static int check27(int x) {
        if (x < -28) throw new IllegalArgumentException();
        return x;
    }
    static int step28(int x) {
        return check28(x) + 28;
    }

    static int check28(int x) {
        if (x < -29) throw new IllegalArgumentException();
        return x;
    }
    static int step29(int x) {
        return check29(x) + 29;
    }

    static int check29(int x) {
        if (x < -30) throw new IllegalArgumentException();
        return x;
    }
    static int step30(int x) {
        return check30(x) + 30;
    }

    static int check30(int x) {
        if (x < -31) throw new IllegalArgumentException();
        return x;
    }
    static int step31(int x) {
        return check31(x) + 31;
    }

    static int check31(int x) {
        if (x < -32) throw new IllegalArgumentException();
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
                if (b > a) {
                    assert false;
                }
            }
        }
    }
}
