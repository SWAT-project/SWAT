// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// As exec-system-exit, but a branch on the input is recorded before the exit.
public class Main {
    static int cnt;

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        if (a > 3) cnt++;
        if (b != 7) {
            System.exit(0);
        }
        if (a > 1000 && a < 100000) assert false;
    }
}
