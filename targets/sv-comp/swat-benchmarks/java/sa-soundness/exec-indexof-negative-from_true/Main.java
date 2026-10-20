// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): string:a int:-6 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// String.indexOf(String, int) treats a negative fromIndex as 0.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        int from = Verifier.nondetInt();
        int a = Verifier.nondetInt();
        if (from < -5 && s.indexOf("a", from) == 0) {
            if (a > 1000 && a < 1001) assert false;
        }
    }
}
