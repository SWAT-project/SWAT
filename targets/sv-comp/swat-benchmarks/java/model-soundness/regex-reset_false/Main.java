// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:a string:1
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sosy_lab.sv_benchmarks.Verifier;
// A matcher reused on another input with reset(t): not modelled, context loss. A model that kept
// matching s would hide the violation (s does not match, t does).
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        String t = Verifier.nondetString();
        Matcher m = Pattern.compile("[0-9]+").matcher(s);
        boolean first = m.matches();
        m.reset(t);
        boolean second = m.matches();
        if (!first && second) {
            if (t.length() == 1) assert false;
        }
    }
}
