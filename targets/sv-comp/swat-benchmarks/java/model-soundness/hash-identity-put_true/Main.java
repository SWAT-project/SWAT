// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): string:k string:k
// The identity of what put returns: the previous value of the entry the key equals, v0 iff
// the two symbolic keys are equal. Reference comparisons are decided on the objects, so the
// path must say which entry matched. SAFE: put returns v0 only for equal keys.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String a = Verifier.nondetString();
        String b = Verifier.nondetString();
        Object v0 = new Object();
        Object v1 = new Object();
        Map<String, Object> m = new HashMap<>();
        m.put(a, v0);
        Object previous = m.put(b, v1);
        if ((previous == v0 && !a.equals(b)) || (previous != v0 && previous != null)) assert false;
    }
}
