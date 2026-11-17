// Expected verdict: VIOLATION. Violating inputs (nondet call order): short:7
// A Short key built by a constructor SWAT does not model (from a String): its shadow keeps
// the default 0, so it must not be compared (comparing it puts x == 0 on the path instead of
// x == 7, and the VIOLATION task becomes a false SAFE). Must stay UNKNOWN. The all-zero run
// ends early.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        short x = Verifier.nondetShort();
        if (x == 0) return;
        Map<Short, Integer> m = new HashMap<>();
        m.put(new Short("7"), 1);
        if (m.containsKey(x)) assert false;
    }
}
