// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:3 int:1001
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
            if (b > 1000 && b < 1001) assert false;
        }
    }
}
