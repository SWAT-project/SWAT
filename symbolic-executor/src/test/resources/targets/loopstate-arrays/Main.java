import org.sosy_lab.sv_benchmarks.Verifier;

/**
 * Loop-state matching with arrays on the heap (LoopStateMatcherArraysAgentSpec). The loop reads a
 * constant table; the first input decides what happens to a scratch array before it: nothing (0),
 * an input stored in it (1), a store at an input index (2), or a write by untracked JDK code (3).
 */
public class Main {
    static final int[] TABLE = {3, 1, 4, 1, 5};

    public static void main(String[] args) {
        int mode = Verifier.nondetInt();
        int[] scratch = new int[4];
        scratch[1] = 9;
        if (mode == 1) {
            scratch[0] = Verifier.nondetInt();
        } else if (mode == 2) {
            int i = Verifier.nondetInt();
            if (i >= 0 && i < scratch.length) {
                scratch[i] = 7;
            }
        } else if (mode == 3) {
            java.util.Arrays.fill(scratch, 2);
        }
        int phase = 0;
        while (Verifier.nondetBoolean()) {
            phase = (phase + TABLE[phase]) % TABLE.length;
        }
        System.out.println("phase " + phase + " " + scratch[1]);
    }
}
