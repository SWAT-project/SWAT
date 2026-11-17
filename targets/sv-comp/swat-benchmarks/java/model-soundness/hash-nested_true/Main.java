// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): string:p string:id
// A map of maps with symbolic keys: get returns the very map that was put, so what is put
// into it shows through the outer map. SAFE: only q == "id" makes "id" a key.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String p = Verifier.nondetString();
        String q = Verifier.nondetString();
        Map<String, Map<String, String>> outer = new HashMap<>();
        outer.put(p, new HashMap<String, String>());
        outer.get(p).put(q, "v");
        Map<String, String> inner = outer.get(p);
        if (inner.containsKey("id") && !q.equals("id")) assert false;
    }
}
