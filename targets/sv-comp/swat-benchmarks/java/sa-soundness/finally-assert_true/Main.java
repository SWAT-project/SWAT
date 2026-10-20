// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// The assert is inside a finally block, which javac copies onto every exit path of try.
public class Main {
    static int cnt;

    static void g(int a, int b) {
        try {
            if (b == 7) return;
            cnt++;
            if (b == 8) throw new IllegalStateException();
        } finally {
            if (b == 7) {
                if (a > 1000 && a < 1001) assert false;
            }
        }
    }

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        try {
            g(a, b);
        } catch (IllegalStateException e) {
            cnt--;
        }
    }
}
