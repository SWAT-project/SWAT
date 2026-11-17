// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): long:4294967296
// v with (int) v == 0 and v >>> 32 == 1 is 2^32, whose hash is 31 + 1 = 32, not 31.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        long v = Verifier.nondetLong();
        int h = Arrays.hashCode(new long[] {v});
        if (h == 31 && (int) v == 0 && (v >>> 32) == 1) assert false;
    }
}
