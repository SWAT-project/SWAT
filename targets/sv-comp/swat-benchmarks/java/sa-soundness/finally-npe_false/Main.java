// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// A NullPointerException leaves a try block with a finally (a catch-any handler in bytecode); the
// assert is in the finally block. main catches the exception afterwards.
public class Main {
    static class Box {
        int v = 3;
    }

    static int read(Box box, int a) {
        try {
            return box.v;
        } finally {
            if (box == null) {
                if (a > 1000 && a < 100000) assert false;
            }
        }
    }

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        Box box = b == 7 ? null : new Box();
        int r;
        try {
            r = read(box, a);
        } catch (NullPointerException e) {
            r = 0;
        }
        if (r > 50) r--;
    }
}
