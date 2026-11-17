// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): string:q string:q
// The identity of what get returns, with possibly equal symbolic keys: the lookup key b
// finds v1 iff it equals a (which may itself have replaced the entry of "x"). SAFE: get
// returns v1 only for b equal to a, and v0 only for "x".
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String a = Verifier.nondetString();
        String b = Verifier.nondetString();
        Object v0 = new Object();
        Object v1 = new Object();
        Map<String, Object> m = new HashMap<>();
        m.put("x", v0);
        m.put(a, v1);
        Object got = m.get(b);
        if ((got == v1 && !b.equals(a)) || (got == v0 && (!b.equals("x") || a.equals("x"))) || (got != v0 && got != v1 && got != null)) assert false;
    }
}
