// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:ab
// Sanity pair: the builder holds the same chars as the String.
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        StringBuilder sb = new StringBuilder(s);
        if (!sb.equals(s) && sb.toString().equals("ab")) assert false;
    }
}
