// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): string:stop int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// A switch on a nondet String selects the asserting case.
public class Main {
    public static void main(String[] args) {
        String cmd = Verifier.nondetString();
        int a = Verifier.nondetInt();
        int r;
        switch (cmd) {
            case "start": r = 1; break;
            case "stop":
                if (a > 1000 && a < 1001) assert false;
                r = 2;
                break;
            case "status": r = 3; break;
            default: r = 0;
        }
        if (r > 2) r--;
    }
}
