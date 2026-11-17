// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): double:-0.0
// Double keys: Double.equals compares bits, so -0.0 is not the key 0.0 although -0.0 == 0.0.
// Not modelled. Must stay UNKNOWN.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        double d = Verifier.nondetDouble();
        Map<Double, Integer> m = new HashMap<>();
        m.put(0.0, 1);
        if (m.containsKey(d) && d != 0.0) assert false;
    }
}
