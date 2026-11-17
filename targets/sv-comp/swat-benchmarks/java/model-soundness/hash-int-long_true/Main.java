// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): int:5
// Integer and Long keys: an Integer never equals a Long of the same value. The VIOLATION
// variant puts Integer.valueOf(x), which replaces the entry of 5 for x == 5; the SAFE one
// puts Long.valueOf(x), which is always a second entry.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        int x = Verifier.nondetInt();
        Map<Object, String> m = new HashMap<>();
        m.put(Integer.valueOf(5), "int");
        m.put(Long.valueOf(x), "other");
        if (m.size() == 1 || !"int".equals(m.get(5))) assert false;
    }
}
