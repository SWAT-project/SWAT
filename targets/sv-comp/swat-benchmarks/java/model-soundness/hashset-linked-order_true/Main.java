// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): string:a
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
        if (!first.equals("b")) assert false;
    }
}
