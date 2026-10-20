// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1001 int:7
import java.lang.reflect.Method;
import org.sosy_lab.sv_benchmarks.Verifier;
// The asserting no-argument method is called through Method.invoke; it reads the input from a
// static field.
public class Main {
    static int input;

    public static void check() {
        if (input > 1000 && input < 1001) assert false;
    }

    public static void main(String[] args) throws Exception {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        input = a;
        Method m = Main.class.getMethod("check");
        if (b == 7) {
            m.invoke(null);
        }
        if (a > 3) a--;
    }
}
