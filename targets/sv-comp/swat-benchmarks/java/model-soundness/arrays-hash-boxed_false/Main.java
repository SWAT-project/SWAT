// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:5 boolean:true long:4294967296
// Objects.hash(Integer, Boolean, null, Long) is Arrays.hashCode(Object[]): 2255468 for
// x = 5, z = true, v = 2^32.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.Objects;

public class Main {
    public static void main(String[] args) {
        int x = Verifier.nondetInt();
        boolean z = Verifier.nondetBoolean();
        long v = Verifier.nondetLong();
        if (Objects.hash(x, z, null, v) == 2255468) assert false;
    }
}
