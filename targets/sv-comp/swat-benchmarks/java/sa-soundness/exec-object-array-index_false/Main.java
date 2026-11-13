// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:2
import org.sosy_lab.sv_benchmarks.Verifier;
// An object read from an array at an input-dependent index decides whether the assert is reached.
public class Main {
    static class C {
        final int k;

        C(int k) {
            this.k = k;
        }
    }

    static final C[] TABLE = {new C(0), new C(0), new C(7)};

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        if (b >= 0 && b < TABLE.length) {
            if (TABLE[b].k == 7) {
                if (a > 1000 && a < 100000) assert false;
            }
        }
    }
}
