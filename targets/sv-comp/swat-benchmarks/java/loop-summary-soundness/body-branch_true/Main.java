// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:5 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// A counting loop with a branch in its body.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int i = 0;
        int s = 0;
        while (i < val) {
            i++;
            if (i == 3) {
                s += 10;
            }
            s += 1;
        }
        if (s == 15) {
            if (b > 1000 && b < 1001) assert false;
        }
    }
}
