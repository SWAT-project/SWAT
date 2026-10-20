// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// The assert is in a catch (ArrayIndexOutOfBoundsException) two frames above the array read.
public class Main {
    static final int[] TABLE = {1, 2, 3};

    static int get(int i) {
        int v = TABLE[i];
        if (v > 1) v--;
        return v;
    }

    static int lookup(int i) {
        int v = get(i);
        if (v < 0) v = 0;
        return v;
    }

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int r;
        try {
            r = lookup(b);
        } catch (ArrayIndexOutOfBoundsException e) {
            if (a > 1000 && a < 100000) assert false;
            r = 0;
        }
        if (r > 1) r--;
    }
}
