// Expected verdict: VIOLATION. Violating inputs (nondet call order): char:Z long:4294967296 boolean:true
// Arrays.equals for char[], long[] and boolean[]; the long must be 2^32 (its upper half).
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        char c = Verifier.nondetChar();
        long v = Verifier.nondetLong();
        boolean z = Verifier.nondetBoolean();
        if (Arrays.equals(new char[] {c}, new char[] {'Z'}) && Arrays.equals(new long[] {v}, new long[] {4294967296L})
                && Arrays.equals(new boolean[] {z}, new boolean[] {true})) assert false;
    }
}
