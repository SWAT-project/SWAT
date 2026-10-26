// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:4 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// A counting loop whose body calls a helper that sets a flag the assert's guard reads.
public class Main {
    static boolean reached;

    static void visit(int i) {
        if (i == 4) {
            reached = true;
        }
    }

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int i = 0;
        while (i < val) {
            i++;
            visit(i);
        }
        if (reached) {
            if (b > 1000 && b < 100000) assert false;
        }
    }
}
