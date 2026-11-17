// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:k
// Lookups in Collections.emptyMap() and emptySet() with a symbolic key: null and false for
// every key. SAFE: nothing is found.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        Map<String, String> m = Collections.emptyMap();
        Set<String> e = Collections.emptySet();
        if (m.get(s) == null && !e.contains(s) && s.equals("k")) assert false;
    }
}
