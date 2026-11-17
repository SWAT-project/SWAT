// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): string:ab
// StringBuilder does not override equals: a builder never equals a String, whatever its
// chars (jbmc-regression/StringBuilderConstructors02).
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        StringBuilder sb = new StringBuilder(s);
        if (sb.equals(s)) assert false;
    }
}
