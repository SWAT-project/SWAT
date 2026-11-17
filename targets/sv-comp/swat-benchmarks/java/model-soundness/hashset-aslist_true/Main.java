// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): string:z
// A HashSet built from Arrays.asList, a source the model does not copy from: the set is not
// modelled. Must stay UNKNOWN.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String a = Verifier.nondetString();
        Set<String> s = new HashSet<>(Arrays.asList(a, "z"));
        if (s.size() == 1 && !a.equals("z")) assert false;
    }
}
