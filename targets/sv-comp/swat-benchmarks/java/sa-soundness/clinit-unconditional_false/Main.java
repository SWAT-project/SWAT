// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// Control for the clinit pairs: the class is first used on every path, so its <clinit> runs in
// the first round, inside the masked part of the walk. Only the input-dependent first use fails.
public class Main {
    static int input;

    static class Late {
        static int K;

        static {
            if (Main.input > 1000 && Main.input < 100000) assert false;
            K = 1;
        }

        static int get() {
            return K;
        }
    }
    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        input = a;
        int r = Late.get();
        if (b == 7) r++;
        if (r > 3) r--;
    }
}
