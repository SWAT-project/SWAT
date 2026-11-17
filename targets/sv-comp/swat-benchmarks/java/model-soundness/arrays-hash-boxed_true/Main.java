// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): int:5 boolean:true long:4294967296
// With z = false, x = 5 and v = 2^32 fixed the hash is 2261234 (1237 instead of 1231),
// never 2255468.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.Objects;

public class Main {
    public static void main(String[] args) {
        int x = Verifier.nondetInt();
        boolean z = Verifier.nondetBoolean();
        long v = Verifier.nondetLong();
        if (!z && x == 5 && v == 4294967296L && Objects.hash(x, z, null, v) == 2255468) assert false;
    }
}
