// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// The assert is in an anonymous class's run(), called on an input-dependent path.
public class Main {
    static int input;

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        input = a;
        Runnable r = new Runnable() {
            public void run() {
                if (input > 1000 && input < 100000) assert false;
            }
        };
        if (b == 7) r.run();
        if (a > 3) a--;
    }
}
