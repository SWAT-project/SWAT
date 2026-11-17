// Expected verdict: SAFE. "x" upper-cases to "X", not "SS".
// Paired VIOLATION inputs (must not fail here): string:ß
import org.sosy_lab.sv_benchmarks.Verifier;
// The upper case of "\u00df" (sharp s) is "SS": a string of one char upper-cases to two.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (s.length() == 1 && s.toUpperCase().equals("SS")) {
            if (s.equals("x")) assert false;
        }
    }
}
