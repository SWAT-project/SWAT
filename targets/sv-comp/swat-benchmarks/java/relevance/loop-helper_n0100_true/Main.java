// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// relevance/loop-helper: each iteration calls a helper with input-dependent branches that only
// update a counter the assert does not read.
public class Main {
    static int logged;

    static void log(int i, int v) {
        if (i % 2 == 0) {
            if (i < v) {
                logged++;
            }
        } else if (i > v) {
            logged += 2;
        }
    }

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        for (int i = 0; i < 100; i++) {
            log(i, val);
        }
        if (val > 1000 && val < 1001) assert false;
    }
}
