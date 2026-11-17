// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): string:p
// As hash-iterate-order, for a ConcurrentHashMap: its iteration order is the hash order too.
// Must stay UNKNOWN.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String a = Verifier.nondetString();
        Map<String, Integer> m = new java.util.concurrent.ConcurrentHashMap<>();
        m.put("b", 1);
        m.put(a, 2);
        int first = m.values().iterator().next();
        if (first == 2 && a.length() == 1 && a.charAt(0) > 'b' && a.charAt(0) < 'p') assert false;
    }
}
