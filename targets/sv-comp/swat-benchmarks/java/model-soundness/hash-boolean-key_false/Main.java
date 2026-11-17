// Expected verdict: VIOLATION. Violating inputs (nondet call order): boolean:true byte:-3
// Boolean and Byte keys from constants, looked up with symbolic values. SAFE: only true and
// (byte) -3 are keys.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        boolean b = Verifier.nondetBoolean();
        byte y = Verifier.nondetByte();
        Map<Object, Integer> m = new HashMap<>();
        m.put(true, 1);
        m.put((byte) -3, 2);
        if (m.containsKey(b) && m.containsKey(y)) assert false;
    }
}
