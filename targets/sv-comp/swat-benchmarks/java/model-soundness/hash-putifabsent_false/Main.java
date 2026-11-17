// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:k
// putIfAbsent: a key mapped to null counts as absent and gets the new value (VIOLATION
// variant, a == "k"); a key with a value keeps it (SAFE variant).
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String a = Verifier.nondetString();
        Map<String, String> m = new HashMap<>();
        m.put("k", null);
        m.put("j", "J");
        m.putIfAbsent(a, "new");
        if ("new".equals(m.get("k"))) assert false;
    }
}
