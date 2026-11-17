// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:ß
import org.sosy_lab.sv_benchmarks.Verifier;
// The upper case of "\u00df" (sharp s) is "SS": a string of one char upper-cases to two.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (s.length() == 1 && s.toUpperCase().equals("SS")) {
            if (true) assert false;
        }
    }
}
