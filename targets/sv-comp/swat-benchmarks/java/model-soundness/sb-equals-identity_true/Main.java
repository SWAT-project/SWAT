// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): string:ab boolean:true
// StringBuffer.equals is identity: a different buffer with the same chars is never equal.
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        boolean f = Verifier.nondetBoolean();
        StringBuffer sb = new StringBuffer(s);
        StringBuffer other = new StringBuffer(s);
        if (f && sb.equals(other) && s.length() == 2) assert false;
    }
}
