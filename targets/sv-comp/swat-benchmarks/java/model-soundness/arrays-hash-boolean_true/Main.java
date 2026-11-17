// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): boolean:true
// The hash of a one-element boolean[] is 31 + 1231 or 31 + 1237, nothing else.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        boolean z = Verifier.nondetBoolean();
        int h = Arrays.hashCode(new boolean[] {z});
        if (h != 1262 && h != 1268) assert false;
    }
}
