// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:10
import org.sosy_lab.sv_benchmarks.Verifier;
// loop-summary/count-down: a loop counting an input down to zero.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int i = val;
        int c = 0;
        while (i > 0) {
            i--;
            c += 2;
        }
        if (c == 20) assert false;
    }
}
