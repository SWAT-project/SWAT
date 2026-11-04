// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1001 int:7
import java.util.function.IntPredicate;
import org.sosy_lab.sv_benchmarks.Verifier;
// The assert condition is evaluated by a lambda (invokedynamic) created in main.
public class Main {
    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        IntPredicate p = x -> x > 1000 && x < 1001;
        if (b == 7) {
            if (p.test(a)) assert false;
        }
    }
}
