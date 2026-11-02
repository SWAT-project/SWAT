// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:-4 int:3 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// A counting loop from an input start to an input bound.
public class Main {
    public static void main(String[] args) {
        int lo = Verifier.nondetInt();
        int hi = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        if (hi - lo > 50 || lo < -50) return;
        int i = lo;
        int n = 0;
        while (i < hi) {
            i++;
            n++;
        }
        if (n == 7 && i == 3) {
            if (b > 1000 && b < 100000) assert false;
        }
    }
}
