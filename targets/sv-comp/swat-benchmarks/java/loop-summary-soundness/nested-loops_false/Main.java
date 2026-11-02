// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:3 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// A counting loop inside a loop: c = 4 * max(val, 0).
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int c = 0;
        for (int k = 0; k < 4; k++) {
            int j = 0;
            while (j < val) {
                j++;
                c++;
            }
        }
        if (c == 12) {
            if (b > 1000 && b < 100000) assert false;
        }
    }
}
