// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:5 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// The region throws to a handler in the caller, which skips a write the assert's guard reads.
public class Main {
    static int done;

    static void work(int val) {
        for (int i = 0; i < 10; i++) {
            if (i == val - 5) {
                throw new IllegalStateException("stop");
            }
        }
        done = 1;
    }

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        try {
            work(val);
        } catch (IllegalStateException e) {
            System.out.println("caught");
        }
        if (done == 0) {
            if (b > 1000 && b < 1001) assert false;
        }
    }
}
