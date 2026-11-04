// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:a int:-6 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// String.indexOf(String, int) treats a negative fromIndex as 0.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        int from = Verifier.nondetInt();
        int a = Verifier.nondetInt();
        if (from < -5 && s.indexOf("a", from) == 0) {
            if (a > 1000 && a < 100000) assert false;
        }
    }
}
