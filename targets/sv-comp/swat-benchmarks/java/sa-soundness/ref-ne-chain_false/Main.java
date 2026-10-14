// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// Several object != comparisons, then branches, then the assert.
public class Main {
    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        Object o1 = new Object();
        Object o2 = b == 7 ? o1 : new Object();
        Object o3 = new Object();
        int r = 0;
        if (o1 != o3) r++;
        if (o2 != o3) r++;
        if (a > 5) r++;
        if (o1 != o2) return;
        if (r == 3) {
            if (a > 1000 && a < 100000) assert false;
        }
    }
}
