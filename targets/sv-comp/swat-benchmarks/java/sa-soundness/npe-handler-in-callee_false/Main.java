// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// The last branch before the null dereference is in main, but the handler is in a callee entered
// after it: the walk cannot know that callee's call site and must give up instead.
public class Main {
    static class Box {
        int v = 3;
    }

    static int read(Box box) {
        return box.v;
    }

    static int guarded(Box box, int a) {
        try {
            return read(box);
        } catch (NullPointerException e) {
            if (a > 1000 && a < 100000) assert false;
            return 0;
        }
    }

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        Box box = b == 7 ? null : new Box();
        int r = guarded(box, a);
        if (r > 50) r--;
    }
}
