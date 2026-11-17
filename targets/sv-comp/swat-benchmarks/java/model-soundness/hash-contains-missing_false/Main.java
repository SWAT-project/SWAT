// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:c
// containsKey of a symbolic key the map does not hold: the path must say it differs from
// every stored key. SAFE: a one-char string in [a, b] is a key.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        Map<String, Integer> m = new HashMap<>();
        m.put("a", 1);
        m.put("b", 2);
        if (!m.containsKey(s) && s.length() == 1 && s.charAt(0) >= 'a' && s.charAt(0) <= 'c') assert false;
    }
}
