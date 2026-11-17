// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:z string:z
// A HashSet built from a list with symbolic elements (and addAll of another set): equal
// elements collapse. SAFE: one element is left only if a is "z" too.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String a = Verifier.nondetString();
        String b = Verifier.nondetString();
        List<String> l = new ArrayList<>();
        l.add(a);
        l.add(b);
        l.add("z");
        Set<String> s = new HashSet<>(l);
        Set<String> t = new LinkedHashSet<>();
        t.add("z");
        boolean changed = t.addAll(s);
        if (!changed && s.size() == 1 && t.size() == 1 && a.length() == 1) assert false;
    }
}
