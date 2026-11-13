// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:3 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// The region writes a field through an alias of the object the assert's guard reads.
public class Main {
    static class Box {
        int v;
    }

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        Box p = new Box();
        Box q = p;
        for (int i = 0; i < 10; i++) {
            if (i == val) {
                q.v = 1;
            }
        }
        if (p.v == 1) {
            if (b > 1000 && b < 100000) assert false;
        }
    }
}
