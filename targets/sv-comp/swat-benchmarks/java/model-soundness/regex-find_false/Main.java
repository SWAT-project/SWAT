// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:x12
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sosy_lab.sv_benchmarks.Verifier;
// Matcher.find() is not modelled: context loss. A model that took it for matches() would hide
// the violation (a match inside the string).
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        Matcher m = Pattern.compile("[0-9]{2}").matcher(s);
        if (m.find()) {
            if (s.startsWith("x")) assert false;
        }
    }
}
