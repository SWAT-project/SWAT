// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): string:<bad/> int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// As explore-given-up-tochararray-len, but the length pin is only implied: s.contains() needs
// six chars or more, a length the pinned runs never have.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        int a = Verifier.nondetInt();
        char[] c = s.toCharArray();
        if (s.contains("<bad/>")) {
            if (a > 1000 && a < 1001) assert false;
        }
    }
}
