// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// Control: an explicit throw of IllegalStateException one frame down, caught as RuntimeException.
// The graph models explicit throws, so this one must work.
public class Main {
    static int check(int b) {
        if (b == 7) throw new IllegalStateException();
        return b;
    }

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int r;
        try {
            r = check(b);
        } catch (RuntimeException e) {
            if (a > 1000 && a < 100000) assert false;
            r = 0;
        }
        if (r > 50) r--;
    }
}
