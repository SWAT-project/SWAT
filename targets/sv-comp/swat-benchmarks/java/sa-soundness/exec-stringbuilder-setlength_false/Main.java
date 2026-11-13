// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:ab int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// A StringBuilder is reused with setLength(0), as parsers do between fields.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        int a = Verifier.nondetInt();
        StringBuilder sb = new StringBuilder();
        sb.append(s);
        sb.setLength(0);
        sb.append('x');
        if (sb.length() == 1 && s.length() == 2) {
            if (a > 1000 && a < 100000) assert false;
        }
    }
}
