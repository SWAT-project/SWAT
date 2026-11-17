// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:a
// A LinkedHashSet iterates in insertion order, also with symbolic elements. SAFE: "b" stays
// first.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String a = Verifier.nondetString();
        Set<String> s = new LinkedHashSet<>();
        s.add("b");
        s.add(a);
        Iterator<String> it = s.iterator();
        String first = it.next();
        if (it.hasNext() && it.next().equals("a")) assert false;
    }
}
