// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): string:key
// put with a symbolic key overwrites the entry of an equal concrete key; get of the concrete
// key then returns the new value. SAFE: only the key "key" overwrites it.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String k = Verifier.nondetString();
        Map<String, String> m = new HashMap<>();
        m.put("key", "old");
        String before = m.put(k, "new");
        if (m.get("key").equals("new") && !k.equals("key")) assert false;
    }
}
