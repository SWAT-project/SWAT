// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// The first run (b = 0) ends in System.exit before the assert.
public class Main {
    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        if (b != 7) {
            System.exit(0);
        }
        if (a > 1000 && a < 1001) assert false;
    }
}
