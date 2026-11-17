// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): string:a
// Iteration of a LinkedHashMap with a symbolic key: insertion order, whatever the keys are.
// SAFE: "b" was put first, and putting it again does not move it.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String a = Verifier.nondetString();
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("b", 1);
        m.put(a, 2);
        Iterator<String> it = m.keySet().iterator();
        String first = it.next();
        if (!first.equals("b")) assert false;
    }
}
