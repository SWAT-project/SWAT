// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:3141592
import org.sosy_lab.sv_benchmarks.Verifier;
// relevance/example: the loop's branches all lead to the assert either way, so reachability
// pruning keeps them; none of them can change val, so slicing does not flip them.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        for (int i = 0; i < 100000; i++) {
            if (i < val) {
                System.out.println(i);
            }
        }
        assert val != 3141592;
    }
}
