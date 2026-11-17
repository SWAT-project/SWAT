// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an integer x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): long:1001 double:2.5 int:7
import java.util.function.LongSupplier;
import org.sosy_lab.sv_benchmarks.Verifier;
// A lambda captures a long and a double (two-slot values) besides an int.
public class Main {
    public static void main(String[] args) {
        long l = Verifier.nondetLong();
        double d = Verifier.nondetDouble();
        int b = Verifier.nondetInt();
        LongSupplier s = () -> (d > 2.0 && b == 7) ? l : 0L;
        long x = s.getAsLong();
        if (x > 1000 && x < 1001) assert false;
    }
}
