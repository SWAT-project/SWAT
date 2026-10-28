// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:2996 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0
import org.sosy_lab.sv_benchmarks.Verifier;

// error-handling/bounds: 32 steps that can fail with ArrayIndexOutOfBoundsException; each catch block logs and returns.
public class Main {
    static final int[] TABLE = {3, 1, 4, 1};

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
        } catch (ArrayIndexOutOfBoundsException e) {
            log(0, x0);
            return;
        }
        try {
            acc += step1(x1);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(1, x1);
            return;
        }
        try {
            acc += step2(x2);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(2, x2);
            return;
        }
        try {
            acc += step3(x3);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(3, x3);
            return;
        }
        try {
            acc += step4(x4);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(4, x4);
            return;
        }
        try {
            acc += step5(x5);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(5, x5);
            return;
        }
        try {
            acc += step6(x6);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(6, x6);
            return;
        }
        try {
            acc += step7(x7);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(7, x7);
            return;
        }
        try {
            acc += step8(x8);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(8, x8);
            return;
        }
        try {
            acc += step9(x9);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(9, x9);
            return;
        }
        try {
            acc += step10(x10);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(10, x10);
            return;
        }
        try {
            acc += step11(x11);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(11, x11);
            return;
        }
        try {
            acc += step12(x12);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(12, x12);
            return;
        }
        try {
            acc += step13(x13);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(13, x13);
            return;
        }
        try {
            acc += step14(x14);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(14, x14);
            return;
        }
        try {
            acc += step15(x15);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(15, x15);
            return;
        }
        try {
            acc += step16(x16);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(16, x16);
            return;
        }
        try {
            acc += step17(x17);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(17, x17);
            return;
        }
        try {
            acc += step18(x18);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(18, x18);
            return;
        }
        try {
            acc += step19(x19);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(19, x19);
            return;
        }
        try {
            acc += step20(x20);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(20, x20);
            return;
        }
        try {
            acc += step21(x21);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(21, x21);
            return;
        }
        try {
            acc += step22(x22);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(22, x22);
            return;
        }
        try {
            acc += step23(x23);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(23, x23);
            return;
        }
        try {
            acc += step24(x24);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(24, x24);
            return;
        }
        try {
            acc += step25(x25);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(25, x25);
            return;
        }
        try {
            acc += step26(x26);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(26, x26);
            return;
        }
        try {
            acc += step27(x27);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(27, x27);
            return;
        }
        try {
            acc += step28(x28);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(28, x28);
            return;
        }
        try {
            acc += step29(x29);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(29, x29);
            return;
        }
        try {
            acc += step30(x30);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(30, x30);
            return;
        }
        try {
            acc += step31(x31);
        } catch (ArrayIndexOutOfBoundsException e) {
            log(31, x31);
            return;
        }
        target(a, b);
    }

    static int step0(int x) {
        return TABLE[x] + 0;
    }
    static int step1(int x) {
        return TABLE[x] + 1;
    }
    static int step2(int x) {
        return TABLE[x] + 2;
    }
    static int step3(int x) {
        return TABLE[x] + 3;
    }
    static int step4(int x) {
        return TABLE[x] + 4;
    }
    static int step5(int x) {
        return TABLE[x] + 5;
    }
    static int step6(int x) {
        return TABLE[x] + 6;
    }
    static int step7(int x) {
        return TABLE[x] + 7;
    }
    static int step8(int x) {
        return TABLE[x] + 8;
    }
    static int step9(int x) {
        return TABLE[x] + 9;
    }
    static int step10(int x) {
        return TABLE[x] + 10;
    }
    static int step11(int x) {
        return TABLE[x] + 11;
    }
    static int step12(int x) {
        return TABLE[x] + 12;
    }
    static int step13(int x) {
        return TABLE[x] + 13;
    }
    static int step14(int x) {
        return TABLE[x] + 14;
    }
    static int step15(int x) {
        return TABLE[x] + 15;
    }
    static int step16(int x) {
        return TABLE[x] + 16;
    }
    static int step17(int x) {
        return TABLE[x] + 17;
    }
    static int step18(int x) {
        return TABLE[x] + 18;
    }
    static int step19(int x) {
        return TABLE[x] + 19;
    }
    static int step20(int x) {
        return TABLE[x] + 20;
    }
    static int step21(int x) {
        return TABLE[x] + 21;
    }
    static int step22(int x) {
        return TABLE[x] + 22;
    }
    static int step23(int x) {
        return TABLE[x] + 23;
    }
    static int step24(int x) {
        return TABLE[x] + 24;
    }
    static int step25(int x) {
        return TABLE[x] + 25;
    }
    static int step26(int x) {
        return TABLE[x] + 26;
    }
    static int step27(int x) {
        return TABLE[x] + 27;
    }
    static int step28(int x) {
        return TABLE[x] + 28;
    }
    static int step29(int x) {
        return TABLE[x] + 29;
    }
    static int step30(int x) {
        return TABLE[x] + 30;
    }
    static int step31(int x) {
        return TABLE[x] + 31;
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
