// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): string:p
// The first value of a HashMap is the symbolic key's only if that key hashes before "b": true
// for "" and "p" (table index 0), false for "c" (index 3 after 2). No branch says so: a search
// that takes the order of one run as fixed sees first == 1 for every one-char key after 'b'
// and reports the VIOLATION task SAFE. Must stay UNKNOWN (SAFE variant: 'c' to 'o' all
// come after "b").
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String a = Verifier.nondetString();
        Map<String, Integer> m = new HashMap<>();
        m.put("b", 1);
        m.put(a, 2);
        int first = m.values().iterator().next();
        if (first == 2 && a.length() == 1 && a.charAt(0) > 'b' && a.charAt(0) < 'p') assert false;
    }
}
