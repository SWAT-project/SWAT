// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// Whether the callee dereferences null is decided by a branch inside the callee; the
// NullPointerException is caught in main, where the assert is.
public class Main {
    static class Box {
        int v = 3;
    }

    static int read(Box box, int b) {
        Box x = b == 7 ? null : box;
        int v = x.v;
        if (v > 2) v--;
        return v;
    }

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int r;
        try {
            r = read(new Box(), b);
        } catch (NullPointerException e) {
            if (a > 1000 && a < 1001) assert false;
            r = 0;
        }
        if (r > 50) r--;
    }
}
