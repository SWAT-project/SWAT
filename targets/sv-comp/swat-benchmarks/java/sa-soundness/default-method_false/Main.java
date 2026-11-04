// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// The assert is in an interface default method, called on a class that does not override it.
public class Main {
    interface Checker {
        default void check(int a) {
            if (a > 1000 && a < 100000) assert false;
        }
    }

    static class Impl implements Checker {
    }

    static class Other implements Checker {
        int cnt;

        public void check(int a) {
            cnt++;
        }
    }

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        if (b == 7) {
            new Impl().check(a);
        } else {
            new Other().check(a);
        }
        if (a > 3) a--;
    }
}
