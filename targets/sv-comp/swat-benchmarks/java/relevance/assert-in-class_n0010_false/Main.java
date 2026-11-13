// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// relevance/assert-in-class: the loop only appends to a StringBuilder (library state); the assert
// is in another class, whose static initializer javac gives a Class.desiredAssertionStatus() call.
// That call must not count as reading library state, or the loop's branches would be relevant.
public class Main {
    static class Check {
        static void hit(int x) {
            if (x > 1000 && x < 100000) assert false;
        }
    }

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        StringBuilder log = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (i < val) {
                log.append('.');
            }
        }
        Check.hit(val);
    }
}
