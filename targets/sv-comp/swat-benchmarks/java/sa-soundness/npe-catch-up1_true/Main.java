// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// The assert is in a catch (NullPointerException) one frame above the dereference.
public class Main {
    static class Box {
        int v = 3;
    }

    static int read(Box box) {
        int v = box.v;
        if (v > 2) v--;
        return v;
    }

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        Box box = b == 7 ? null : new Box();
        int r;
        try {
            r = read(box);
        } catch (NullPointerException e) {
            if (a > 1000 && a < 1001) assert false;
            r = 0;
        }
        if (r > 50) r--;
    }
}
