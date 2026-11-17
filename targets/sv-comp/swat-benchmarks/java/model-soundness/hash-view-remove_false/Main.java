// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:k
// A key removed through the keySet view, which is not modelled: the map's exact entries are
// not known afterwards. Must stay UNKNOWN.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String a = Verifier.nondetString();
        Map<String, Integer> m = new HashMap<>();
        m.put("k", 1);
        m.keySet().remove(a);
        if (m.isEmpty()) assert false;
    }
}
