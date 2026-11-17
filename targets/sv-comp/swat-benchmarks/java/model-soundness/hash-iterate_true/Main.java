// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): string:c
// Iteration of a HashMap with a symbolic key: the order depends on the key's hash code
// ("b" comes before "c" but after "a" and ""), which no branch on the path says. Must stay
// UNKNOWN: an order taken as fixed makes the VIOLATION task a false SAFE.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String a = Verifier.nondetString();
        Map<String, Integer> m = new HashMap<>();
        m.put("b", 1);
        m.put(a, 2);
        String first = m.keySet().iterator().next();
        if (first.equals("b") && a.equals("a")) assert false;
    }
}
