// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// relevance/scratch-array: the loop fills a scratch array (bounds-checked) the assert never reads.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int[] hist = new int[100];
        for (int i = 0; i < 100; i++) {
            if (i < val) {
                hist[i] = i;
            } else {
                hist[i] = -i;
            }
        }
        System.out.println(hist.length);
        if (val > 1000 && val < 1001) assert false;
    }
}
