// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): string:hi
// A String key built by a constructor SWAT may not model (from bytes): its shadow must not
// be compared unless it is the string. SAFE or UNKNOWN on the SAFE task. The all-zero run ends
// early.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (s.isEmpty()) return;
        Map<String, Integer> m = new HashMap<>();
        m.put(new String(new byte[] {104, 105}), 1);
        if (m.containsKey(s) && !s.equals("hi")) assert false;
    }
}
