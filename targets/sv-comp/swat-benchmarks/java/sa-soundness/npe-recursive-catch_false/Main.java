// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// A recursive method dereferences null at depth 3 (b == 7); only the activation at depth 2 has a
// handler around its recursive call, with the assert. Resuming the walk must pick that frame.
public class Main {
    static class Box {
        int v = 3;
    }

    static int rec(int depth, int a, int b) {
        if (depth == 3) {
            Box x = b == 7 ? null : new Box();
            return x.v;
        }
        if (depth == 2) {
            try {
                return rec(depth + 1, a, b);
            } catch (NullPointerException e) {
                if (a > 1000 && a < 100000) assert false;
                return 0;
            }
        }
        return rec(depth + 1, a, b) + 1;
    }

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int r = rec(0, a, b);
        if (r > 50) r--;
    }
}
