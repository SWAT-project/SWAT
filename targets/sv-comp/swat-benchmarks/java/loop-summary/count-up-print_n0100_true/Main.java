// Expected verdict: SAFE. s = 3 * max(val, 0) with val <= 2^31 - 1, which is never 3n + 1 (mod 2^32).
// Paired VIOLATION inputs (must not fail here): int:100
import org.sosy_lab.sv_benchmarks.Verifier;
// loop-summary/count-up-print: a counting loop up to an input bound that also prints.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int i = 0;
        int s = 0;
        while (i < val) {
            i++;
            s += 3;
            System.out.println(i);
        }
        if (s == 301) assert false;
    }
}
