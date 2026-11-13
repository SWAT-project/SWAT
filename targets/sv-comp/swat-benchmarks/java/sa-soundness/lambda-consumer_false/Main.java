// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import java.util.function.IntConsumer;
import org.sosy_lab.sv_benchmarks.Verifier;
// The assert is inside a lambda created in main and called on an input-dependent path.
public class Main {
    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        IntConsumer check = x -> {
            if (x > 1000 && x < 100000) assert false;
        };
        if (b == 7) check.accept(a);
        if (a > 3) a--;
    }
}
