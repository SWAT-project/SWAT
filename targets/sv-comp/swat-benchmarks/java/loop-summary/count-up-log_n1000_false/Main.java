// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1000
import org.sosy_lab.sv_benchmarks.Verifier;
// loop-summary/count-up-log: a counting loop that also logs into a field and a scratch array.
public class Main {
    static int logged;
    static int[] recent = new int[8];

    static void log(int i) {
        logged++;
        recent[i & 7] = i;
    }

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int i = 0;
        int s = 0;
        while (i < val) {
            i++;
            s += 2;
            log(i);
        }
        if (s == 2000) assert false;
    }
}
