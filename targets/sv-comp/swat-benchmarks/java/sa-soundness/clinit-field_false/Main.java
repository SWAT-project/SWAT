// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// The assert is in the static initializer of a class first used (static field read) on an
// input-dependent path.
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
    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        input = a;
        int r = 0;
        if (b == 7) {
            r = Late.K;
        }
        if (r > 3) r--;
    }
}
