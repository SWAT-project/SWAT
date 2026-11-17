// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:5 int:5
// A String key and an Integer key are never equal ("5" is not 5). SAFE: the map always has
// two entries.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        int x = Verifier.nondetInt();
        Map<Object, String> m = new HashMap<>();
        m.put(s, "string");
        m.put(x, "int");
        if (m.size() == 2 && s.equals("5") && x == 5) assert false;
    }
}
