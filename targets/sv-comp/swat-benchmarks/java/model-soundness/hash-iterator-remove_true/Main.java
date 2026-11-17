// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): string:k
// An entry removed through an iterator: the map's exact entries are not known afterwards.
// Must stay UNKNOWN.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String a = Verifier.nondetString();
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("k", 1);
        m.put(a, 2);
        Iterator<String> it = m.keySet().iterator();
        it.next();
        it.remove();
        if (m.isEmpty() && !a.equals("k")) assert false;
    }
}
