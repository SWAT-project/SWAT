// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): string:p
// A set changed by library code that is not modelled (Collections.addAll): its exact
// elements are not known afterwards. Must stay UNKNOWN.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String a = Verifier.nondetString();
        Set<String> s = new HashSet<>();
        s.add(a);
        Collections.addAll(s, "p", "q");
        if (s.size() == 2 && !a.equals("p") && !a.equals("q")) assert false;
    }
}
