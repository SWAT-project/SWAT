// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:3 int:1001
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
            if (b > 1000 && b < 100000) assert false;
        }
    }
}
