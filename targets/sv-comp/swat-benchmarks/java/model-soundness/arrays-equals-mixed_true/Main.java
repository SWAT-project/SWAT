// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): char:Z long:4294967296 boolean:true
// As arrays-equals-mixed_false, but the low half of the long must also be 1: long equality is
// 64-bit, so v == 2^32 has (int) v == 0 and the assert is unreachable.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        char c = Verifier.nondetChar();
        long v = Verifier.nondetLong();
        boolean z = Verifier.nondetBoolean();
        if (Arrays.equals(new char[] {c}, new char[] {'Z'}) && Arrays.equals(new long[] {v}, new long[] {4294967296L})
                && Arrays.equals(new boolean[] {z}, new boolean[] {true}) && (int) v == 1) assert false;
    }
}
