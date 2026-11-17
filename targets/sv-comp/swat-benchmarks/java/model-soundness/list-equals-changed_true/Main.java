// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): string:z
// equals of a list that library code SWAT does not model has changed (Collections.reverse):
// its elements are not known in order any more, and comparing the recorded ones puts
// a == "k" on the path instead of a == "z". Must stay UNKNOWN.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String a = Verifier.nondetString();
        List<String> x = new ArrayList<>();
        x.add(a);
        x.add("k");
        Collections.reverse(x);
        List<String> y = new ArrayList<>();
        y.add("k");
        y.add("z");
        if (x.equals(y) && !a.equals("z")) assert false;
    }
}
