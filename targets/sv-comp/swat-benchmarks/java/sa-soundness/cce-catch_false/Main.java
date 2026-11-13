// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// The assert is in a catch (ClassCastException).
public class Main {
    static class A {
    }

    static class B extends A {
        int v = 3;
    }

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        A o = b == 7 ? new A() : new B();
        int r;
        try {
            B bb = (B) o;
            r = bb.v;
            if (r > 2) r--;
        } catch (ClassCastException e) {
            if (a > 1000 && a < 100000) assert false;
            r = 0;
        }
        if (r > 50) r--;
    }
}
