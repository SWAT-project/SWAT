// Expected verdict: VIOLATION. Violating inputs (nondet call order): boolean:true
// Arrays.hashCode(boolean[]): Boolean.hashCode is 1231 for true, 1237 for false.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        boolean z = Verifier.nondetBoolean();
        int h = Arrays.hashCode(new boolean[] {z});
        if (h == 1262) assert false;
    }
}
