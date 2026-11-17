// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:k
// clear, then a new entry: the keys from before are gone. SAFE: only "k" is a key.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String a = Verifier.nondetString();
        Map<String, Integer> m = new HashMap<>();
        m.put(a, 1);
        m.clear();
        m.put("k", 2);
        if (m.containsKey(a) && m.size() == 1) assert false;
    }
}
