// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:s int:13
// A map with a symbolic String key and a key with its own equals: the model gives up at the
// second, and the map is not exact any more. Must stay UNKNOWN.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    static final class Key {
        final int v;
        Key(int v) { this.v = v; }
        @Override public boolean equals(Object o) { return o instanceof Key && ((Key) o).v % 10 == v % 10; }
        @Override public int hashCode() { return v % 10; }
    }
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        int x = Verifier.nondetInt();
        Map<Object, String> m = new HashMap<>();
        m.put(s, "string");
        m.put(new Key(3), "three");
        m.remove(new Key(x));
        if (s.equals("s") && x != 3 && m.size() == 1) assert false;
    }
}
