// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:c
// As hash-iterate, over values(): their order is the keys' hash order too. Must stay UNKNOWN.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String a = Verifier.nondetString();
        Map<String, Integer> m = new HashMap<>();
        m.put("b", 1);
        m.put(a, 2);
        int first = m.values().iterator().next();
        if (first == 1 && a.equals("c")) assert false;
    }
}
