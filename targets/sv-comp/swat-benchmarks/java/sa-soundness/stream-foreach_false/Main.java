// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import java.util.stream.IntStream;
import org.sosy_lab.sv_benchmarks.Verifier;
// The asserting method is called back from JDK stream code through a method reference.
public class Main {
    static void check(int x) {
        if (x > 1000 && x < 100000) assert false;
    }

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        if (b == 7) {
            IntStream.of(a).forEach(Main::check);
        }
        if (a > 3) a--;
    }
}
