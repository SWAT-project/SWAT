// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:100
import org.sosy_lab.sv_benchmarks.Verifier;
// loop-summary/count-up-print: a counting loop up to an input bound that also prints.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int i = 0;
        int s = 0;
        while (i < val) {
            i++;
            s += 3;
            System.out.println(i);
        }
        if (s == 300) assert false;
    }
}
