// Expected verdict: VIOLATION. Violating inputs (nondet call order): short:7
// As hash-char-literal, with a Short key from a constant. The all-zero run ends early.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        short x = Verifier.nondetShort();
        if (x == 0) return;
        Map<Short, Integer> m = new HashMap<>();
        m.put((short) 7, 1);
        if (m.containsKey(x)) assert false;
    }
}
