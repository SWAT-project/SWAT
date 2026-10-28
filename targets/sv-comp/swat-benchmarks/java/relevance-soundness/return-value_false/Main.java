// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:4 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// The region decides a callee's return value.
public class Main {
    static int scan(int val) {
        int found = -1;
        for (int i = 0; i < 10; i++) {
            if (i == val) {
                found = i;
            }
        }
        return found;
    }

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        if (scan(val) == 4) {
            if (b > 1000 && b < 100000) assert false;
        }
    }
}
