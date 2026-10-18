// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1001 int:4
import org.sosy_lab.sv_benchmarks.Verifier;
// The assert is at an input-dependent recursion depth (b = 4).
public class Main {
    static void rec(int depth, int a, int b) {
        if (depth == b && depth > 3) {
            if (a > 1000 && a < 1001) assert false;
            return;
        }
        if (depth < 6) rec(depth + 1, a, b);
    }

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        rec(0, a, b);
    }
}
