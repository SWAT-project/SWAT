// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:69273666
// Arrays.hashCode(int[]) = 31 * (31 + x) + 1 wraps around: x = 69273666 > 0 gives a
// negative hash (an unbounded-integer model would say it cannot).
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        int x = Verifier.nondetInt();
        int h = Arrays.hashCode(new int[] {x, 1});
        if (x > 0 && h < 0) assert false;
    }
}
