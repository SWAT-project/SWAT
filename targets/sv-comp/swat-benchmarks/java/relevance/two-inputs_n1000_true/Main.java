// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:0 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// relevance/two-inputs: the loop branches on one input, the assert on another.
public class Main {
    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        for (int i = 0; i < 1000; i++) {
            if (i < a) {
                System.out.print('.');
            } else if (i == a + 1) {
                System.out.print('#');
            }
        }
        System.out.println();
        if (b > a) {
            if (b > 1000 && b < 1001) assert false;
        }
    }
}
