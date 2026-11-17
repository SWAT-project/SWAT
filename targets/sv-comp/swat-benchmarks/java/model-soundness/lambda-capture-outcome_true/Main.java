// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an integer x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1001 int:0 int:7
import java.util.function.IntSupplier;
import org.sosy_lab.sv_benchmarks.Verifier;
// The value a capturing lambda computes from two captured inputs (order matters) decides the assert.
public class Main {
    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int c = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        IntSupplier s = () -> a - 2 * c;
        if (b == 7) {
            int x = s.getAsInt();
            if (x > 1000 && x < 1001) assert false;
        }
    }
}
