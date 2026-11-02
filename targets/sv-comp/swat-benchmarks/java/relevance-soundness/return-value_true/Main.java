// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:4 int:1001
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
            if (b > 1000 && b < 1001) assert false;
        }
    }
}
