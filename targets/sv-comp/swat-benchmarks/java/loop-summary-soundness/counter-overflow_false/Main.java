// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:3 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// The merged counter must wrap around like Java's int: three steps of 10^9 are negative.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int count = 0;
        for (int i = 0; i < 5; i++) {
            if (i < val) {
                count += 1000000000;
            }
        }
        if (count < 0) {
            if (b > 1000 && b < 100000) assert false;
        }
    }
}
