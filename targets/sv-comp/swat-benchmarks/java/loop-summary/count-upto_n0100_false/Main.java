// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:100
import org.sosy_lab.sv_benchmarks.Verifier;
// loop-summary/count-upto: a counting loop up to and including an input bound.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int i = 1;
        int s = 0;
        while (i <= val) {
            i++;
            s += 2;
        }
        if (s == 200) assert false;
    }
}
