// Expected verdict: SAFE. found is below n.
// Paired VIOLATION inputs (must not fail here): int:998
import org.sosy_lab.sv_benchmarks.Verifier;
// loop-summary/found-index: found = the index equal to val, or -1.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int found = -1;
        for (int i = 0; i < 1000; i++) {
            if (i == val) {
                found = i;
            }
        }
        if (found == 1000) assert false;
    }
}
