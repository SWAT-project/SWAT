// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// An inner class reads a private field and calls a private method of its outer class through
// javac's synthetic access$ methods (Java 8 target).
public class Main {
    private int limit = 100000;

    private boolean inRange(int a) {
        return a > 1000;
    }

    class Inner {
        void check(int a) {
            if (inRange(a) && a < limit) {
                if (a > 1000 && a < 100000) assert false;
            }
        }
    }

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        Main m = new Main();
        Main.Inner in = m.new Inner();
        if (b == 7) in.check(a);
        if (a > 3) a--;
    }
}
