// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1001
import java.lang.reflect.Method;
import org.sosy_lab.sv_benchmarks.Verifier;
// Method.invoke runs a method with branches unconditionally; main has a branch of its own after
// the call, which the walk may pair with the callee's branches.
public class Main {
    static int cnt;

    public static void check(int x) {
        if (x > 1000 && x < 1001) assert false;
    }

    public static void main(String[] args) throws Exception {
        int a = Verifier.nondetInt();
        Method m = Main.class.getMethod("check", int.class);
        m.invoke(null, a);
        if (a > 3) cnt++;
        if (a > 5) cnt++;
    }
}
