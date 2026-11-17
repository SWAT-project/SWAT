// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:c
// A HashSet with a symbolic element copied into a list: the list's order is the hash order.
// Must stay UNKNOWN.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String a = Verifier.nondetString();
        Set<String> s = new HashSet<>();
        s.add("b");
        s.add(a);
        List<String> l = new ArrayList<>(s);
        if (l.get(0).equals("b") && a.equals("c")) assert false;
    }
}
