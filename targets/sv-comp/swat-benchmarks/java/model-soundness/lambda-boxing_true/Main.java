// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an integer x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1000 int:1 int:7
import java.util.function.Function;
import org.sosy_lab.sv_benchmarks.Verifier;
// A capturing Function<Integer, Integer>: the argument is boxed, unboxed in the body, and the result boxed.
public class Main {
    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int c = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        Function<Integer, Integer> f = v -> v + c;
        if (b == 7) {
            int x = f.apply(a);
            if (x > 1000 && x < 1001) assert false;
        }
    }
}
