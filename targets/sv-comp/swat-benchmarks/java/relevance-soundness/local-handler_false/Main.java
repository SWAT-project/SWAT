// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:3 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// The region catches an exception of a library call locally and records it.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int state = 0;
        for (int i = 0; i < 10; i++) {
            if (i == val) {
                try {
                    Integer.parseInt("x" + i);
                } catch (NumberFormatException e) {
                    state = 1;
                }
            }
        }
        if (state == 1) {
            if (b > 1000 && b < 100000) assert false;
        }
    }
}
