// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): int:69273666
// For 0 < x < 69273635, 962 + 31 * x <= 2147483616 does not overflow, so the hash stays
// positive.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        int x = Verifier.nondetInt();
        int h = Arrays.hashCode(new int[] {x, 1});
        if (x > 0 && x < 69273635 && h < 0) assert false;
    }
}
