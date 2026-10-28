// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// The assert is in a static initializer triggered inside a callee that has no branches of its own.
public class Main {
    static int input;

    static class Late {
        static int K;

        static {
            if (Main.input > 1000 && Main.input < 100000) assert false;
            K = 1;
        }

        static int get() {
            return K;
        }
    }
    static int touch() {
        return Late.get() + 1;
    }

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        input = a;
        int r = 0;
        if (b == 7) {
            r = touch();
        }
        if (r > 3) r--;
    }
}
