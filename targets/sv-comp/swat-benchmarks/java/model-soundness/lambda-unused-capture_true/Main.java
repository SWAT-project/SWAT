// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an integer x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1001 int:7
import java.util.function.IntSupplier;
import org.sosy_lab.sv_benchmarks.Verifier;
// A lambda captures an input but is never called (the NotificationCenter shape); the assert is outside it.
public class Main {
    static IntSupplier make(int v) {
        return () -> v + 1;
    }

    public static void main(String[] args) {
        int x = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        IntSupplier unused = make(x);
        if (b == 7 && x > 1000 && x < 1001) assert false;
    }
}
