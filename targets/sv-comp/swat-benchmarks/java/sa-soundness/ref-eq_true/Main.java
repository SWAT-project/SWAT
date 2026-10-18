// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// Object == before the assert (rewritten to UtilInstrumented.refEquals, which adds branch events).
public class Main {
    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        Object o1 = new Object();
        Object o2 = b == 7 ? o1 : new Object();
        if (o1 == o2) {
            if (a > 1000 && a < 1001) assert false;
        }
    }
}
