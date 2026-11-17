// Expected verdict: SAFE. only x and X upper-case to "X", and their lower case "x" does not contain "<bad/>".
// Paired VIOLATION inputs (must not fail here): string:<BAD/>
import org.sosy_lab.sv_benchmarks.Verifier;
// As the securibench sinks: the case mapping result is only searched for a concrete string.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        String l = s.toLowerCase();
        if (l.contains("<bad/>")) {
            if (s.toUpperCase().equals("X")) assert false;
        }
    }
}
