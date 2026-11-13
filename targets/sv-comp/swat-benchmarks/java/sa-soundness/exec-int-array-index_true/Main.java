// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1001 int:2
import org.sosy_lab.sv_benchmarks.Verifier;
// Control for exec-object-array-index: the same with an int array.
public class Main {
    static final int[] TABLE = {0, 0, 7};

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        if (b >= 0 && b < TABLE.length) {
            if (TABLE[b] == 7) {
                if (a > 1000 && a < 1001) assert false;
            }
        }
    }
}
