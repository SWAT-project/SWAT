// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:q string:q
// Objects.equals of two lists of different sizes: never equal, whatever the elements are.
// SAFE: the lists differ.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String a = Verifier.nondetString();
        String b = Verifier.nondetString();
        List<String> x = new ArrayList<>();
        x.add(a);
        List<String> y = new ArrayList<>();
        y.add(a);
        y.add(b);
        if (!Objects.equals(x, y) && a.equals("q") && x.equals(x)) assert false;
    }
}
