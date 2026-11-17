// Expected verdict: SAFE. after reset("123"), matches() is true, so the assert needs !true. reset is not modelled: UNKNOWN.
// Paired VIOLATION inputs (must not fail here): string:a
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sosy_lab.sv_benchmarks.Verifier;
// A matcher of a symbolic string reset to a concrete one: not modelled, context loss.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        Matcher m = Pattern.compile("[0-9]+").matcher(s);
        boolean first = m.matches();
        m.reset("123");
        boolean second = m.matches();
        if (!first && s.length() == 1) {
            if (!second) assert false;
        }
    }
}
