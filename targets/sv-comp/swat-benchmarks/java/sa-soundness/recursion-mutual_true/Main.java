// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1001 int:5
import org.sosy_lab.sv_benchmarks.Verifier;
// Mutual recursion; the assert is only reachable at an odd depth b > 3 (b = 5).
public class Main {
    static void even(int depth, int a, int b) {
        if (depth < 8) odd(depth + 1, a, b);
    }

    static void odd(int depth, int a, int b) {
        if (depth == b && depth > 3) {
            if (a > 1000 && a < 1001) assert false;
            return;
        }
        if (depth < 8) even(depth + 1, a, b);
    }

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        even(0, a, b);
    }
}
