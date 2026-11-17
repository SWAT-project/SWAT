// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:k string:k
// A ConcurrentHashMap with symbolic String keys: put, putIfAbsent, get, getOrDefault and
// remove by key as in a HashMap. SAFE: "v1" stays only under a (putIfAbsent of an equal b
// changes nothing, and remove(b) then removes it).
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String a = Verifier.nondetString();
        String b = Verifier.nondetString();
        Map<String, String> m = new java.util.concurrent.ConcurrentHashMap<>();
        m.put(a, "v1");
        String old = m.putIfAbsent(b, "v2");
        String got = m.getOrDefault(b, "none");
        m.remove(b);
        if (old != null && got.equals("v1") && m.isEmpty() && a.equals("k")) assert false;
    }
}
