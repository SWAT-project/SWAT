// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:3 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// The summarized loop's accumulator must wrap around like Java's int.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        if (val > 100) return;
        int i = 0;
        int s = 0;
        while (i < val) {
            i++;
            s += 1000000000;
        }
        if (s < 0) {
            if (b > 1000 && b < 1001) assert false;
        }
    }
}
