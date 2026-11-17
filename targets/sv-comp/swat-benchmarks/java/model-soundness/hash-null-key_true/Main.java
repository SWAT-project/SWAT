// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): string:null
// The null key next to a symbolic String key: the String "null" is not the null key, and a
// nondet String is never null. SAFE: the null entry is never replaced.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        Map<String, String> m = new HashMap<>();
        m.put(null, "n");
        m.put(s, "s");
        if (m.size() != 2 || !m.get(null).equals("n")) assert false;
    }
}
