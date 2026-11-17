// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:ab int:5
// Arrays.equals(Object[], Object[]) over String, Integer and null elements. In the SAFE
// variant the second element is a Long: Integer.equals(Long) is false for every value.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        int x = Verifier.nondetInt();
        Object[] a = {s, Integer.valueOf(x), null};
        Object[] b = {"ab", Integer.valueOf(5), null};
        if (Arrays.equals(a, b)) assert false;
    }
}
