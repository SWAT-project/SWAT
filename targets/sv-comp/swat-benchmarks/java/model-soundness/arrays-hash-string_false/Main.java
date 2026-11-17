// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:a
// Arrays.hashCode(Object[]) with a String element (String.hashCode is not modelled: stays a
// context loss). {"a"} hashes to 31 + 97.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (Arrays.hashCode(new Object[] {s}) == 128) assert false;
    }
}
