// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// relevance/scratch-array: the loop fills a scratch array (bounds-checked) the assert never reads.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int[] hist = new int[10];
        for (int i = 0; i < 10; i++) {
            if (i < val) {
                hist[i] = i;
            } else {
                hist[i] = -i;
            }
        }
        System.out.println(hist.length);
        if (val > 1000 && val < 100000) assert false;
    }
}
