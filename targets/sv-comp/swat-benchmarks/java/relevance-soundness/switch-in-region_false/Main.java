// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:2 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// A switch in the region sets the mode the assert's guard reads.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int mode = 0;
        for (int i = 0; i < 4; i++) {
            switch (i - val) {
                case 0:
                    mode = 2;
                    break;
                case 1:
                    System.out.println("one");
                    break;
                default:
                    break;
            }
        }
        if (mode == 2) {
            if (b > 1000 && b < 100000) assert false;
        }
    }
}
