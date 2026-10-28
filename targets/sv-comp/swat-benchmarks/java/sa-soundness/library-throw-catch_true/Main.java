// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// A JDK method (Integer.parseInt) throws, and the application catches it: the usual way CLI tools
// handle a malformed argument. The assert is in the handler.
public class Main {
    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        String arg = b == 7 ? "x7" : "12";
        int v;
        try {
            v = Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            if (a > 1000 && a < 1001) assert false;
            v = 0;
        }
        if (v > 5) v--;
    }
}
