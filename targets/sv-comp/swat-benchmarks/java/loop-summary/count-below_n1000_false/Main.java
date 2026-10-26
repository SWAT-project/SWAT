// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:997
import org.sosy_lab.sv_benchmarks.Verifier;
// loop-summary/count-below: count = number of i < n with i < val.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int count = 0;
        for (int i = 0; i < 1000; i++) {
            if (i < val) {
                count++;
            }
        }
        if (count == 997) assert false;
    }
}
