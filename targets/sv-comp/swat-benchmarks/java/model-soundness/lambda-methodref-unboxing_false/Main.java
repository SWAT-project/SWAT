// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1000 int:1 int:7
import java.util.function.Function;
import org.sosy_lab.sv_benchmarks.Verifier;
// A bound method reference whose method takes an int, called as Function<Integer, Integer>: LambdaMetafactory unboxes the argument (an adaptation SWAT does not model).
public class Main {
    int c;

    int addTo(int v) {
        int x = v + c;
        if (x > 1000 && x < 100000) assert false;
        return x;
    }

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        Main m = new Main();
        m.c = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        Function<Integer, Integer> f = m::addTo;
        if (b == 7) f.apply(a);
    }
}
