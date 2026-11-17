// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): string:a
// Must stay UNKNOWN (String.hashCode is not modelled). SAFE: a one-char string hashing to
// 97 is "a".
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (Arrays.hashCode(new Object[] {s}) == 128 && s.length() == 1 && !s.equals("a")) assert false;
    }
}
