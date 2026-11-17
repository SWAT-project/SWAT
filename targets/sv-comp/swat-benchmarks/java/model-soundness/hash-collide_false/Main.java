// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:k string:k
// Two symbolic keys that may or may not be equal: put(a), put(b) leaves one entry iff
// a.equals(b), and then get(a) is the second value. SAFE: with one entry, get(a) is 2.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String a = Verifier.nondetString();
        String b = Verifier.nondetString();
        Map<String, Integer> m = new HashMap<>();
        m.put(a, 1);
        m.put(b, 2);
        if (a.equals("k") && m.size() == 1 && m.get(a) == 2) assert false;
    }
}
