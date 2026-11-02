// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:3 int:1001
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
            if (b > 1000 && b < 1001) assert false;
        }
    }
}
