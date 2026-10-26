// Expected verdict: SAFE. s is even.
// Paired VIOLATION inputs (must not fail here): int:10
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
        if (s == 21) assert false;
    }
}
