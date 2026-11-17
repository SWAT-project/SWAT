// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1000 int:1 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// An instance-capturing lambda (receiver captured first) reads a field and a captured local.
public class Main {
    int f;

    void check(int y) {
        Runnable r = () -> {
            int x = f + y;
            if (x > 1000 && x < 100000) assert false;
        };
        r.run();
    }

    public static void main(String[] args) {
        Main m = new Main();
        m.f = Verifier.nondetInt();
        int y = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        if (b == 7) m.check(y);
    }
}
