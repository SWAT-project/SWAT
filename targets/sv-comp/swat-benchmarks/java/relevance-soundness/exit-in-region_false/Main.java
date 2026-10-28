// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:9 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// System.exit in the region ends the initial run (val = 0) before the assert.
// By design, the all-zero run ends early.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        for (int i = 0; i < 10; i++) {
            if (val < i) {
                System.out.println("bye");
                System.exit(0);
            }
        }
        if (b > 1000 && b < 100000) assert false;
    }
}
