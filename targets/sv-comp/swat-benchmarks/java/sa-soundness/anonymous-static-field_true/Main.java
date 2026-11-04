// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// The assert is in an anonymous class stored in a static field (receiver read from a field).
public class Main {
    static int input;

    static final Runnable CHECK = new Runnable() {
        public void run() {
            if (input > 1000 && input < 1001) assert false;
        }
    };

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        input = a;
        if (b == 7) CHECK.run();
        if (a > 3) a--;
    }
}
