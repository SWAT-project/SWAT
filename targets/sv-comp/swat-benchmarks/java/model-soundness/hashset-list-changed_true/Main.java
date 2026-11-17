// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): string:z
// A HashSet built from a list that a call SWAT does not model changed (remove(Object)): the
// list's elements are not known exactly, so neither are the set's. Must stay UNKNOWN.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String a = Verifier.nondetString();
        List<String> l = new ArrayList<>();
        l.add("y");
        l.add("z");
        l.remove(a);
        Set<String> s = new HashSet<>(l);
        if (s.size() == 1 && s.contains("y") && !a.equals("z")) assert false;
    }
}
