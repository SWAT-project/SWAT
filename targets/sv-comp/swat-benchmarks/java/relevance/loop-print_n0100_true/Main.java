// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// relevance/loop-print: an input-dependent branch per iteration that only prints.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        for (int i = 0; i < 100; i++) {
            if (i < val) {
                System.out.println(i);
            }
        }
        if (val > 1000 && val < 1001) assert false;
    }
}
