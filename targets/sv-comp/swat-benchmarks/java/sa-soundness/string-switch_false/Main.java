// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// A switch on a String (hashCode lookupswitch + equals branches) selects the asserting case.
public class Main {
    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        String cmd = b == 1 ? "start" : b == 7 ? "stop" : "status";
        int r;
        switch (cmd) {
            case "start": r = 1; break;
            case "stop":
                if (a > 1000 && a < 100000) assert false;
                r = 2;
                break;
            case "status": r = 3; break;
            default: r = 0;
        }
        if (r > 2) r--;
    }
}
