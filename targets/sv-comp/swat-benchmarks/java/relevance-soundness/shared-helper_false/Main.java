// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:101 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// A helper with a branch is called both in an irrelevant region and to compute what the assert's
// guard reads.
public class Main {
    static int clamp(int x) {
        if (x > 100) {
            return 100;
        }
        return x;
    }

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        for (int i = 0; i < 10; i++) {
            if (i < val) {
                System.out.println(clamp(i));
            }
        }
        if (clamp(val) == 100) {
            if (b > 1000 && b < 100000) assert false;
        }
    }
}
