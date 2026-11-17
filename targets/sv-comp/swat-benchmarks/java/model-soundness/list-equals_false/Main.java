// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:k string:m
// ArrayList.equals of two lists with symbolic String elements: equal iff the elements are,
// pair by pair. SAFE: only a == "k" and b == "m" make them equal.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String a = Verifier.nondetString();
        String b = Verifier.nondetString();
        List<String> x = new ArrayList<>();
        x.add(a);
        x.add("m");
        List<String> y = new ArrayList<>();
        y.add("k");
        y.add(b);
        if (x.equals(y) && a.length() == 1) assert false;
    }
}
