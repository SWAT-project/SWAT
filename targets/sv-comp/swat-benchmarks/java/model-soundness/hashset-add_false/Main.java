// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:x string:x
// HashSet.add of a symbolic element returns whether it was new; contains of another symbolic
// one. SAFE: if a was not new, the set is {"x"} and holds b only for b == "x".
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String a = Verifier.nondetString();
        String b = Verifier.nondetString();
        Set<String> s = new HashSet<>();
        s.add("x");
        boolean fresh = s.add(a);
        if (!fresh && s.size() == 1 && s.contains(b) && b.length() == 1) assert false;
    }
}
