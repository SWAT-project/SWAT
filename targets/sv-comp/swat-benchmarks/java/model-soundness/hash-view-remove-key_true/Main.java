// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): string:k
// As hash-view-remove, with the symbolic key in the map and a concrete one removed through
// the view. Must stay UNKNOWN.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String a = Verifier.nondetString();
        Map<String, Integer> m = new HashMap<>();
        m.put(a, 1);
        m.keySet().remove("k");
        if (m.isEmpty() && !a.equals("k")) assert false;
    }
}
