// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import java.lang.reflect.Method;
import org.sosy_lab.sv_benchmarks.Verifier;
// The asserting method is called through Method.invoke on an input-dependent path.
public class Main {
    public static void check(int x) {
        if (x > 1000 && x < 100000) assert false;
    }

    public static void main(String[] args) throws Exception {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        Method m = Main.class.getMethod("check", int.class);
        if (b == 7) {
            m.invoke(null, a);
        }
        if (a > 3) a--;
    }
}
