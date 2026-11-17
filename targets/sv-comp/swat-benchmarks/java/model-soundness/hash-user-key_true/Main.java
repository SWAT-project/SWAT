// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): int:13
// A key class with its own equals and hashCode (equal modulo 10): not a key the model
// compares. Must stay UNKNOWN (SAFE would need the hash code to be tracked into the map).
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
        int x = Verifier.nondetInt();
        Map<Key, String> m = new HashMap<>();
        m.put(new Key(3), "three");
        if (x >= 0 && x < 10 && x != 3 && m.containsKey(new Key(x))) assert false;
    }
}
