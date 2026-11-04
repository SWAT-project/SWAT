// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// The assert is in a catch (NullPointerException) in the method that dereferences.
public class Main {
    static class Box {
        int v = 3;
    }

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        Box box = b == 7 ? null : new Box();
        int r;
        try {
            r = box.v;
            if (r > 2) r--;
        } catch (NullPointerException e) {
            if (a > 1000 && a < 100000) assert false;
            r = 0;
        }
        if (r > 50) r--;
    }
}
