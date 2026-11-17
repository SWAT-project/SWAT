// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): string:j
// addAll of a map's key set (with a symbolic key) into a set. SAFE: two elements are left
// only if a is "j" or "k".
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String a = Verifier.nondetString();
        Map<String, Integer> m = new HashMap<>();
        m.put(a, 1);
        m.put("k", 2);
        Set<String> s = new HashSet<>();
        s.add("j");
        s.addAll(m.keySet());
        if (s.size() == 2 && !a.equals("j") && !a.equals("k")) assert false;
    }
}
