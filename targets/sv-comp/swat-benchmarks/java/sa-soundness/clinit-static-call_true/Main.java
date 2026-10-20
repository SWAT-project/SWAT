// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// The assert is in the static initializer of a class first used (static call) on an
// input-dependent path.
public class Main {
    static int input;

    static class Late {
        static int K;

        static {
            if (Main.input > 1000 && Main.input < 1001) assert false;
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
            r = Late.get();
        }
        if (r > 3) r--;
    }
}
