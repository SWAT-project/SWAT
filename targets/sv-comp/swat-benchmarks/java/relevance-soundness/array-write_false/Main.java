// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:3 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// The region writes an array element that is summed up later.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int[] marks = new int[10];
        for (int i = 0; i < 10; i++) {
            if (i == val) {
                marks[i] = 1;
            }
        }
        int sum = 0;
        for (int j = 0; j < 10; j++) {
            sum += marks[j];
        }
        if (sum == 1) {
            if (b > 1000 && b < 100000) assert false;
        }
    }
}
