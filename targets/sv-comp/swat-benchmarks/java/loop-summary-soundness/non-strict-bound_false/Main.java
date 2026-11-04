// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:5 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// i <= val: the loop never ends for val = 2^31 - 1, so it has no closed form.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        if (val > 100) return;
        int i = 0;
        int s = 0;
        while (i <= val) {
            i++;
            s++;
        }
        if (s == 6) {
            if (b > 1000 && b < 100000) assert false;
        }
    }
}
