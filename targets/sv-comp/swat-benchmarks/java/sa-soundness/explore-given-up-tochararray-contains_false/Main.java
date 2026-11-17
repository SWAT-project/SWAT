// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:<bad/> int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// As explore-given-up-tochararray-len, but the length pin is only implied: s.contains() needs
// six chars or more, a length the pinned runs never have.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        int a = Verifier.nondetInt();
        char[] c = s.toCharArray();
        if (s.contains("<bad/>")) {
            if (a > 1000 && a < 100000) assert false;
        }
    }
}
