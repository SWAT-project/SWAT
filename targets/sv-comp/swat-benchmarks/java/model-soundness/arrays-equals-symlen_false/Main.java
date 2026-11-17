// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:2
// Arrays.equals with an array of symbolic length (not modelled: stays a context loss).
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        int n = Verifier.nondetInt();
        if (n < 0 || n > 3) return;
        int[] a = new int[n];
        int[] b = new int[2];
        if (Arrays.equals(a, b)) assert false;
    }
}
