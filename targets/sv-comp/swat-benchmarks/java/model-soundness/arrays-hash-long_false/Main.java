// Expected verdict: VIOLATION. Violating inputs (nondet call order): long:4294967296
// Arrays.hashCode(long[]): Long.hashCode(v) = (int) (v ^ (v >>> 32)); v = 2^32 hashes to 1,
// so h == 32 although (int) v == 0 (a model using (int) v would say SAFE).
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        long v = Verifier.nondetLong();
        int h = Arrays.hashCode(new long[] {v});
        if (h == 32 && (int) v == 0) assert false;
    }
}
