// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1001 int:7
import java.lang.reflect.Method;
import org.sosy_lab.sv_benchmarks.Verifier;
// Class.getMethods() returns an array the executor does not track; it is read before the assert.
public class Main {
    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        Method[] ms = Main.class.getMethods();
        int n = ms.length;
        String name = ms[0].getName();
        if (b == 7) {
            if (a > 1000 && a < 1001) assert false;
        }
        if (n > 3) n--;
    }
}
