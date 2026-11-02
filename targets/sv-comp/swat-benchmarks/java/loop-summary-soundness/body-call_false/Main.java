// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:5 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// A counting loop with a call in its body: not a pure loop.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int i = 0;
        int s = 0;
        while (i < val) {
            i++;
            s += 2;
            System.out.println(i);
        }
        if (s == 10) {
            if (b > 1000 && b < 100000) assert false;
        }
    }
}
