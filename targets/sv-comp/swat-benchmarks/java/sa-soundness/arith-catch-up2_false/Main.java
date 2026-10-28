// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// The assert is in a catch (ArithmeticException) two frames above the division.
public class Main {
    static int div(int x, int y) {
        int q = x / y;
        if (q > 10) q--;
        return q;
    }

    static int mid(int x, int y) {
        int r = div(x, y);
        if (r < 0) r = -r;
        return r;
    }

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int r;
        try {
            r = mid(100, b - 7);
        } catch (ArithmeticException e) {
            if (a > 1000 && a < 100000) assert false;
            r = 0;
        }
        if (r > 50) r--;
    }
}
