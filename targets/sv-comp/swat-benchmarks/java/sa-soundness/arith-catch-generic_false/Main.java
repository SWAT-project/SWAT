// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// The division's ArithmeticException is caught as RuntimeException one frame up.
public class Main {
    static int div(int x, int y) {
        return x / y;
    }

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int r;
        try {
            r = div(100, b - 7);
            if (r > 3) r--;
        } catch (RuntimeException e) {
            if (a > 1000 && a < 100000) assert false;
            r = 0;
        }
        if (r > 50) r--;
    }
}
