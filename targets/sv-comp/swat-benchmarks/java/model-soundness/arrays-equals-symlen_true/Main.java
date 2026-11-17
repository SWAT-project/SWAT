// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): int:2
// Must stay UNKNOWN: Arrays.equals with an array of symbolic length is not modelled. (SAFE:
// equal arrays have equal lengths, so n == 2.)
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        int n = Verifier.nondetInt();
        if (n < 0 || n > 3) return;
        int[] a = new int[n];
        int[] b = new int[2];
        if (Arrays.equals(a, b) && n != 2) assert false;
    }
}
