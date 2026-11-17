// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:3 int:7
// Arrays.equals(int[], int[]): equal iff every element is. The SAFE variant differs in a
// concrete element (5 vs 6), so no x, y make the arrays equal.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        int x = Verifier.nondetInt();
        int y = Verifier.nondetInt();
        int[] a = {x, 5, 7};
        int[] b = {3, 5, y};
        if (Arrays.equals(a, b)) assert false;
    }
}
