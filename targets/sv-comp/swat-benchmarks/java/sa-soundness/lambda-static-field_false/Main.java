// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import java.util.function.IntConsumer;
import org.sosy_lab.sv_benchmarks.Verifier;
// The assert is inside a lambda stored in a static field (created in <clinit>) and called on an
// input-dependent path.
public class Main {
    static final IntConsumer CHECK = x -> {
        if (x > 1000 && x < 100000) assert false;
    };

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        if (b == 7) CHECK.accept(a);
        if (a > 3) a--;
    }
}
