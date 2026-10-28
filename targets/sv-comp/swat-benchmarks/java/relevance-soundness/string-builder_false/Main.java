// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:3 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// The region appends to a StringBuilder whose length the assert's guard reads.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (i == val) {
                sb.append('x');
            }
        }
        if (sb.length() == 1) {
            if (b > 1000 && b < 100000) assert false;
        }
    }
}
