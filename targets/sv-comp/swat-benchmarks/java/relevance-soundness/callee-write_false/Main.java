// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:3 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// The region calls a method that writes a field the assert's guard reads.
public class Main {
    static boolean marked;

    static void mark() {
        marked = true;
    }

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        for (int i = 0; i < 10; i++) {
            if (i == val) {
                mark();
            }
        }
        if (marked) {
            if (b > 1000 && b < 100000) assert false;
        }
    }
}
