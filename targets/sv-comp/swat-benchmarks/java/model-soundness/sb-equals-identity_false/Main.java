// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:ab boolean:true
// StringBuilder.equals is identity: the same builder equals itself (f true), another one with
// the same chars does not.
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        boolean f = Verifier.nondetBoolean();
        StringBuilder sb = new StringBuilder(s);
        StringBuilder other = f ? sb : new StringBuilder(s);
        if (sb.equals(other) && s.length() == 2) assert false;
    }
}
