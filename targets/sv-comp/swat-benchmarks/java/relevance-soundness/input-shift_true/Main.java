// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:2 int:0 int:0 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// The region reads inputs, which shifts which input every later read gets.
public class Main {
    public static void main(String[] args) {
        int x = Verifier.nondetInt();
        for (int i = 0; i < 3; i++) {
            if (i < x) {
                int junk = Verifier.nondetInt();
                System.out.println(junk);
            }
        }
        int z = Verifier.nondetInt();
        if (x == 2) {
            if (z > 1000 && z < 1001) assert false;
        }
    }
}
