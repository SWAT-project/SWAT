// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an integer x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1000 int:1 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// An instance-capturing lambda (receiver captured first) reads a field and a captured local.
public class Main {
    int f;

    void check(int y) {
        Runnable r = () -> {
            int x = f + y;
            if (x > 1000 && x < 1001) assert false;
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
