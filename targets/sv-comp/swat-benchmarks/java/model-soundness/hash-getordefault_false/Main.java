// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:k
// getOrDefault: a key mapped to null gives null, not the default (VIOLATION variant,
// a == "k"); in the SAFE variant no value is null.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String a = Verifier.nondetString();
        Map<String, String> m = new HashMap<>();
        m.put("k", null);
        String v = m.getOrDefault(a, "default");
        if (v == null) assert false;
    }
}
