// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): string: string:x int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// Two String inputs that the solver may set to the same value (here both empty at first): each
// must keep its own symbolic variable.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        String t = Verifier.nondetString();
        int a = Verifier.nondetInt();
        if (s.length() == 0 && t.length() == 1) {
            if (a > 1000 && a < 1001) assert false;
        }
    }
}
