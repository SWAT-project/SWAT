// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:10
import org.sosy_lab.sv_benchmarks.Verifier;
// loop-summary/count-up: a counting loop up to an input bound.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int i = 0;
        int s = 0;
        while (i < val) {
            i++;
            s += 3;
        }
        if (s == 30) assert false;
    }
}
