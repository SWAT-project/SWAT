// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:7 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// The loop's branch counts, and the count decides whether the assert is reached: relevant.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int count = 0;
        for (int i = 0; i < 10; i++) {
            if (i < val) {
                count++;
            }
        }
        if (count == 7) {
            if (b > 1000 && b < 1001) assert false;
        }
    }
}
