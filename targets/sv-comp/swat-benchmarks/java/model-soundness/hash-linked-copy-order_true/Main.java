// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): string:p
// A LinkedHashMap copied from a HashMap with a symbolic key iterates in the HashMap's hash
// order, not in the order the keys were put (see hash-iterate-order). Must stay UNKNOWN.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String a = Verifier.nondetString();
        Map<String, Integer> m = new HashMap<>();
        m.put("b", 1);
        m.put(a, 2);
        Map<String, Integer> l = new LinkedHashMap<>(m);
        int first = l.values().iterator().next();
        if (first == 2 && a.length() == 1 && a.charAt(0) > 'b' && a.charAt(0) < 'p') assert false;
    }
}
