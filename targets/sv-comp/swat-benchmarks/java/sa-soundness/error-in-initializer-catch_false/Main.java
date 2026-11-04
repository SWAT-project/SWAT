// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// A static initializer throws on b == 7; the ExceptionInInitializerError (an Error) is caught as
// Error in main, where the assert is.
public class Main {
    static int input;

    static class Late {
        static int K;

        static {
            if (Main.input == 7) throw new IllegalStateException();
            K = 1;
        }
    }

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        input = b;
        int r;
        try {
            r = Late.K;
        } catch (Error e) {
            if (a > 1000 && a < 100000) assert false;
            r = 0;
        }
        if (r > 50) r--;
    }
}
