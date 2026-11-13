// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// relevance/loop-print: an input-dependent branch per iteration that only prints.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        for (int i = 0; i < 10; i++) {
            if (i < val) {
                System.out.println(i);
            }
        }
        if (val > 1000 && val < 100000) assert false;
    }
}
